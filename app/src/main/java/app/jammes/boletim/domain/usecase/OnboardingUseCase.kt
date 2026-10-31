package app.jammes.boletim.domain.usecase

import app.jammes.boletim.domain.model.AlunoDomain
import app.jammes.boletim.domain.model.CriacaoDoAno
import app.jammes.boletim.domain.model.Etapa
import app.jammes.boletim.domain.model.FonteMateria
import app.jammes.boletim.domain.model.IconeMateria
import app.jammes.boletim.domain.model.LimiteDoGratisException
import app.jammes.boletim.domain.model.MateriaDomain
import app.jammes.boletim.domain.model.PeriodoDomain
import app.jammes.boletim.domain.model.RegraAvaliacaoDomain
import app.jammes.boletim.domain.model.TipoPeriodo
import app.jammes.boletim.domain.model.criacaoDoAno
import app.jammes.boletim.domain.model.periodoAoAbrir
import app.jammes.boletim.domain.model.podeCriarAluno
import app.jammes.boletim.domain.model.podeGravarMateriasNovas
import app.jammes.boletim.domain.repository.AlunoRepository
import app.jammes.boletim.domain.repository.ContextoRepository
import app.jammes.boletim.domain.repository.MateriaRepository
import app.jammes.boletim.domain.repository.PlanoRepository
import app.jammes.boletim.domain.repository.Transacao
import jakarta.inject.Inject
import kotlinx.coroutines.flow.first
import java.text.Normalizer
import java.time.LocalDate
import kotlin.time.Clock

// ---------------------------------------------------------------------------
// Regras puras
// ---------------------------------------------------------------------------

// Pelo nome das matérias padrão (MateriasPadraoCallback): no primeiro uso, o cadastro é exatamente ele
private val MATERIAS_DO_FUNDAMENTAL = setOf(
    "Português", "Matemática", "História", "Geografia", "Ciências", "Inglês", "Arte", "Educação Física",
)

// O Médio troca Ciências por Física, Química e Biologia, e ganha Filosofia e Sociologia
private val MATERIAS_DO_MEDIO =
    MATERIAS_DO_FUNDAMENTAL - "Ciências" + setOf("Física", "Química", "Biologia", "Filosofia", "Sociologia")

/**
 * As matérias que o onboarding já deixa marcadas, na ordem do cadastro: as de costume na [etapa]. Sem etapa, ou se
 * o cadastro não tiver nenhuma delas, as [MATERIAS_DO_ANO_NOVO] primeiras, como num ano novo.
 */
internal fun materiasDoPrimeiroAno(etapa: Etapa?, materias: List<MateriaDomain>): List<MateriaDomain> {
    val deCostume = when (etapa) {
        Etapa.FUNDAMENTAL -> MATERIAS_DO_FUNDAMENTAL
        Etapa.MEDIO -> MATERIAS_DO_MEDIO
        null -> emptySet()
    }
    return materias.filter { it.nome in deCostume }.ifEmpty { materias.take(MATERIAS_DO_ANO_NOVO) }
}

// O ícone de uma matéria nova, pelo começo de uma palavra do nome (sem acentos: "Música" e
// "Musical" dão no mesmo). Sem nenhuma, fica o padrão
private val ICONES_POR_PALAVRA = listOf(
    "music" to IconeMateria.NOTA_MUSICAL,
    "robot" to IconeMateria.ROBO,
    "program" to IconeMateria.CODIGO,
    "informat" to IconeMateria.COMPUTADOR,
    "tecnolog" to IconeMateria.COMPUTADOR,
    "redac" to IconeMateria.REDACAO,
    "literat" to IconeMateria.REDACAO,
    "financ" to IconeMateria.DINHEIRO,
    "econom" to IconeMateria.DINHEIRO,
    "teatro" to IconeMateria.TEATRO,
    "desenho" to IconeMateria.REGUA,
    "espanhol" to IconeMateria.IDIOMA,
    "frances" to IconeMateria.IDIOMA,
)

/** O ícone que o nome sugere: "Educação Musical" ganha a nota musical, "Robótica" o robô. */
internal fun iconeSugerido(nome: String): IconeMateria {
    val palavras = Normalizer.normalize(nome.lowercase(), Normalizer.Form.NFD)
        .replace(MARCAS_DE_ACENTO, "")
        .split(' ', '-')
    return ICONES_POR_PALAVRA.firstOrNull { (comeco, _) -> palavras.any { it.startsWith(comeco) } }?.second
        ?: IconeMateria.PADRAO
}

private val MARCAS_DE_ACENTO = Regex("\\p{M}")

/**
 * Uma matéria criada no onboarding ou nas matérias do ano, como foi digitada: o nome e a sigla.
 * É Serializable porque fica nos formulários, que o rememberSaveable guarda.
 */
data class MateriaNova(
    val nome: String,
    val abreviacao: String? = null, // null ou vazia: sai do nome
) : java.io.Serializable

/**
 * As matérias criadas no onboarding ou nas matérias do ano, ainda sem id. Cada uma pega a menor cor
 * que o [cadastro] ainda não usa (as padrão deixam umas livres), para não repetir a de outra aba, e o
 * [iconeSugerido]. Sem sigla digitada, ela sai das três primeiras letras do nome, a mesma que o campo
 * mostra de exemplo.
 */
internal fun materiasNovas(novas: List<MateriaNova>, cadastro: List<MateriaDomain>): List<MateriaDomain> {
    val usadas = cadastro.map { it.cor }.toSet()
    val livres = generateSequence(0) { it + 1 }.filter { it !in usadas }.iterator()
    return novas.map { nova ->
        val nome = nova.nome.trim()
        MateriaDomain(
            nome = nome,
            abreviacao = nova.abreviacao?.trim()?.takeIf { it.isNotEmpty() }?.uppercase()
                ?: nome.filter { it.isLetterOrDigit() }.take(3).uppercase(),
            cor = livres.next(),
            icone = iconeSugerido(nome),
            fonte = FonteMateria.USUARIO,
        )
    }
}

// ---------------------------------------------------------------------------
// Caso de uso
// ---------------------------------------------------------------------------

/**
 * O que o onboarding escolheu. Os períodos e a regra ainda não têm ano letivo (id 0); as matérias são
 * as do cadastro marcadas ([materiaIds]) e as criadas ali ([materiasNovas], com o nome e a sigla).
 */
data class PrimeiroAno(
    val nome: String,
    val ano: Int,
    val serie: String?,
    val tipoPeriodo: TipoPeriodo,
    val periodos: List<PeriodoDomain>,
    val regra: RegraAvaliacaoDomain,
    val materiaIds: Set<Long>,
    val materiasNovas: List<MateriaNova> = emptyList(),
)

class ComecarBoletim @Inject constructor(
    private val transacao: Transacao,
    private val alunoRepository: AlunoRepository,
    private val materiaRepository: MateriaRepository,
    private val gravarAnoLetivoNovo: GravarAnoLetivoNovo,
    private val contextoRepository: ContextoRepository,
    private val planoRepository: PlanoRepository,
) {
    /**
     * O fim do onboarding: cria o aluno e o primeiro ano letivo dele, do jeito que o [primeiro] diz.
     * Grava tudo de uma vez e só então salva o Contexto, no período de [hoje]: com ele salvo, o app
     * sai do onboarding e abre o boletim.
     *
     * Se o plano não deixa criar mais um aluno ([podeCriarAluno]), as matérias novas
     * ([podeGravarMateriasNovas]) ou o ano letivo ([criacaoDoAno]), sai [LimiteDoGratisException] sem
     * gravar nada (as telas já mandam para a folha do Pro antes de chegar aqui).
     */
    suspend operator fun invoke(primeiro: PrimeiroAno, hoje: LocalDate = LocalDate.now()) {
        val plano = planoRepository.plano.first()
        val cadastro = materiaRepository.observeMaterias().first()
        if (!plano.podeCriarAluno(alunoRepository.observeAluno().first().size)) throw LimiteDoGratisException.deAlunos()
        if (!plano.podeGravarMateriasNovas(cadastro, primeiro.materiasNovas.size)) throw LimiteDoGratisException.deMaterias()
        // O aluno é novo: ainda não tem nenhum ano
        if (plano.criacaoDoAno(primeiro.ano, emptySet(), hoje) != CriacaoDoAno.LIBERADA) throw LimiteDoGratisException.deAnos()

        val marcadas = cadastro.filter { it.id in primeiro.materiaIds }

        val (alunoId, anoLetivo) = transacao {
            val alunoId = alunoRepository.upsert(AlunoDomain(nome = primeiro.nome.trim(), criadoEm = Clock.System.now()))
            // As novas entram no cadastro aqui, junto com o resto: desistir no meio não deixa sobra
            val criadas = materiasNovas(primeiro.materiasNovas, cadastro).map { it.copy(id = materiaRepository.upsert(it)) }
            val novo = anoLetivoNovo(alunoId, primeiro.ano, primeiro.serie, primeiro.tipoPeriodo, primeiro.periodos)
            alunoId to gravarAnoLetivoNovo(novo, marcadas + criadas, primeiro.regra)
        }

        contextoRepository.selecionarAluno(alunoId, anoLetivo.id, anoLetivo.periodo.periodoAoAbrir(hoje))
    }
}

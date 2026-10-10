package app.jammes.boletim.domain.usecase

import app.jammes.boletim.domain.model.AlunoComAno
import app.jammes.boletim.domain.model.AlunoDomain
import app.jammes.boletim.domain.model.AnoLetivoDomain
import app.jammes.boletim.domain.model.Contexto
import app.jammes.boletim.domain.model.PerfilAluno
import app.jammes.boletim.domain.model.PeriodoDomain
import app.jammes.boletim.domain.model.TipoPeriodo
import app.jammes.boletim.domain.model.periodoAoAbrir
import app.jammes.boletim.domain.repository.AlunoRepository
import app.jammes.boletim.domain.repository.AnoLetivoRepository
import app.jammes.boletim.domain.repository.ContextoRepository
import app.jammes.boletim.domain.repository.DisciplinaRepository
import app.jammes.boletim.domain.repository.MateriaRepository
import app.jammes.boletim.domain.repository.RegraAvaliacaoRepository
import app.jammes.boletim.domain.repository.Transacao
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.LocalDate

// ---------------------------------------------------------------------------
// Regras puras
// ---------------------------------------------------------------------------

/** Quantas matérias um ano novo já traz: as 5 primeiras do cadastro (Português, Matemática, História, Geografia e Ciências). */
const val MATERIAS_DO_ANO_NOVO = 5

/** Do mais recente ao mais antigo; no mesmo ano, o criado por último primeiro. */
private val MAIS_RECENTE_PRIMEIRO = compareByDescending<AnoLetivoDomain> { it.ano }.thenByDescending { it.id }

/** O perfil com o aluno do [contexto]; null quando ele não existe mais no banco. */
internal fun montarPerfil(
    contexto: Contexto,
    alunos: List<AlunoDomain>,
    anos: List<AnoLetivoDomain>, // de todos os alunos
): PerfilAluno? {
    val aluno = alunos.find { it.id == contexto.alunoId } ?: return null
    val anosPorAluno = anos.groupBy { it.alunoId }.mapValues { (_, lista) -> lista.sortedWith(MAIS_RECENTE_PRIMEIRO) }
    val doAluno = anosPorAluno[aluno.id].orEmpty()
    val anoAberto = doAluno.find { it.id == contexto.anoLetivoId }

    return PerfilAluno(
        aluno = aluno,
        anoLetivo = anoAberto,
        anosLetivos = doAluno,
        alunos = alunos
            .sortedBy { it.id }
            .map { outro ->
                // Escolher o aluno que já está aberto não troca nada: o ano dele continua o aberto
                val ano = if (outro.id == aluno.id) {
                    anoAberto ?: doAluno.firstOrNull()
                } else {
                    anosPorAluno[outro.id]?.firstOrNull()
                }
                AlunoComAno(aluno = outro, anoLetivo = ano)
            },
    )
}

/**
 * Os períodos de um ano novo: 4 unidades, de fevereiro a meados de dezembro, com as férias de julho
 * no meio. É só o ponto de partida; os ajustes do ano abrem logo depois para conferir.
 */
internal fun periodosPadrao(ano: Int): List<PeriodoDomain> = listOf(
    LocalDate.of(ano, 2, 1) to LocalDate.of(ano, 4, 30),
    LocalDate.of(ano, 5, 1) to LocalDate.of(ano, 7, 15),
    LocalDate.of(ano, 8, 1) to LocalDate.of(ano, 9, 30),
    LocalDate.of(ano, 10, 1) to LocalDate.of(ano, 12, 15),
).mapIndexed { i, (inicio, fim) ->
    // anoLetivoId 0: o ano ainda não foi gravado, e o id dele só sai do banco
    PeriodoDomain(anoLetivoId = 0, periodo = i + 1, dataInicio = inicio, dataFim = fim)
}

/** O ano letivo novo, ainda sem id, com os [periodosPadrao]. Série em branco fica sem série. */
internal fun anoLetivoNovo(alunoId: Long, ano: Int, serie: String?): AnoLetivoDomain {
    val periodos = periodosPadrao(ano)
    return AnoLetivoDomain(
        ano = ano,
        serie = serie?.trim()?.ifEmpty { null },
        periodo = periodos,
        tipoPeriodo = TipoPeriodo.UNIDADE,
        qtdPeriodos = periodos.size,
        alunoId = alunoId,
    )
}

// ---------------------------------------------------------------------------
// Casos de uso
// ---------------------------------------------------------------------------

/** O aluno já tem um ano letivo com esse ano: nada foi criado. */
class AnoLetivoRepetidoException(ano: Int) : Exception("Já existe o ano letivo $ano")

class ObterPerfilAluno @Inject constructor(
    private val alunoRepository: AlunoRepository,
    private val anoLetivoRepository: AnoLetivoRepository,
) {
    /** Muda junto com o Contexto: depois de uma troca, o perfil já mostra o aluno e o ano novos. */
    operator fun invoke(contexto: Flow<Contexto>): Flow<PerfilAluno?> =
        combine(
            contexto,
            alunoRepository.observeAluno(),
            anoLetivoRepository.observeAll(),
            ::montarPerfil,
        )
}

class CriarAnoLetivo @Inject constructor(
    private val transacao: Transacao,
    private val anoLetivoRepository: AnoLetivoRepository,
    private val regraRepository: RegraAvaliacaoRepository,
    private val materiaRepository: MateriaRepository,
    private val disciplinaRepository: DisciplinaRepository,
    private val contextoRepository: ContextoRepository,
) {
    /**
     * Cria um ano letivo para o aluno com tudo no padrão: os [periodosPadrao], a [REGRA_PADRAO] e as
     * [MATERIAS_DO_ANO_NOVO] primeiras matérias do cadastro. Depois abre esse ano no app, no período
     * de [hoje], para os ajustes mostrarem ele.
     *
     * Grava tudo de uma vez: se algo falhar no meio, não sobra um ano pela metade. Se o aluno já tiver
     * esse ano, sai [AnoLetivoRepetidoException] sem gravar nada (a tela não deixa chegar aqui assim).
     */
    suspend operator fun invoke(alunoId: Long, ano: Int, serie: String?, hoje: LocalDate = LocalDate.now()) {
        if (anoLetivoRepository.observeByAluno(alunoId).first().any { it.ano == ano }) {
            throw AnoLetivoRepetidoException(ano)
        }
        val materias = materiaRepository.observeMaterias().first().take(MATERIAS_DO_ANO_NOVO)
        val novo = anoLetivoNovo(alunoId, ano, serie)

        val criado = transacao {
            val id = anoLetivoRepository.upsert(novo)
            val periodos = novo.periodo.map { periodo ->
                val doAno = periodo.copy(anoLetivoId = id)
                doAno.copy(id = anoLetivoRepository.upsertPeriodo(doAno))
            }
            // A regra padrão gravada: o cálculo e os ajustes passam a ler a do próprio ano
            regraRepository.upsert(REGRA_PADRAO.copy(anoLetivoId = id))
            materias.forEachIndexed { i, materia ->
                disciplinaRepository.upsert(novaDisciplina(materia, id, ordem = i + 1))
            }
            novo.copy(id = id, periodo = periodos)
        }

        contextoRepository.selecionarAnoLetivo(criado.id, criado.periodo.periodoAoAbrir(hoje))
    }
}

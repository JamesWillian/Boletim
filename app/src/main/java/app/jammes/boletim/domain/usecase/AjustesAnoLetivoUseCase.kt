package app.jammes.boletim.domain.usecase

import app.jammes.boletim.domain.model.AjustesAnoLetivo
import app.jammes.boletim.domain.model.AnoLetivoDomain
import app.jammes.boletim.domain.model.Contexto
import app.jammes.boletim.domain.model.DisciplinaDomain
import app.jammes.boletim.domain.model.MateriaDomain
import app.jammes.boletim.domain.model.RegraAvaliacaoDomain
import app.jammes.boletim.domain.repository.AnoLetivoRepository
import app.jammes.boletim.domain.repository.ContextoRepository
import app.jammes.boletim.domain.repository.DisciplinaRepository
import app.jammes.boletim.domain.repository.MateriaRepository
import app.jammes.boletim.domain.repository.RegraAvaliacaoRepository
import app.jammes.boletim.domain.repository.Transacao
import jakarta.inject.Inject
import kotlinx.coroutines.flow.first

// ---------------------------------------------------------------------------
// Regras puras
// ---------------------------------------------------------------------------

/**
 * As disciplinas das matérias que entram no ano agora. Copiam nome, cor e ícone da matéria, como
 * no cadastro, e vão para o fim das abas, na ordem das matérias.
 */
internal fun disciplinasNovas(original: AjustesAnoLetivo, materiaIds: Set<Long>): List<DisciplinaDomain> {
    val jaNoAno = original.disciplinas.map { it.materiaId }.toSet()
    val ultimaOrdem = original.disciplinas.maxOfOrNull { it.ordem } ?: 0

    return original.materias
        .filter { it.id in materiaIds && it.id !in jaNoAno }
        .mapIndexed { i, materia -> novaDisciplina(materia, original.anoLetivo.id, ordem = ultimaOrdem + 1 + i) }
}

/** A disciplina de uma matéria que entra no ano: copia nome, cor e ícone dela, como no cadastro. */
internal fun novaDisciplina(materia: MateriaDomain, anoLetivoId: Long, ordem: Int) = DisciplinaDomain(
    nome = materia.nome,
    cor = materia.cor,
    icone = materia.icone,
    ordem = ordem,
    materiaId = materia.id,
    anoLetivoId = anoLetivoId,
)

// ---------------------------------------------------------------------------
// Casos de uso
// ---------------------------------------------------------------------------

/** O Salvar apagaria avaliações ou faltas, então não gravou nada. */
class AjusteBloqueadoException(mensagem: String) : Exception(mensagem)

class ObterAjustesAnoLetivo @Inject constructor(
    private val anoLetivoRepository: AnoLetivoRepository,
    private val regraRepository: RegraAvaliacaoRepository,
    private val materiaRepository: MateriaRepository,
    private val disciplinaRepository: DisciplinaRepository,
) {
    /**
     * Uma leitura só, sem Flow: a tela edita uma cópia e grava tudo de uma vez no Salvar.
     * Devolve null quando o ano letivo do Contexto não existe mais no banco.
     */
    suspend operator fun invoke(contexto: Contexto): AjustesAnoLetivo? {
        val anoLetivo = anoLetivoRepository.observeByAluno(contexto.alunoId).first()
            .find { it.id == contexto.anoLetivoId }
            ?: return null

        return AjustesAnoLetivo(
            anoLetivo = anoLetivo,
            // Sem a padrão gravada, mostra a que o cálculo usa no lugar dela (e que é do ano 1)
            regra = regraRepository.observeByAnoLetivo(anoLetivo.id).first()
                .padraoDoAno()
                .copy(anoLetivoId = anoLetivo.id),
            materias = materiaRepository.observeMaterias().first(),
            disciplinas = disciplinaRepository.observeByAnoLetivo(anoLetivo.id).first(),
            lancamentosPorPeriodo = anoLetivoRepository.countLancamentosByPeriodo(anoLetivo.id),
            lancamentosPorDisciplina = disciplinaRepository.countLancamentosByDisciplina(anoLetivo.id),
        )
    }
}

class SalvarAjustesAnoLetivo @Inject constructor(
    private val transacao: Transacao,
    private val anoLetivoRepository: AnoLetivoRepository,
    private val regraRepository: RegraAvaliacaoRepository,
    private val disciplinaRepository: DisciplinaRepository,
    private val contextoRepository: ContextoRepository,
) {
    /**
     * Grava os ajustes de uma vez, comparando com o [original] que a tela leu.
     *
     * Período ou disciplina que sai do ano é apagado, e as avaliações e faltas iriam junto. Então,
     * se algum deles já tiver lançamentos, nada é gravado e sai [AjusteBloqueadoException]. A tela
     * não deixa chegar aqui assim; a conta é refeita no banco só para garantir.
     *
     * @param anoLetivo já com os períodos que devem existir, em ordem (id 0 = período novo).
     * @param materiaIds as matérias que entram no ano.
     */
    suspend operator fun invoke(
        original: AjustesAnoLetivo,
        anoLetivo: AnoLetivoDomain,
        regra: RegraAvaliacaoDomain,
        materiaIds: Set<Long>,
    ) {
        require(anoLetivo.periodo.isNotEmpty()) { "O ano letivo precisa de pelo menos um período" }

        val ficam = anoLetivo.periodo.map { it.id }.toSet()
        val periodosQueSaem = original.anoLetivo.periodo.filter { it.id !in ficam }
        val disciplinasQueSaem = original.disciplinas.filter { it.materiaId !in materiaIds }
        val (novos, existentes) = anoLetivo.periodo.partition { it.id == 0L }

        val periodosGravados = transacao {
            // Conferido dentro da transação, com o banco como está agora. A exceção desfaz a transação.
            val porPeriodo = anoLetivoRepository.countLancamentosByPeriodo(anoLetivo.id)
            if (periodosQueSaem.any { porPeriodo[it.id]?.vazio == false }) {
                throw AjusteBloqueadoException("Um período com avaliações ou faltas não pode sair do ano")
            }
            val porDisciplina = disciplinaRepository.countLancamentosByDisciplina(anoLetivo.id)
            if (disciplinasQueSaem.any { porDisciplina[it.id]?.vazio == false }) {
                throw AjusteBloqueadoException("Uma matéria com avaliações ou faltas não pode sair do ano")
            }

            periodosQueSaem.forEach { anoLetivoRepository.deletePeriodo(it) }
            // Os que já existem antes dos novos: o número de um período novo nunca bate com o de
            // um que ainda está no banco (ano + número é único)
            val ids = (existentes + novos).map { anoLetivoRepository.upsertPeriodo(it) }
            anoLetivoRepository.upsert(anoLetivo)
            regraRepository.upsert(regra)
            disciplinasQueSaem.forEach { disciplinaRepository.delete(it) }
            disciplinasNovas(original, materiaIds).forEach { disciplinaRepository.upsert(it) }
            ids
        }

        // O período aberto no app saiu do ano: passa para o último que ficou
        val contexto = contextoRepository.contexto.first()
        if (contexto != null && periodosQueSaem.any { it.id == contexto.periodoId }) {
            contextoRepository.selecionarPeriodo(periodosGravados.last())
        }
    }
}

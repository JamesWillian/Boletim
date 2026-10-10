package app.jammes.boletim.domain.usecase

import app.jammes.boletim.domain.model.AnoLetivoDomain
import app.jammes.boletim.domain.model.AvaliacaoDomain
import app.jammes.boletim.domain.model.AvaliacoesDoPeriodo
import app.jammes.boletim.domain.model.Contexto
import app.jammes.boletim.domain.model.DisciplinaDetalhe
import app.jammes.boletim.domain.model.DisciplinaDomain
import app.jammes.boletim.domain.model.FaltaDomain
import app.jammes.boletim.domain.model.FaltasDoPeriodo
import app.jammes.boletim.domain.model.RegraAvaliacaoDomain
import app.jammes.boletim.domain.model.TipoPeriodo
import app.jammes.boletim.domain.model.noFiltro
import app.jammes.boletim.domain.repository.AnoLetivoRepository
import app.jammes.boletim.domain.repository.AvaliacaoRepository
import app.jammes.boletim.domain.repository.DisciplinaRepository
import app.jammes.boletim.domain.repository.FaltaRepository
import app.jammes.boletim.domain.repository.RegraAvaliacaoRepository
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class ObterDisciplinaDetalhe @Inject constructor(
    private val disciplinaRepository: DisciplinaRepository,
    private val avaliacaoRepository: AvaliacaoRepository,
    private val faltaRepository: FaltaRepository,
    private val anoLetivoRepository: AnoLetivoRepository,
    private val regraRepository: RegraAvaliacaoRepository,
) {
    /** Emite null quando a disciplina não existe mais no banco. */
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(disciplinaId: Long, contexto: Flow<Contexto>): Flow<DisciplinaDetalhe?> {
        // Como no ObterBoletim: as avaliações de todos os períodos vêm de uma vez,
        // então trocar o chip de período só refaz a conta em memória.
        val periodoId = contexto.map { it.periodoId }.distinctUntilChanged()

        return disciplinaRepository.observeById(disciplinaId)
            // O Room reemite a cada escrita na tabela, mesmo em outra linha
            .distinctUntilChanged()
            .flatMapLatest { disciplina ->
                if (disciplina == null) flowOf(null)
                else combine(
                    avaliacaoRepository.observeByDisciplina(disciplinaId),
                    faltaRepository.observeByDisciplina(disciplinaId),
                    regraRepository.observeByAnoLetivo(disciplina.anoLetivoId),
                    // Os períodos dão nome e ordem aos grupos de avaliações e de faltas
                    anoLetivoRepository.observeById(disciplina.anoLetivoId),
                    periodoId,
                ) { avaliacoes, faltas, regras, anoLetivo, periodo ->
                    montar(disciplina, anoLetivo, avaliacoes, faltas, regras.paraDisciplina(disciplina.id), periodo)
                }
            }
    }

    // Mesma conta e mesma regra do card no Boletim, para a média e as faltas das duas telas baterem
    private fun montar(
        disciplina: DisciplinaDomain,
        anoLetivo: AnoLetivoDomain?,
        avaliacoes: List<AvaliacaoDomain>,
        faltas: List<FaltaDomain>,
        regra: RegraAvaliacaoDomain,
        periodoId: Long?,
    ): DisciplinaDetalhe {
        val periodos = anoLetivo?.periodo.orEmpty()
        val media = calcularMediaNoFiltro(avaliacoes, regra, periodoId)

        // No ano letivo inteiro entram todos os períodos; num período, só ele. Avaliações e faltas
        // se agrupam do mesmo jeito, e período sem nada lançado fica de fora.
        val noFiltro = periodos.noFiltro(periodoId)
        val avaliacoesPorPeriodo = noFiltro
            .map { periodo -> AvaliacoesDoPeriodo(periodo, avaliacoes.filter { it.periodoId == periodo.id }) }
            .filter { it.avaliacoes.isNotEmpty() }
        val faltasPorPeriodo = noFiltro
            .map { periodo -> FaltasDoPeriodo(periodo, faltas.filter { it.periodoId == periodo.id }) }
            .filter { it.faltas.isNotEmpty() }

        return DisciplinaDetalhe(
            disciplina = disciplina,
            periodoId = periodoId,
            tipoPeriodo = anoLetivo?.tipoPeriodo ?: TipoPeriodo.UNIDADE,
            periodos = periodos,
            regra = regra,
            avaliacoesPorPeriodo = avaliacoesPorPeriodo,
            faltasPorPeriodo = faltasPorPeriodo,
            media = media,
            status = statusDaMedia(media, regra),
            faltas = faltas.sumOf { it.qtdAulas },
            limiteFaltas = CalcularFrequencia.limiteFaltas(disciplina.totalAulas, regra),
        )
    }
}

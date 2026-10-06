package app.jammes.boletim.domain.usecase

import app.jammes.boletim.domain.model.AvaliacaoDomain
import app.jammes.boletim.domain.model.Contexto
import app.jammes.boletim.domain.model.DisciplinaDetalhe
import app.jammes.boletim.domain.model.DisciplinaDomain
import app.jammes.boletim.domain.model.RegraAvaliacaoDomain
import app.jammes.boletim.domain.repository.AvaliacaoRepository
import app.jammes.boletim.domain.repository.DisciplinaRepository
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
                    regraRepository.observeByAnoLetivo(disciplina.anoLetivoId),
                    periodoId,
                ) { avaliacoes, regras, periodo ->
                    montar(disciplina, avaliacoes, regras.paraDisciplina(disciplina.id), periodo)
                }
            }
    }

    // Mesmo filtro e mesma regra do card no Boletim, para a média das duas telas bater
    private fun montar(
        disciplina: DisciplinaDomain,
        avaliacoes: List<AvaliacaoDomain>,
        regra: RegraAvaliacaoDomain,
        periodoId: Long,
    ): DisciplinaDetalhe {
        val doPeriodo = avaliacoes.filter { it.periodoId == periodoId }
        val media = CalcularMediaDisciplina(doPeriodo, regra)

        return DisciplinaDetalhe(
            disciplina = disciplina,
            periodoId = periodoId,
            regra = regra,
            avaliacoes = doPeriodo,
            media = media,
            status = statusDaMedia(media, regra),
        )
    }
}

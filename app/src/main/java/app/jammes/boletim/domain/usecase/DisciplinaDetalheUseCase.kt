package app.jammes.boletim.domain.usecase

import app.jammes.boletim.domain.model.AnoLetivoDomain
import app.jammes.boletim.domain.model.AvaliacaoDomain
import app.jammes.boletim.domain.model.AvaliacoesDoPeriodo
import app.jammes.boletim.domain.model.Contexto
import app.jammes.boletim.domain.model.DisciplinaDetalhe
import app.jammes.boletim.domain.model.DisciplinaDomain
import app.jammes.boletim.domain.model.FaltaDomain
import app.jammes.boletim.domain.model.Fechamento
import app.jammes.boletim.domain.model.FaltasDoPeriodo
import app.jammes.boletim.domain.model.PeriodoDomain
import app.jammes.boletim.domain.model.RegraAvaliacaoDomain
import app.jammes.boletim.domain.model.TipoAvaliacao
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
        val calculo = calcularMediaNoFiltro(avaliacoes, regra, periodoId)

        // As faltas se agrupam como as avaliações: os períodos do filtro, sem os que não têm nada
        val (avaliacoesPorPeriodo, finais) = agruparAvaliacoes(avaliacoes, periodos, periodoId)
        val faltasPorPeriodo = periodos.noFiltro(periodoId)
            .map { periodo -> FaltasDoPeriodo(periodo, faltas.filter { it.periodoId == periodo.id }) }
            .filter { it.faltas.isNotEmpty() }
        val totalFaltas = faltas.sumOf { it.qtdAulas }
        val limiteFaltas = CalcularFrequencia.limiteFaltas(disciplina.totalAulas, regra)

        return DisciplinaDetalhe(
            disciplina = disciplina,
            periodoId = periodoId,
            tipoPeriodo = anoLetivo?.tipoPeriodo ?: TipoPeriodo.UNIDADE,
            periodos = periodos,
            regra = regra,
            avaliacoesPorPeriodo = avaliacoesPorPeriodo,
            finais = finais,
            faltasPorPeriodo = faltasPorPeriodo,
            media = calculo.media,
            mediaRegular = calculo.regular,
            substituta = calculo.substituta,
            status = statusDaMedia(calculo.media, regra),
            faltas = totalFaltas,
            limiteFaltas = limiteFaltas,
            // Finalizar fecha a disciplina toda, não um período: é a conta do ano em qualquer filtro
            fechamento = fecharAno(avaliacoes, periodos, regra, totalFaltas, limiteFaltas),
        )
    }
}

/**
 * Como o ano fecha com as avaliações de agora: a média do ano letivo inteiro (a mesma do filtro "Ano
 * letivo", com a final) contra a mínima, e as [faltas] contra o limite. null quando nenhum dos
 * [periodos] tem nota: sem média do ano não há como dizer se passou, nem com a final lançada.
 */
internal fun fecharAno(
    avaliacoes: List<AvaliacaoDomain>,
    periodos: List<PeriodoDomain>,
    regra: RegraAvaliacaoDomain,
    faltas: Int,
    limiteFaltas: Int?,
): Fechamento? {
    // Sem nota é sem média, como o CalcularMediaAnual vê: a recuperação sozinha já conta, a final
    // não, porque é do ano e não de um período
    val semNota = periodos.filter { periodo ->
        val doPeriodo = avaliacoes.filter { it.periodoId == periodo.id && it.tipo != TipoAvaliacao.FINAL }
        CalcularMediaDoPeriodo(doPeriodo, regra).media == null
    }
    if (semNota.size == periodos.size) return null
    val media = CalcularMediaAnual(avaliacoes, regra).media ?: return null

    return Fechamento(
        mediaDoAno = media,
        mediaMinima = regra.mediaMinima,
        faltas = faltas,
        limiteFaltas = limiteFaltas,
        periodosSemNota = semNota,
    )
}

/**
 * As avaliações como a tela lista: por período (os do filtro: no ano letivo inteiro, todos; num
 * período, só ele), sem os que não têm nenhuma, e a final à parte. A final é do ano, então fica fora
 * dos períodos mesmo que tenha ficado com um, e só aparece no ano letivo inteiro.
 */
internal fun agruparAvaliacoes(
    avaliacoes: List<AvaliacaoDomain>,
    periodos: List<PeriodoDomain>,
    periodoId: Long?,
): Pair<List<AvaliacoesDoPeriodo>, List<AvaliacaoDomain>> {
    val (finais, dosPeriodos) = avaliacoes.partition { it.tipo == TipoAvaliacao.FINAL }
    val porPeriodo = periodos.noFiltro(periodoId)
        .map { periodo -> AvaliacoesDoPeriodo(periodo, dosPeriodos.filter { it.periodoId == periodo.id }) }
        .filter { it.avaliacoes.isNotEmpty() }
    return porPeriodo to if (periodoId == null) finais else emptyList()
}

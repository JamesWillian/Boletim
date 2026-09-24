package app.jammes.boletim.domain.usecase

import app.jammes.boletim.domain.model.AvaliacaoDomain
import app.jammes.boletim.domain.model.Boletim
import app.jammes.boletim.domain.model.Contexto
import app.jammes.boletim.domain.model.DisciplinaDados
import app.jammes.boletim.domain.model.DisciplinaResumo
import app.jammes.boletim.domain.model.RegraAvaliacaoDomain
import app.jammes.boletim.domain.model.StatusDisciplina
import app.jammes.boletim.domain.model.TipoArredondamento
import app.jammes.boletim.domain.model.TipoAvaliacao
import app.jammes.boletim.domain.model.TipoMedia
import app.jammes.boletim.domain.repository.DisciplinaRepository
import app.jammes.boletim.domain.repository.RegraAvaliacaoRepository
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.collections.filter

// ---------------------------------------------------------------------------
// Cálculos puros — sem repository, sem Flow. É aqui que os testes vão morar.
// ---------------------------------------------------------------------------

object CalcularMediaDisciplina {
    operator fun invoke(avaliacoes: List<AvaliacaoDomain>, regra: RegraAvaliacaoDomain): Double? {
        // TODO: recuperação e arredondamento entram aqui
        val validas = avaliacoes.filter { it.tipo == TipoAvaliacao.NORMAL }
        if (validas.isEmpty()) return null

        val mediaRaw = when (regra.tipoMedia) {
            TipoMedia.SIMPLES -> validas.map { it.nota }.average()
            TipoMedia.PONDERADA -> {
                val pesoTotal = validas.sumOf { it.peso }
                if (pesoTotal == 0.0) return null
                validas.sumOf { it.nota * it.peso } / pesoTotal
            }
            TipoMedia.SOMA -> validas.sumOf { it.nota }
        }

        return arredondarMedia(mediaRaw, regra.arredondamento)
    }
}

val DOIS = BigDecimal(2)

fun arredondarMedia(media: Double, tipo: TipoArredondamento): Double {

    val limpa = media.toBigDecimal().setScale(6, RoundingMode.HALF_UP)

    val arredondada = when (tipo) {
        TipoArredondamento.NENHUM -> limpa.setScale(2, RoundingMode.HALF_UP)
        TipoArredondamento.MEIO_PONTO -> limpa.multiply(DOIS)
            .setScale(0, RoundingMode.HALF_UP)
            .divide(DOIS)
        TipoArredondamento.INTEIRO -> limpa.setScale(0, RoundingMode.HALF_UP)
        TipoArredondamento.CIMA -> limpa.setScale(1, RoundingMode.CEILING)
        TipoArredondamento.BAIXO ->  limpa.setScale(1, RoundingMode.FLOOR)
    }

    return arredondada.toDouble()
}

object CalcularFrequencia {
    /** Faltas permitidas no ano. null enquanto totalAulas não foi informado. */
    fun limiteFaltas(totalAulas: Int?, regra: RegraAvaliacaoDomain): Int? =
        totalAulas?.let { (it * (100 - regra.frequenciaMinima) / 100).toInt() }
}

private const val MARGEM_ATENCAO = 1.0

fun statusDaMedia(media: Double?, regra: RegraAvaliacaoDomain): StatusDisciplina = when {
    media == null -> StatusDisciplina.SEM_NOTA
    media < regra.mediaMinima -> StatusDisciplina.ABAIXO
    media < regra.mediaMinima + MARGEM_ATENCAO -> StatusDisciplina.ATENCAO
    else -> StatusDisciplina.APROVADO
}

private val REGRA_PADRAO = RegraAvaliacaoDomain(
    anoLetivoId = 1, disciplinaId = null,
    mediaMinima = 7.0, mediaRecuperacao = 5.0, frequenciaMinima = 75.0,
    tipoMedia = TipoMedia.SIMPLES
)

/** Exceção da disciplina se existir, senão a regra padrão do ano (disciplinaId nulo). */
private fun List<RegraAvaliacaoDomain>.paraDisciplina(disciplinaId: Long): RegraAvaliacaoDomain =
    firstOrNull { it.disciplinaId == disciplinaId }
        ?: firstOrNull { it.disciplinaId == null } // CriarAnoLetivo garante que a padrão existe
        ?: REGRA_PADRAO

// ---------------------------------------------------------------------------
// Caso de uso
// ---------------------------------------------------------------------------

class ObterBoletim @Inject constructor(
    private val disciplinaRepository: DisciplinaRepository,
    private val regraRepository: RegraAvaliacaoRepository,
) {
    private data class DadosDoAno(
        val anoLetivoId: Long,
        val disciplinas: List<DisciplinaDados>,
        val regras: List<RegraAvaliacaoDomain>,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(contexto: Flow<Contexto>): Flow<Boletim> {
        // Só a troca de ano vai no Room. O @Relation já traz as avaliações
        // de todos os períodos, então trocar o chip de período recalcula em memória.
        val dadosDoAno = contexto
            .map { it.anoLetivoId }
            .distinctUntilChanged()
            .flatMapLatest { anoId ->
                combine(
                    disciplinaRepository.observeBoletim(anoId),
                    regraRepository.observeByAnoLetivo(anoId),
                ) { disciplinas, regras ->
                    DadosDoAno(anoId, disciplinas, regras)
                }
            }

        return combine(dadosDoAno, contexto) { dados, ctx ->
            // Descarta o instante em que o ano mudou mas a query nova ainda não emitiu
            if (dados.anoLetivoId != ctx.anoLetivoId) null
            else montar(dados, ctx.periodoId)
        }.filterNotNull()
    }

    private fun montar(dados: DadosDoAno, periodoId: Long): Boletim {
        val resumos = dados.disciplinas.map { d ->
            val regra = dados.regras.paraDisciplina(d.disciplinaId)
            val media = CalcularMediaDisciplina(
                avaliacoes = d.avaliacoes.filter { it.periodoId == periodoId },
                regra = regra,
            )
            DisciplinaResumo(
                id = d.disciplinaId,
                nome = d.nome,
                cor = d.cor,
                media = media,
                status = statusDaMedia(media, regra),
                faltas = d.faltas.sumOf { it.qtdAulas }, // ano inteiro, contra o limite anual
                limiteFaltas = CalcularFrequencia.limiteFaltas(d.totalAulas, regra),
            )
        }

        return Boletim(
            disciplinas = resumos,
            mediaGeral = resumos.mapNotNull { it.media }.takeIf { it.isNotEmpty() }?.average(),
            totalFaltas = resumos.sumOf { it.faltas },
        )
    }
}
package app.jammes.boletim.domain.usecase

import app.jammes.boletim.domain.model.AvaliacaoDomain
import app.jammes.boletim.domain.model.Boletim
import app.jammes.boletim.domain.model.Contexto
import app.jammes.boletim.domain.model.DisciplinaDados
import app.jammes.boletim.domain.model.DisciplinaResumo
import app.jammes.boletim.domain.model.MediaNoFiltro
import app.jammes.boletim.domain.model.PeriodoDomain
import app.jammes.boletim.domain.model.RegraAvaliacaoDomain
import app.jammes.boletim.domain.model.StatusDisciplina
import app.jammes.boletim.domain.model.Substituta
import app.jammes.boletim.domain.model.TipoArredondamento
import app.jammes.boletim.domain.model.TipoAvaliacao
import app.jammes.boletim.domain.model.TipoMedia
import app.jammes.boletim.domain.repository.AnoLetivoRepository
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

private const val ESCALA_DA_MEDIA = 10.0

/**
 * Quanto a nota valeria numa avaliação de 10, a escala da média mínima: 4 de 5 vira 8, 85 de 100
 * vira 8,5. É a conta das médias simples e ponderada, e a que pinta cada nota na lista.
 * Multiplica antes de dividir para notas redondas saírem exatas (57 de 100 dá 5,7, e não 5,699…).
 * Sem uma nota máxima válida não há como converter: null.
 */
fun notaNaEscalaDe10(nota: Double, notaMaxima: Double): Double? =
    if (notaMaxima > 0.0) nota * ESCALA_DA_MEDIA / notaMaxima else null

/**
 * A média regular: só a das avaliações normais, já arredondada pela regra. A recuperação e a final
 * não entram nela; elas tomam o lugar dela depois, em [CalcularMediaDoPeriodo] e [CalcularMediaAnual].
 */
object CalcularMediaDisciplina {
    private class NotaLancada(val nota: Double, val peso: Double)

    operator fun invoke(avaliacoes: List<AvaliacaoDomain>, regra: RegraAvaliacaoDomain): Double? {
        val validas = avaliacoes
            .filter { it.tipo == TipoAvaliacao.NORMAL }
            .mapNotNull { avaliacao ->
                // Sem nota = ainda não aconteceu, então fica fora da média
                val nota = avaliacao.nota ?: return@mapNotNull null
                // Na soma os pontos entram como foram lançados: a máxima é só a fatia de cada avaliação
                val valor = when (regra.tipoMedia) {
                    TipoMedia.SOMA -> nota
                    else -> notaNaEscalaDe10(nota, avaliacao.notaMaxima)
                }
                valor?.let { NotaLancada(it, avaliacao.peso) }
            }
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

/**
 * A média de um período: a regular, ou a nota da recuperação quando ela é maior. As [avaliacoes]
 * são só as do período.
 */
object CalcularMediaDoPeriodo {
    operator fun invoke(avaliacoes: List<AvaliacaoDomain>, regra: RegraAvaliacaoDomain): MediaNoFiltro =
        substituirSeMaior(CalcularMediaDisciplina(avaliacoes, regra), avaliacoes, TipoAvaliacao.RECUPERACAO, regra)
}

/**
 * Média do ano letivo: a média das médias de cada período (já com a recuperação de cada um), como no
 * boletim da escola, e a final no lugar dela quando a final é maior. Período ainda sem nota fica de
 * fora (não conta como zero), e o resultado passa pelo mesmo arredondamento da regra.
 */
object CalcularMediaAnual {
    operator fun invoke(avaliacoes: List<AvaliacaoDomain>, regra: RegraAvaliacaoDomain): MediaNoFiltro {
        val mediasDosPeriodos = avaliacoes
            // A final é do ano, não de um período, mesmo que tenha ficado com um
            .filter { it.periodoId != null && it.tipo != TipoAvaliacao.FINAL }
            .groupBy { it.periodoId }
            .values
            .mapNotNull { CalcularMediaDoPeriodo(it, regra).media }
        val regular = mediasDosPeriodos
            .takeIf { it.isNotEmpty() }
            ?.let { arredondarMedia(it.average(), regra.arredondamento) }

        return substituirSeMaior(regular, avaliacoes, TipoAvaliacao.FINAL, regra)
    }
}

/**
 * A [regular] com a recuperação ou a final (o [tipo]) no lugar, quando ela é maior. A nota vale como
 * uma média: vai para a escala de 10, mesmo na soma, e passa pelo arredondamento da regra. Só cabe
 * uma de cada; se o banco tiver mais, vale a maior. Agendada, sem nota, ainda não conta.
 */
private fun substituirSeMaior(
    regular: Double?,
    avaliacoes: List<AvaliacaoDomain>,
    tipo: TipoAvaliacao,
    regra: RegraAvaliacaoDomain,
): MediaNoFiltro {
    val nota = avaliacoes
        .filter { it.tipo == tipo }
        .mapNotNull { avaliacao -> avaliacao.nota?.let { notaNaEscalaDe10(it, avaliacao.notaMaxima) } }
        .maxOrNull()
        ?.let { arredondarMedia(it, regra.arredondamento) }
        ?: return MediaNoFiltro(media = regular, regular = regular, substituta = null)

    // Sem média regular (nenhuma normal com nota ainda), a recuperação ou a final é a média
    val substituiu = regular == null || nota > regular
    return MediaNoFiltro(
        media = if (substituiu) nota else regular,
        regular = regular,
        substituta = Substituta(tipo, nota, substituiu),
    )
}

/** A média no filtro do Contexto: a do período escolhido, ou a do ano letivo quando periodoId é nulo. */
fun calcularMediaNoFiltro(
    avaliacoes: List<AvaliacaoDomain>,
    regra: RegraAvaliacaoDomain,
    periodoId: Long?,
): MediaNoFiltro =
    if (periodoId == null) CalcularMediaAnual(avaliacoes, regra)
    else CalcularMediaDoPeriodo(avaliacoes.filter { it.periodoId == periodoId }, regra)

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

/** A regra que vale enquanto o ano não tem a sua gravada; também é a que um ano novo ganha. */
internal val REGRA_PADRAO = RegraAvaliacaoDomain(
    anoLetivoId = 1, disciplinaId = null,
    mediaMinima = 7.0, mediaRecuperacao = 5.0, frequenciaMinima = 75.0,
    tipoMedia = TipoMedia.SIMPLES
)

/** Exceção da disciplina se existir, senão a regra padrão do ano. */
internal fun List<RegraAvaliacaoDomain>.paraDisciplina(disciplinaId: Long): RegraAvaliacaoDomain =
    firstOrNull { it.disciplinaId == disciplinaId } ?: padraoDoAno()

/** A regra padrão do ano (disciplinaId nulo). Enquanto ela não for gravada, vale a REGRA_PADRAO. */
internal fun List<RegraAvaliacaoDomain>.padraoDoAno(): RegraAvaliacaoDomain =
    firstOrNull { it.disciplinaId == null } // CriarAnoLetivo garante que a padrão existe
        ?: REGRA_PADRAO

// ---------------------------------------------------------------------------
// Caso de uso
// ---------------------------------------------------------------------------

class ObterBoletim @Inject constructor(
    private val disciplinaRepository: DisciplinaRepository,
    private val regraRepository: RegraAvaliacaoRepository,
    private val anoLetivoRepository: AnoLetivoRepository,
) {
    private data class DadosDoAno(
        val anoLetivoId: Long,
        val disciplinas: List<DisciplinaDados>,
        val regras: List<RegraAvaliacaoDomain>,
        val periodos: List<PeriodoDomain>,
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
                    // Os períodos entram no fechamento do ano das finalizadas, como no detalhe
                    anoLetivoRepository.observeById(anoId),
                ) { disciplinas, regras, anoLetivo ->
                    DadosDoAno(anoId, disciplinas, regras, anoLetivo?.periodo.orEmpty())
                }
            }

        return combine(dadosDoAno, contexto) { dados, ctx ->
            // Descarta o instante em que o ano mudou mas a query nova ainda não emitiu
            if (dados.anoLetivoId != ctx.anoLetivoId) null
            else montar(dados, ctx.periodoId)
        }.filterNotNull()
    }

    private fun montar(dados: DadosDoAno, periodoId: Long?): Boletim {
        val resumos = dados.disciplinas.map { d ->
            val regra = dados.regras.paraDisciplina(d.disciplinaId)
            val calculo = calcularMediaNoFiltro(d.avaliacoes, regra, periodoId)
            val faltas = d.faltas.sumOf { it.qtdAulas } // ano inteiro, contra o limite anual
            val limiteFaltas = CalcularFrequencia.limiteFaltas(d.totalAulas, regra)
            DisciplinaResumo(
                id = d.disciplinaId,
                nome = d.nome,
                cor = d.cor,
                icone = d.icone,
                media = calculo.media,
                mediaRegular = calculo.regular,
                substituta = calculo.substituta,
                mediaMinima = regra.mediaMinima,
                status = statusDaMedia(calculo.media, regra),
                faltas = faltas,
                limiteFaltas = limiteFaltas,
                // Só depois de finalizada, e com a conta do detalhe: o card e a tela dela não divergem
                situacao = if (d.ativa) null else fecharAno(d.avaliacoes, dados.periodos, regra, faltas, limiteFaltas)?.situacao,
            )
        }

        val mediaGeral = resumos.mapNotNull { it.media }.takeIf { it.isNotEmpty() }?.average()
        val regraDoAno = dados.regras.padraoDoAno()

        return Boletim(
            periodoId = periodoId,
            disciplinas = resumos,
            mediaGeral = mediaGeral,
            mediaMinima = regraDoAno.mediaMinima,
            // A média geral chega sem arredondar: o status sai da mesma limpeza que a tela faz
            // ao mostrar o número, para a cor nunca contradizer o que está escrito
            status = statusDaMedia(
                mediaGeral?.let { arredondarMedia(it, TipoArredondamento.NENHUM) },
                regraDoAno,
            ),
            totalFaltas = resumos.sumOf { it.faltas },
        )
    }
}
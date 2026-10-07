package app.jammes.boletim.presentation.ui.anoletivo

import app.jammes.boletim.domain.model.AjustesAnoLetivo
import app.jammes.boletim.domain.model.AnoLetivoDomain
import app.jammes.boletim.domain.model.Lancamentos
import app.jammes.boletim.domain.model.PeriodoDomain
import app.jammes.boletim.domain.model.RegraAvaliacaoDomain
import app.jammes.boletim.domain.model.TipoArredondamento
import app.jammes.boletim.domain.model.TipoMedia
import app.jammes.boletim.domain.model.TipoPeriodo
import app.jammes.boletim.presentation.ui.disciplina.formatarNumero
import app.jammes.boletim.presentation.ui.disciplina.lerNumero
import java.io.Serializable
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Os campos da tela de ajustes, do jeito que estão digitados. É Serializable para o
 * rememberSaveable guardar: o que já foi editado sobrevive a girar a tela.
 */
data class FormularioAnoLetivo(
    val ano: String,
    val serie: String,
    val tipoPeriodo: TipoPeriodo,
    val quantidade: Int,
    // Pode ter mais itens que a quantidade: ao diminuir, os do fim ficam guardados e voltam com as
    // mesmas datas se ela aumentar de novo
    val periodos: List<DatasPeriodo>,
    val mediaMinima: String,
    val frequenciaMinima: String,
    val tipoMedia: TipoMedia,
    val arredondamento: TipoArredondamento,
    val materias: Set<Long>, // ids das matérias que entram no ano
) : Serializable {

    /** As datas dos períodos que estão na tela, um por período da quantidade. */
    val visiveis: List<DatasPeriodo>
        get() = periodos.take(quantidade)

    // O valor de cada campo, ou null enquanto ele estiver inválido
    val anoValor: Int?
        get() = ano.trim().toIntOrNull()?.takeIf { it in 1000..9999 }
    val mediaMinimaValor: Double?
        get() = lerNumero(mediaMinima)?.takeIf { it > 0 }
    val frequenciaMinimaValor: Double?
        get() = lerNumero(frequenciaMinima)?.takeIf { it in 0.0..100.0 }

    /** Um período a mais no fim: volta o último que tinha saído, ou cria um logo depois do último. */
    fun maisUmPeriodo(): FormularioAnoLetivo {
        if (quantidade >= MAX_PERIODOS) return this
        val guardado = periodos.size > quantidade
        return copy(
            quantidade = quantidade + 1,
            periodos = if (guardado) periodos else periodos + periodoSeguinte(periodos.last()),
        )
    }

    /** Tira o último período da tela. As datas dele ficam guardadas para o caso de ele voltar. */
    fun menosUmPeriodo(): FormularioAnoLetivo =
        if (quantidade > 1) copy(quantidade = quantidade - 1) else this

    fun comDatas(indice: Int, datas: DatasPeriodo): FormularioAnoLetivo =
        copy(periodos = periodos.toMutableList().also { it[indice] = datas })

    /** Sem os períodos guardados além da quantidade, para comparar com o que está gravado. */
    fun semGuardados(): FormularioAnoLetivo = copy(periodos = visiveis)

    /**
     * O que impede de tirar o último período da tela, ou null quando ele pode sair. Só um período
     * que já está gravado tem lançamentos: um criado agora sai sempre.
     */
    fun bloqueioParaDiminuir(ajustes: AjustesAnoLetivo): Lancamentos? =
        ajustes.anoLetivo.periodo.getOrNull(quantidade - 1)
            ?.let { ajustes.lancamentosPorPeriodo[it.id] }
            ?.takeIf { !it.vazio }

    /** O que vai para o banco, ou null enquanto algum campo estiver inválido. */
    fun paraGravar(ajustes: AjustesAnoLetivo): Gravacao? {
        val ano = anoValor ?: return null
        val mediaMinima = mediaMinimaValor ?: return null
        val frequenciaMinima = frequenciaMinimaValor ?: return null
        if (problemasDasDatas(visiveis).any { it != null }) return null

        val anoLetivo = ajustes.anoLetivo
        return Gravacao(
            anoLetivo = anoLetivo.copy(
                ano = ano,
                serie = serie.trim().ifEmpty { null },
                tipoPeriodo = tipoPeriodo,
                qtdPeriodos = quantidade,
                periodo = visiveis.mapIndexed { i, datas ->
                    PeriodoDomain(
                        // Na posição de um período gravado, é ele com as datas novas; depois, é novo
                        id = anoLetivo.periodo.getOrNull(i)?.id ?: 0L,
                        anoLetivoId = anoLetivo.id,
                        periodo = i + 1,
                        dataInicio = datas.inicio,
                        dataFim = datas.fim,
                    )
                },
            ),
            regra = ajustes.regra.copy(
                mediaMinima = mediaMinima,
                frequenciaMinima = frequenciaMinima,
                tipoMedia = tipoMedia,
                arredondamento = arredondamento,
            ),
            materiaIds = materias,
        )
    }

    companion object {
        /** Bimestres dão 4 por ano, trimestres 3, semestres 2: 6 já sobra. */
        const val MAX_PERIODOS = 6

        /** O formulário com o ano como está gravado. */
        fun de(ajustes: AjustesAnoLetivo): FormularioAnoLetivo {
            val anoLetivo = ajustes.anoLetivo
            // Se o banco tiver mais períodos que a quantidade, mostra todos: nenhum some sem a
            // pessoa ver. Se tiver menos, completa com datas de partida.
            val quantidade = maxOf(anoLetivo.qtdPeriodos, anoLetivo.periodo.size, 1)
            val periodos = anoLetivo.periodo.map { DatasPeriodo(it.dataInicio, it.dataFim) }.toMutableList()
            if (periodos.isEmpty()) {
                periodos += DatasPeriodo(LocalDate.of(anoLetivo.ano, 1, 1), LocalDate.of(anoLetivo.ano, 12, 31))
            }
            while (periodos.size < quantidade) periodos += periodoSeguinte(periodos.last())

            return FormularioAnoLetivo(
                ano = anoLetivo.ano.toString(),
                serie = anoLetivo.serie.orEmpty(),
                tipoPeriodo = anoLetivo.tipoPeriodo,
                quantidade = quantidade,
                periodos = periodos,
                mediaMinima = formatarNumero(ajustes.regra.mediaMinima),
                frequenciaMinima = formatarNumero(ajustes.regra.frequenciaMinima),
                tipoMedia = ajustes.regra.tipoMedia,
                arredondamento = ajustes.regra.arredondamento,
                materias = ajustes.disciplinas.map { it.materiaId }.toSet(),
            )
        }
    }
}

data class DatasPeriodo(val inicio: LocalDate, val fim: LocalDate) : Serializable

/** O ano letivo com os períodos que devem existir, a regra e as matérias: o que o Salvar grava. */
class Gravacao(
    val anoLetivo: AnoLetivoDomain,
    val regra: RegraAvaliacaoDomain,
    val materiaIds: Set<Long>,
)

enum class ProblemaDatas(val mensagem: String) {
    FIM_ANTES_DO_INICIO("O fim vem antes do início"),
    COMECA_ANTES_DO_FIM_DO_ANTERIOR("Começa antes do fim do período anterior"),
}

/**
 * Um item por período: null quando as datas dele estão certas. O fim não pode vir antes do início,
 * e um período só começa depois que o anterior acaba. Folga entre eles (as férias) pode.
 */
fun problemasDasDatas(periodos: List<DatasPeriodo>): List<ProblemaDatas?> =
    periodos.mapIndexed { i, periodo ->
        when {
            periodo.fim < periodo.inicio -> ProblemaDatas.FIM_ANTES_DO_INICIO
            i > 0 && periodo.inicio <= periodos[i - 1].fim -> ProblemaDatas.COMECA_ANTES_DO_FIM_DO_ANTERIOR
            else -> null
        }
    }

/**
 * Datas de um período criado ao aumentar a quantidade: começa no dia seguinte ao fim do anterior e
 * dura os mesmos dias que ele. É só um ponto de partida, para ajustar na tela.
 */
fun periodoSeguinte(anterior: DatasPeriodo): DatasPeriodo {
    val dias = ChronoUnit.DAYS.between(anterior.inicio, anterior.fim).coerceAtLeast(0)
    val inicio = anterior.fim.plusDays(1)
    return DatasPeriodo(inicio, inicio.plusDays(dias))
}

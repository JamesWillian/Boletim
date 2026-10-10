package app.jammes.boletim.presentation.ui.anoletivo

import app.jammes.boletim.domain.model.AjustesAnoLetivo
import app.jammes.boletim.domain.model.AnoLetivoDomain
import app.jammes.boletim.domain.model.Lancamentos
import app.jammes.boletim.domain.model.PeriodoDomain
import app.jammes.boletim.domain.model.RegraAvaliacaoDomain
import app.jammes.boletim.domain.model.TipoArredondamento
import app.jammes.boletim.domain.model.TipoMedia
import app.jammes.boletim.domain.model.TipoPeriodo
import java.io.Serializable
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Os campos da tela de ajustes, do jeito que estão na tela. É Serializable para o
 * rememberSaveable guardar: o que já foi editado sobrevive a girar a tela.
 *
 * Vem dividido em uma parte por seção. Mexer numa parte cria outro formulário com as outras
 * intactas (os mesmos objetos), e assim o Compose pula as seções que não mudaram.
 */
data class FormularioAnoLetivo(
    val identificacao: Identificacao,
    val periodos: Periodos,
    val regras: Regras,
    val materias: Set<Long>, // ids das matérias que entram no ano
) : Serializable {

    /** O ano e a série, como estão digitados. */
    data class Identificacao(
        val ano: String,
        val serie: String,
    ) : Serializable {

        /** O ano, ou null enquanto ele não tiver 4 dígitos. */
        val anoValor: Int?
            get() = ano.trim().toIntOrNull()?.takeIf { it in 1000..9999 }

        // Espaço sobrando nas pontas não muda o que é gravado, então não conta
        internal fun alteracoes(gravada: Identificacao): Int = listOf(
            ano.trim() != gravada.ano.trim(),
            serie.trim() != gravada.serie.trim(),
        ).count { it }
    }

    /** Como o ano se divide: o tipo de período, quantos são e as datas de cada um. */
    data class Periodos(
        val tipo: TipoPeriodo,
        val quantidade: Int,
        // Pode ter mais itens que a quantidade: ao diminuir, os do fim ficam guardados e voltam
        // com as mesmas datas se ela aumentar de novo
        val datas: List<DatasPeriodo>,
    ) : Serializable {

        /** As datas dos períodos que estão na tela, um por período da quantidade. */
        val visiveis: List<DatasPeriodo>
            get() = datas.take(quantidade)

        /** Um período a mais no fim: volta o último que tinha saído, ou cria um logo depois do último. */
        fun maisUm(): Periodos {
            if (quantidade >= MAX_PERIODOS) return this
            val guardado = datas.size > quantidade
            return copy(
                quantidade = quantidade + 1,
                datas = if (guardado) datas else datas + periodoSeguinte(datas.last()),
            )
        }

        /** Tira o último período da tela. As datas dele ficam guardadas para o caso de ele voltar. */
        fun menosUm(): Periodos =
            if (quantidade > 1) copy(quantidade = quantidade - 1) else this

        fun comDatas(indice: Int, novas: DatasPeriodo): Periodos =
            copy(datas = datas.toMutableList().also { it[indice] = novas })

        /**
         * O que impede de tirar o último período da tela, ou null quando ele pode sair. Só um
         * período que já está gravado tem lançamentos: um criado agora sai sempre.
         */
        fun bloqueioParaDiminuir(ajustes: AjustesAnoLetivo): Lancamentos? =
            ajustes.anoLetivo.periodo.getOrNull(quantidade - 1)
                ?.let { ajustes.lancamentosPorPeriodo[it.id] }
                ?.takeIf { !it.vazio }

        // O tipo e a quantidade contam uma cada; depois, cada período que está nos dois lados
        // e mudou de data. Os guardados além da quantidade não contam.
        internal fun alteracoes(gravados: Periodos): Int =
            listOf(tipo != gravados.tipo, quantidade != gravados.quantidade).count { it } +
                visiveis.zip(gravados.visiveis).count { (agora, antes) -> agora != antes }
    }

    /**
     * As regras de média do ano. Os números só mudam pelo − e +, em passos fixos e dentro dos
     * limites, então nunca ficam inválidos.
     */
    data class Regras(
        val mediaMinima: Double,
        val frequenciaMinima: Double,
        val tipoMedia: TipoMedia,
        val arredondamento: TipoArredondamento,
    ) : Serializable {

        /** A média mínima um passo acima ([sentido] 1) ou abaixo (-1). */
        fun comMediaMinima(sentido: Int): Regras =
            copy(mediaMinima = passo(mediaMinima, PASSO_DA_MEDIA, sentido, FAIXA_DA_MEDIA))

        /** A frequência mínima um passo acima ([sentido] 1) ou abaixo (-1). */
        fun comFrequenciaMinima(sentido: Int): Regras =
            copy(frequenciaMinima = passo(frequenciaMinima, PASSO_DA_FREQUENCIA, sentido, FAIXA_DA_FREQUENCIA))

        internal fun alteracoes(gravadas: Regras): Int = listOf(
            mediaMinima != gravadas.mediaMinima,
            frequenciaMinima != gravadas.frequenciaMinima,
            tipoMedia != gravadas.tipoMedia,
            arredondamento != gravadas.arredondamento,
        ).count { it }
    }

    /** Quantas coisas mudaram em cada seção, comparando com o [gravado]. */
    fun alteracoes(gravado: FormularioAnoLetivo) = Alteracoes(
        ano = identificacao.alteracoes(gravado.identificacao),
        periodos = periodos.alteracoes(gravado.periodos),
        regras = regras.alteracoes(gravado.regras),
        // Cada matéria que entra ou sai do ano conta uma
        materias = (materias - gravado.materias).size + (gravado.materias - materias).size,
    )

    /** O que impede de salvar, do jeito que a barra de baixo mostra; null quando dá para gravar. */
    fun problema(): String? {
        if (identificacao.anoValor == null) return "O ano precisa ter 4 dígitos"

        val comProblema = problemasDasDatas(periodos.visiveis).indexOfFirst { it != null }
        if (comProblema >= 0) {
            val tipo = periodos.tipo
            return "Confira as datas ${if (tipo.feminino) "da" else "do"} ${nomeDoPeriodo(comProblema + 1, tipo)}"
        }
        return null
    }

    /** O que vai para o banco, ou null enquanto houver um [problema]. */
    fun paraGravar(ajustes: AjustesAnoLetivo): Gravacao? {
        if (problema() != null) return null
        val ano = identificacao.anoValor ?: return null

        val anoLetivo = ajustes.anoLetivo
        return Gravacao(
            anoLetivo = anoLetivo.copy(
                ano = ano,
                serie = identificacao.serie.trim().ifEmpty { null },
                tipoPeriodo = periodos.tipo,
                qtdPeriodos = periodos.quantidade,
                periodo = periodos.visiveis.mapIndexed { i, datas ->
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
                mediaMinima = regras.mediaMinima,
                frequenciaMinima = regras.frequenciaMinima,
                tipoMedia = regras.tipoMedia,
                arredondamento = regras.arredondamento,
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
            val datas = anoLetivo.periodo.map { DatasPeriodo(it.dataInicio, it.dataFim) }.toMutableList()
            if (datas.isEmpty()) {
                datas += DatasPeriodo(LocalDate.of(anoLetivo.ano, 1, 1), LocalDate.of(anoLetivo.ano, 12, 31))
            }
            while (datas.size < quantidade) datas += periodoSeguinte(datas.last())

            val regra = ajustes.regra
            return FormularioAnoLetivo(
                identificacao = Identificacao(
                    ano = anoLetivo.ano.toString(),
                    serie = anoLetivo.serie.orEmpty(),
                ),
                periodos = Periodos(
                    tipo = anoLetivo.tipoPeriodo,
                    quantidade = quantidade,
                    datas = datas,
                ),
                regras = Regras(
                    mediaMinima = regra.mediaMinima,
                    frequenciaMinima = regra.frequenciaMinima,
                    tipoMedia = regra.tipoMedia,
                    arredondamento = regra.arredondamento,
                ),
                materias = ajustes.disciplinas.map { it.materiaId }.toSet(),
            )
        }
    }
}

/** Quantas alterações cada seção tem. A barra de baixo mostra o [total]; cada seção, um ponto. */
data class Alteracoes(
    val ano: Int,
    val periodos: Int,
    val regras: Int,
    val materias: Int,
) {
    val total: Int
        get() = ano + periodos + regras + materias
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

// Os passos do − e do +. A média fica na escala de 0 a 10 das médias do app (a mesma da barra e do
// anel); a frequência é uma porcentagem.
internal const val PASSO_DA_MEDIA = 0.5
internal val FAIXA_DA_MEDIA = 0.5..10.0
internal const val PASSO_DA_FREQUENCIA = 5.0
internal val FAIXA_DA_FREQUENCIA = 0.0..100.0

/**
 * Um toque no − ou no +: vai para o próximo múltiplo de [tamanho] no [sentido] (1 sobe, -1 desce),
 * sem sair da [faixa]. Um valor fora da grade cai no vizinho dela: 6,75 em passos de 0,5 vai para
 * 7 subindo e para 6,5 descendo.
 */
internal fun passo(valor: Double, tamanho: Double, sentido: Int, faixa: ClosedFloatingPointRange<Double>): Double {
    val passos = valor / tamanho
    // A folga evita que o ruído do Double (6,9999…) faça pular um passo inteiro
    val alvo = if (sentido > 0) floor(passos + FOLGA) + 1 else ceil(passos - FOLGA) - 1
    return (alvo * tamanho).coerceIn(faixa)
}

private const val FOLGA = 1e-9

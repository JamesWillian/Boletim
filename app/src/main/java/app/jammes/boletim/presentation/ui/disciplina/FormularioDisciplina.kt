package app.jammes.boletim.presentation.ui.disciplina

import app.jammes.boletim.domain.model.DisciplinaDomain
import app.jammes.boletim.domain.model.EdicaoDisciplina
import app.jammes.boletim.domain.model.IconeMateria
import app.jammes.boletim.domain.model.RegraAvaliacaoDomain
import app.jammes.boletim.presentation.ui.anoletivo.DatasPeriodo
import app.jammes.boletim.presentation.ui.anoletivo.FormularioAnoLetivo
import java.io.Serializable
import java.time.LocalDate

/**
 * Os campos da edição da disciplina, do jeito que estão na tela. É Serializable para o
 * rememberSaveable guardar: o que já foi editado sobrevive a girar a tela.
 *
 * O nome, o professor e o total de aulas chegam já conferidos pelos sheets, e o período pelo
 * calendário; o formulário guarda e compara com o que está gravado.
 *
 * As [regras] são as próprias da disciplina, que valem no lugar das do ano letivo. As regras usam
 * as mesmas partes e os mesmos passos dos ajustes do ano.
 */
data class FormularioDisciplina(
    val nome: String,
    val professor: String, // vazio: sem professor
    val cor: Int, // o índice na paleta
    val icone: IconeMateria,
    val totalAulas: Int?, // null: não informado, e as faltas ficam sem limite
    val periodo: DatasPeriodo?, // null: o ano letivo inteiro
    val regras: FormularioAnoLetivo.Regras? = null, // null: segue as regras do ano letivo
) : Serializable {

    /**
     * Quantas coisas mudaram em cada seção, comparando com o [gravado]. As regras contam cada
     * número ou escolha que mudou, como nos ajustes; para isso, as do ano ([doAno]) entram no lugar
     * das próprias que não existem.
     */
    fun alteracoes(gravado: FormularioDisciplina, doAno: FormularioAnoLetivo.Regras) = AlteracoesDaDisciplina(
        // Espaço sobrando nas pontas não muda o que é gravado, então não conta
        disciplina = listOf(
            nome.trim() != gravado.nome.trim(),
            professor.trim() != gravado.professor.trim(),
        ).count { it },
        cor = if (cor != gravado.cor) 1 else 0,
        icone = if (icone != gravado.icone) 1 else 0,
        aulas = listOf(totalAulas != gravado.totalAulas, periodo != gravado.periodo).count { it },
        regras = when (regras) {
            gravado.regras -> 0
            // Tirar uma própria igual às do ano não muda nenhum número, mas ainda é uma alteração
            else -> maxOf(1, (regras ?: doAno).alteracoes(gravado.regras ?: doAno))
        },
    )

    /** O que impede de salvar, do jeito que a barra de baixo mostra; null quando dá para gravar. */
    fun problema(): String? =
        if (nome.isBlank()) "A disciplina precisa de um nome" else null

    /** A disciplina pronta para gravar, a partir da [original], ou null enquanto houver um [problema]. */
    fun paraGravar(original: DisciplinaDomain): DisciplinaDomain? {
        if (problema() != null) return null
        return original.copy(
            nome = nome.trim(),
            professor = professor.trim().ifEmpty { null },
            cor = cor,
            icone = icone,
            totalAulas = totalAulas,
            periodoInicio = periodo?.inicio,
            periodoFim = periodo?.fim,
        )
    }

    /**
     * A regra própria pronta para gravar, ou null quando a disciplina segue a do ano (aí a própria,
     * se havia, é apagada). Uma nova nasce da do ano, só para esta disciplina, e leva junto o que a
     * tela não edita (a média de recuperação, por exemplo); nunca fica com o id da do ano.
     */
    fun regraParaGravar(edicao: EdicaoDisciplina): RegraAvaliacaoDomain? {
        val novas = regras ?: return null
        val base = edicao.regraPropria
            ?: edicao.regraDoAno.copy(id = 0L, disciplinaId = edicao.disciplina.id)
        return base.copy(
            mediaMinima = novas.mediaMinima,
            frequenciaMinima = novas.frequenciaMinima,
            tipoMedia = novas.tipoMedia,
            arredondamento = novas.arredondamento,
        )
    }

    /** A regra que vale com o que está na tela: a própria, se houver, ou a do ano. */
    fun regraEfetiva(edicao: EdicaoDisciplina): RegraAvaliacaoDomain =
        regraParaGravar(edicao) ?: edicao.regraDoAno

    companion object {
        /** O formulário com a disciplina e a regra própria dela como estão gravadas. */
        fun de(disciplina: DisciplinaDomain, regraPropria: RegraAvaliacaoDomain? = null): FormularioDisciplina {
            val inicio = disciplina.periodoInicio
            val fim = disciplina.periodoFim
            return FormularioDisciplina(
                nome = disciplina.nome,
                professor = disciplina.professor.orEmpty(),
                cor = disciplina.cor,
                icone = disciplina.icone,
                totalAulas = disciplina.totalAulas,
                // Uma data sozinha não forma um período: vale o ano letivo inteiro
                periodo = if (inicio != null && fim != null) DatasPeriodo(inicio, fim) else null,
                regras = regraPropria?.let(::regrasDe),
            )
        }
    }
}

/** Quantas alterações cada seção tem. A barra de baixo mostra o [total]; cada seção, um ponto. */
data class AlteracoesDaDisciplina(
    val disciplina: Int, // o nome e o professor
    val cor: Int,
    val icone: Int,
    val aulas: Int, // o total de aulas e o período
    val regras: Int = 0, // a média e a frequência mínimas, o cálculo e o arredondamento
) {
    val total: Int
        get() = disciplina + cor + icone + aulas + regras
}

/** As quatro partes de uma regra que as telas editam, do jeito dos ajustes do ano. */
internal fun regrasDe(regra: RegraAvaliacaoDomain) = FormularioAnoLetivo.Regras(
    mediaMinima = regra.mediaMinima,
    frequenciaMinima = regra.frequenciaMinima,
    tipoMedia = regra.tipoMedia,
    arredondamento = regra.arredondamento,
)

/**
 * O que o sheet das regras aplica. Iguais às do ano ([doAno]), não há por que ter regras próprias, e
 * fica null: a disciplina volta a seguir as do ano, inclusive quando elas mudarem depois.
 */
internal fun regrasEscolhidas(
    escolhidas: FormularioAnoLetivo.Regras,
    doAno: FormularioAnoLetivo.Regras,
): FormularioAnoLetivo.Regras? = escolhidas.takeIf { it != doAno }

/**
 * O período escolhido no calendário. Do primeiro ao último dia do ano letivo ([doAno]) é o ano
 * inteiro, e fica null: assim ele acompanha o ano se as datas dele mudarem depois, e abrir o
 * calendário e aplicar sem mexer não conta como alteração.
 */
internal fun periodoEscolhido(inicio: LocalDate, fim: LocalDate, doAno: DatasPeriodo?): DatasPeriodo? =
    DatasPeriodo(inicio, fim).takeIf { it != doAno }

/** O total de aulas como está digitado no sheet: vazio, um número que serve, ou um que não serve. */
internal sealed interface TotalDigitado {
    data object Vazio : TotalDigitado // não informado
    data class Valido(val aulas: Int) : TotalDigitado
    data object Invalido : TotalDigitado // zero (o campo só aceita algarismos)
}

internal fun lerTotalDeAulas(texto: String): TotalDigitado {
    if (texto.isBlank()) return TotalDigitado.Vazio
    val aulas = texto.trim().toIntOrNull()?.takeIf { it > 0 } ?: return TotalDigitado.Invalido
    return TotalDigitado.Valido(aulas)
}

/** O máximo de algarismos do total: 999 aulas já sobram para um ano. */
internal const val DIGITOS_DO_TOTAL = 3

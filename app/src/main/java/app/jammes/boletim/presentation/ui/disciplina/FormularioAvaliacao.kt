package app.jammes.boletim.presentation.ui.disciplina

import app.jammes.boletim.domain.model.AvaliacaoDomain
import app.jammes.boletim.domain.model.PeriodoDomain
import app.jammes.boletim.domain.model.RegraAvaliacaoDomain
import app.jammes.boletim.domain.model.StatusDisciplina
import app.jammes.boletim.domain.model.TipoArredondamento
import app.jammes.boletim.domain.model.TipoAvaliacao
import app.jammes.boletim.domain.model.TipoMedia
import app.jammes.boletim.domain.model.TipoPeriodo
import app.jammes.boletim.domain.model.periodoDe
import app.jammes.boletim.domain.usecase.arredondarMedia
import app.jammes.boletim.domain.usecase.notaNaEscalaDe10
import app.jammes.boletim.domain.usecase.statusDaMedia
import app.jammes.boletim.presentation.ui.anoletivo.dataCurta
import app.jammes.boletim.presentation.ui.anoletivo.feminino
import app.jammes.boletim.presentation.ui.anoletivo.nomeDoPeriodo
import app.jammes.boletim.presentation.ui.anoletivo.passo
import java.io.Serializable
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Os campos do formulário de avaliação, do jeito que estão na tela: os números ficam como texto,
 * para aceitar o que está no meio da digitação ("8," ou vazio). É Serializable para o
 * rememberSaveable guardar: o que já foi digitado sobrevive a girar a tela.
 *
 * Sem nota, a avaliação fica agendada: ainda não aconteceu, ou a nota não saiu, e fica fora da
 * média. Uma avaliação depois de hoje só pode ficar agendada. A data é obrigatória; só fica vazia
 * numa avaliação gravada antes disso, e aí precisa ser escolhida para gravar de novo.
 */
data class FormularioAvaliacao(
    val nome: String,
    val nota: String, // vazia: agendada
    val notaMaxima: String,
    val peso: String,
    val tipo: TipoAvaliacao,
    val periodoId: Long?,
    val data: LocalDate?,
) : Serializable {

    /** Sem nota: a avaliação fica agendada e não entra na média até a nota ser lançada. */
    val agendada: Boolean
        get() = nota.isBlank()

    /** A nota máxima, ou null enquanto ela não for um número maior que zero. */
    val notaMaximaValor: Double?
        get() = lerNumero(notaMaxima)?.takeIf { it > 0 }

    /** O peso, ou null enquanto ele não for um número maior que zero. */
    val pesoValor: Double?
        get() = lerNumero(peso)?.takeIf { it > 0 }

    /**
     * A nota, ou null quando ela está vazia ou não é um número de 0 até a máxima. Com a máxima
     * inválida, vale qualquer número a partir de 0: o erro aparece na máxima, não aqui.
     */
    val notaValor: Double?
        get() {
            val maxima = notaMaximaValor
            return lerNumero(nota)?.takeIf { it >= 0 && (maxima == null || it <= maxima) }
        }

    /** Tem alguma coisa na nota, mas não é um número de 0 até a máxima. */
    val notaInvalida: Boolean
        get() = !agendada && notaValor == null

    /** O dia da avaliação ainda não chegou. Sem data, nunca. */
    fun noFuturo(hoje: LocalDate): Boolean = data != null && data > hoje

    /**
     * A nota não abre para digitar: a avaliação é depois de [hoje] e ainda está sem nota. Uma que
     * já tem nota (gravada antes desta regra) continua aberta, para a nota poder ser apagada.
     */
    fun notaBloqueada(hoje: LocalDate): Boolean = agendada && noFuturo(hoje)

    /**
     * O que impede de gravar, ou null quando dá para gravar. Vem o primeiro na ordem da tela:
     * nome, nota, data e, por último, o que fica em "Mais opções". O peso só conta [comPeso].
     */
    fun problema(comPeso: Boolean, hoje: LocalDate): Problema? = when {
        nome.isBlank() -> Problema.SEM_NOME
        notaInvalida -> Problema.NOTA_FORA_DA_ESCALA
        !agendada && noFuturo(hoje) -> Problema.NOTA_NO_FUTURO
        data == null -> Problema.SEM_DATA
        notaMaximaValor == null -> Problema.NOTA_MAXIMA
        comPeso && pesoValor == null -> Problema.PESO
        else -> null
    }

    /** A avaliação pronta para gravar, a partir da [original], ou null enquanto houver um [problema]. */
    fun paraGravar(original: AvaliacaoDomain, comPeso: Boolean, hoje: LocalDate): AvaliacaoDomain? {
        if (problema(comPeso, hoje) != null) return null
        return original.copy(
            nome = nome.trim(),
            nota = notaValor, // vazia fica null
            notaMaxima = notaMaximaValor ?: return null,
            // Escondido, o peso fica como estava: fora da média ponderada ele não muda a conta
            peso = if (comPeso) pesoValor ?: return null else original.peso,
            tipo = tipo,
            periodoId = periodoId,
            data = data,
        )
    }

    /** O que o botão faz com a avaliação, que é o que ele diz. */
    fun acao(original: AvaliacaoDomain): Acao = when {
        original.id == 0L -> if (agendada) Acao.AGENDAR else Acao.LANCAR
        original.nota == null && !agendada -> Acao.LANCAR // a agendada ganhando a nota
        else -> Acao.SALVAR
    }

    /** A nota como o app mostra os números ("9.5" vira "9,5"); inválida ou vazia, como está. */
    fun notaArrumada(): FormularioAvaliacao =
        notaValor?.let { copy(nota = formatarNumero(it)) } ?: this

    /**
     * Outra data. Com [periodos] (no ano letivo inteiro, quando o formulário pergunta o período),
     * o período passa a ser o da data, como nas faltas; vazia, o período fica como está.
     */
    fun comData(nova: LocalDate, periodos: List<PeriodoDomain>): FormularioAvaliacao =
        copy(data = nova, periodoId = periodos.periodoDe(nova)?.id ?: periodoId)

    /** Outro período. A data, se ficou fora dele, vai para o dia mais perto dentro dele. */
    fun comPeriodo(periodo: PeriodoDomain): FormularioAvaliacao =
        copy(periodoId = periodo.id, data = data?.coerceIn(periodo.dataInicio..periodo.dataFim))

    /**
     * O formulário da próxima avaliação, depois de gravar esta com "Outra": o que costuma se
     * repetir numa sequência (data, período, tipo, valor e peso) fica; o nome e a nota esvaziam.
     */
    fun paraAProxima(): FormularioAvaliacao = copy(nome = "", nota = "")

    /** O peso um passo acima ([sentido] 1) ou abaixo (-1), pelo − e +; sem peso válido, parte do 1. */
    fun comPassoNoPeso(sentido: Int): FormularioAvaliacao =
        copy(peso = formatarNumero(passo(pesoValor ?: 1.0, PASSO_DO_PESO, sentido, FAIXA_DO_PESO)))

    /** A nota máxima é uma das que aparecem como opção pronta (10, 5 e 100). */
    val notaMaximaComum: Boolean
        get() = notaMaximaValor in NOTAS_MAXIMAS_COMUNS

    /**
     * Os dias que o calendário deixa escolher: os de [intervalo] (o período, ou os da tela) e,
     * com nota, só até [hoje], porque uma avaliação no futuro só pode ficar agendada.
     */
    fun diasDoCalendario(intervalo: ClosedRange<LocalDate>?, hoje: LocalDate) = DiasDoCalendario(
        inicio = intervalo?.start,
        fim = listOfNotNull(intervalo?.endInclusive, hoje.takeUnless { agendada }).minOrNull(),
    )

    /** O resumo da linha "Mais opções": "Normal · vale 10", com " · peso 2" quando o peso conta. */
    fun resumoDasOpcoes(comPeso: Boolean): String = listOfNotNull(
        tipo.displayname,
        "vale ${notaMaximaValor?.let(::formatarNumero) ?: notaMaxima.trim().ifEmpty { "?" }}",
        if (comPeso) "peso ${pesoValor?.let(::formatarNumero) ?: peso.trim().ifEmpty { "?" }}" else null,
    ).joinToString(" · ")

    /** O que está errado em "Mais opções", para a linha avisar com a seção fechada; null quando nada. */
    fun avisoDasOpcoes(comPeso: Boolean): String? = when {
        notaMaximaValor == null -> "Confira a nota máxima"
        comPeso && pesoValor == null -> "Confira o peso"
        else -> null
    }

    enum class Problema {
        SEM_NOME,
        NOTA_FORA_DA_ESCALA,
        NOTA_NO_FUTURO,
        SEM_DATA,
        NOTA_MAXIMA,
        PESO,
    }

    enum class Acao(val texto: String) {
        AGENDAR("Agendar avaliação"),
        LANCAR("Lançar nota"),
        SALVAR("Salvar alterações"),
    }

    companion object {
        /** O formulário com os valores da [avaliacao], como ela está gravada (ou nova). */
        fun de(avaliacao: AvaliacaoDomain) = FormularioAvaliacao(
            nome = avaliacao.nome,
            nota = avaliacao.nota?.let(::formatarNumero).orEmpty(),
            notaMaxima = formatarNumero(avaliacao.notaMaxima),
            peso = formatarNumero(avaliacao.peso),
            tipo = avaliacao.tipo,
            periodoId = avaliacao.periodoId,
            data = avaliacao.data,
        )

        /** Os caracteres que uma nota pode ter: até 6, só algarismos e uma vírgula (ou ponto). */
        fun aceitaComoNota(texto: String): Boolean =
            texto.length <= 6 &&
                texto.all { it.isDigit() || it == ',' || it == '.' } &&
                texto.count { it == ',' || it == '.' } <= 1
    }
}

/** As notas máximas que aparecem como opção pronta; outra qualquer é digitada em "Outro". */
internal val NOTAS_MAXIMAS_COMUNS = listOf(10.0, 5.0, 100.0)

// O peso anda de meio em meio: dá 1,5 sem precisar de teclado, e 3 fica a quatro toques de 1
internal const val PASSO_DO_PESO = 0.5
internal val FAIXA_DO_PESO = 0.5..10.0

/**
 * Os dias do [inicio] ao [fim], os dois inclusive; um lado null não tem limite. Pode não caber
 * nenhum (início depois do fim): um período que ainda não começou, com a nota já digitada.
 */
data class DiasDoCalendario(val inicio: LocalDate?, val fim: LocalDate?) {

    val vazio: Boolean
        get() = inicio != null && fim != null && inicio > fim

    operator fun contains(dia: LocalDate): Boolean =
        (inicio == null || dia >= inicio) && (fim == null || dia <= fim)

    /** O dia mais perto de [dia] entre os que cabem; sem nenhum, o próprio [dia]. */
    fun maisPerto(dia: LocalDate): LocalDate = when {
        vazio -> dia
        inicio != null && dia < inicio -> inicio
        fim != null && dia > fim -> fim
        else -> dia
    }
}

/** A frase embaixo da nota e se ela é um erro. */
internal data class AvisoDaNota(val texto: String, val erro: Boolean)

/**
 * O que a nota faz na conta, ou o que está errado com ela. Na [soma] a nota entra na soma, não na
 * média. [doPeriodo] é o período com o artigo ("do 4º Bimestre"), ou null sem período.
 */
internal fun avisoDaNota(
    formulario: FormularioAvaliacao,
    hoje: LocalDate,
    soma: Boolean,
    doPeriodo: String?,
): AvisoDaNota {
    val conta = if (soma) "soma" else "média"
    return when {
        formulario.notaInvalida -> AvisoDaNota(
            texto = formulario.notaMaximaValor
                ?.let { "Use um número de 0 a ${formatarNumero(it)}." }
                ?: "Use um número.",
            erro = true,
        )
        formulario.noFuturo(hoje) && !formulario.agendada -> AvisoDaNota(
            texto = "Essa data ainda não chegou: apague a nota ou mude a data.",
            erro = true,
        )
        formulario.notaBloqueada(hoje) -> AvisoDaNota(
            texto = "Dá para lançar a nota a partir de ${dataCurta(formulario.data!!, hoje.year)}.",
            erro = false,
        )
        formulario.agendada -> AvisoDaNota("Fica fora da $conta até você lançar a nota.", erro = false)
        doPeriodo != null -> AvisoDaNota("Entra na $conta $doPeriodo.", erro = false)
        else -> AvisoDaNota("Entra na $conta.", erro = false)
    }
}

/** "do 4º Bimestre", "da 1ª Unidade". */
internal fun doPeriodo(periodo: PeriodoDomain, tipo: TipoPeriodo): String =
    "${if (tipo.feminino) "da" else "do"} ${nomeDoPeriodo(periodo.periodo, tipo)}"

/**
 * A situação de uma nota como se fosse uma média: levada para a escala de 10 pela mesma conta do
 * cálculo ([notaNaEscalaDe10]), então a cor da nota e a média nunca discordam, e limpa como a tela
 * limpa as médias. Na soma, cada nota é só uma parte do total e não dá para comparar com a mínima: null.
 */
internal fun statusDaNota(nota: Double, notaMaxima: Double, regra: RegraAvaliacaoDomain): StatusDisciplina? =
    if (regra.tipoMedia == TipoMedia.SOMA) {
        null
    } else {
        notaNaEscalaDe10(nota, notaMaxima)
            ?.let { statusDaMedia(arredondarMedia(it, TipoArredondamento.NENHUM), regra) }
    }

/** O que a pílula da nota diz: onde ela fica perto da mínima, como no Boletim. */
internal fun textoDoStatusDaNota(status: StatusDisciplina): String = when (status) {
    StatusDisciplina.APROVADO -> "Com folga"
    StatusDisciplina.ATENCAO -> "Perto da mínima"
    StatusDisciplina.ABAIXO, StatusDisciplina.REPROVADO -> "Abaixo da mínima"
    StatusDisciplina.SEM_NOTA -> "Sem nota"
}

/** "Hoje · seg, 12 out", "seg, 26 out · daqui a 2 semanas", "qui, 10 set" ou "Escolha a data". */
internal fun resumoDaData(data: LocalDate?, hoje: LocalDate): String = when {
    data == null -> "Escolha a data"
    data == hoje -> "Hoje · ${diaCurto(data, hoje)}"
    data == hoje.plusDays(1) -> "Amanhã · ${diaCurto(data, hoje)}"
    data == hoje.minusDays(1) -> "Ontem · ${diaCurto(data, hoje)}"
    data > hoje -> "${diaCurto(data, hoje)} · ${daquiA(data, hoje)}"
    else -> diaCurto(data, hoje)
}

/** "seg, 26 out". O ano só aparece quando não é o de [hoje]: "seg, 1 fev 2027". */
internal fun diaCurto(data: LocalDate, hoje: LocalDate): String =
    "${DIAS_DA_SEMANA[data.dayOfWeek.value - 1]}, ${dataCurta(data, hoje.year)}"

/**
 * Quanto falta até [data], que é depois de amanhã: em dias na primeira semana, em semanas até
 * quase um mês e depois em meses. "daqui a 5 dias", "daqui a 2 semanas", "daqui a 3 meses".
 */
internal fun daquiA(data: LocalDate, hoje: LocalDate): String {
    val dias = ChronoUnit.DAYS.between(hoje, data)
    return when {
        dias < 7 -> "daqui a $dias dias"
        dias < 35 -> (dias / 7).let { if (it == 1L) "daqui a 1 semana" else "daqui a $it semanas" }
        else -> ChronoUnit.MONTHS.between(hoje, data).let { if (it == 1L) "daqui a 1 mês" else "daqui a $it meses" }
    }
}

private val DIAS_DA_SEMANA = listOf("seg", "ter", "qua", "qui", "sex", "sáb", "dom")

// Aceita vírgula ou ponto. Vazio, ou texto que não é número, vira null.
internal fun lerNumero(texto: String): Double? =
    texto.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }

// Peso, nota máxima e os campos do formulário: até 2 casas, sem zero sobrando (1 · 1,5 · 10)
internal fun formatarNumero(valor: Double): String =
    NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR"))
        .apply {
            maximumFractionDigits = 2
            roundingMode = RoundingMode.HALF_UP
        }
        .format(valor)

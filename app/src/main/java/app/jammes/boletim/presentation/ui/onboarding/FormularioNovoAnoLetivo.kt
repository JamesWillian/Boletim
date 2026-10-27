package app.jammes.boletim.presentation.ui.onboarding

import app.jammes.boletim.domain.model.Etapa
import app.jammes.boletim.domain.model.PeriodoDomain
import app.jammes.boletim.domain.model.TipoPeriodo
import app.jammes.boletim.domain.usecase.periodosPadrao
import app.jammes.boletim.presentation.ui.anoletivo.DatasPeriodo
import app.jammes.boletim.presentation.ui.anoletivo.FormularioAnoLetivo
import app.jammes.boletim.presentation.ui.anoletivo.periodoSeguinte
import app.jammes.boletim.presentation.ui.anoletivo.problemasDasDatas
import java.io.Serializable

/** A série escolhida nos chips. Cada etapa conta do seu jeito: "9º Ano" no Fundamental, "1ª Série" no Médio. */
sealed interface Serie : Serializable {
    data class Fundamental(val numero: Int) : Serie // 1 a 9
    data class Medio(val numero: Int) : Serie // 1 a 3
    data class Outra(val texto: String = "") : Serie
}

/** O texto que vai para o ano letivo; null quando ele fica sem série. */
internal fun Serie?.texto(): String? = when (this) {
    is Serie.Fundamental -> "${numero}º Ano"
    is Serie.Medio -> "${numero}ª Série"
    is Serie.Outra -> texto.trim().ifEmpty { null }
    null -> null
}

/** A etapa que os chips dizem; uma série escrita à mão não diz nenhuma. */
internal val Serie?.etapa: Etapa?
    get() = when (this) {
        is Serie.Fundamental -> Etapa.FUNDAMENTAL
        is Serie.Medio -> Etapa.MEDIO
        is Serie.Outra, null -> null
    }

/** As datas sugeridas para o [tipo] no [ano], do jeito que a tela edita. */
internal fun datasPadrao(ano: Int, tipo: TipoPeriodo): List<DatasPeriodo> =
    periodosPadrao(ano, tipo).map { DatasPeriodo(it.dataInicio, it.dataFim) }

/**
 * Os campos de um ano letivo novo: a série, o ano e como ele se divide. O onboarding começa por eles,
 * e o "Criar novo ano letivo" do perfil é só eles, nas mesmas peças de tela. É Serializable para o
 * rememberSaveable.
 */
data class FormularioNovoAnoLetivo(
    val serie: Serie? = null,
    val ano: Int,
    val tipoPeriodo: TipoPeriodo = TipoPeriodo.BIMESTRE,
    val datas: List<DatasPeriodo> = datasPadrao(ano, tipoPeriodo),
) : Serializable {

    val datasCertas: Boolean get() = problemasDasDatas(datas).all { it == null }

    // Trocar o ano ou o tipo refaz as datas: as de antes eram de outro calendário
    fun comAno(ano: Int) = copy(ano = ano, datas = datasPadrao(ano, tipoPeriodo))
    fun comTipo(tipo: TipoPeriodo) = copy(tipoPeriodo = tipo, datas = datasPadrao(ano, tipo))
    fun comDatas(indice: Int, novas: DatasPeriodo) = copy(datas = datas.toMutableList().also { it[indice] = novas })

    /** O − tira o último período; o + põe outro depois do último, com a mesma duração, como nos ajustes. */
    fun comQuantidade(quantidade: Int): FormularioNovoAnoLetivo {
        val alvo = quantidade.coerceIn(1, FormularioAnoLetivo.MAX_PERIODOS)
        val novas = datas.take(alvo).toMutableList()
        while (novas.size < alvo) novas += periodoSeguinte(novas.last())
        return copy(datas = novas)
    }

    /** Os períodos como vão para o banco: numerados na ordem e ainda sem ano letivo (id 0). */
    fun periodos(): List<PeriodoDomain> = datas.mapIndexed { i, d ->
        PeriodoDomain(anoLetivoId = 0, periodo = i + 1, dataInicio = d.inicio, dataFim = d.fim)
    }
}

/**
 * O ano que o formulário já traz: o do calendário ([atual]), ou o seguinte ao mais recente dos
 * [existentes] quando esse já foi criado (um ano que o aluno já tem não pode ser criado de novo).
 */
internal fun anoSugerido(existentes: Set<Int>, atual: Int): Int =
    maxOf(atual, (existentes.maxOrNull() ?: 0) + 1)

/**
 * Os anos para escolher: o passado, o atual e o próximo, que cobrem quem cadastra atrasado e quem já
 * prepara o seguinte. Quando o aluno já tem o próximo, o [anoSugerido] passa deles e entra no fim.
 */
internal fun anosParaEscolher(existentes: Set<Int>, atual: Int): List<Int> =
    (listOf(atual - 1, atual, atual + 1) + anoSugerido(existentes, atual)).distinct()

/** "2026 já existe", "2025 e 2026 já existem": por que esses anos não dão para escolher. null sem nenhum. */
internal fun anosQueJaExistem(anos: List<Int>): String? = when (anos.size) {
    0 -> null
    1 -> "${anos.single()} já existe"
    else -> anos.dropLast(1).joinToString(", ") + " e ${anos.last()} já existem"
}

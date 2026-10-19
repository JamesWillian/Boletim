package app.jammes.boletim.presentation.ui.onboarding

import app.jammes.boletim.domain.model.Etapa
import app.jammes.boletim.domain.model.PeriodoDomain
import app.jammes.boletim.domain.model.TipoArredondamento
import app.jammes.boletim.domain.model.TipoMedia
import app.jammes.boletim.domain.model.TipoPeriodo
import app.jammes.boletim.domain.usecase.PrimeiroAno
import app.jammes.boletim.domain.usecase.REGRA_PADRAO
import app.jammes.boletim.domain.usecase.periodosPadrao
import app.jammes.boletim.presentation.ui.anoletivo.DatasPeriodo
import app.jammes.boletim.presentation.ui.anoletivo.FAIXA_DA_FREQUENCIA
import app.jammes.boletim.presentation.ui.anoletivo.FAIXA_DA_MEDIA
import app.jammes.boletim.presentation.ui.anoletivo.FormularioAnoLetivo
import app.jammes.boletim.presentation.ui.anoletivo.PASSO_DA_FREQUENCIA
import app.jammes.boletim.presentation.ui.anoletivo.PASSO_DA_MEDIA
import app.jammes.boletim.presentation.ui.anoletivo.passo
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

/** Os campos do onboarding. É Serializable para o rememberSaveable guardar tudo se o app morrer no meio. */
data class FormularioOnboarding(
    val nome: String = "",
    val serie: Serie? = null,
    val ano: Int,
    val tipoPeriodo: TipoPeriodo = TipoPeriodo.BIMESTRE,
    val datas: List<DatasPeriodo> = datasPadrao(ano, tipoPeriodo),
    val mediaMinima: Double = REGRA_PADRAO.mediaMinima,
    val frequenciaMinima: Double = REGRA_PADRAO.frequenciaMinima,
    val tipoMedia: TipoMedia = REGRA_PADRAO.tipoMedia,
    val arredondamento: TipoArredondamento = REGRA_PADRAO.arredondamento,
    val materiaIds: Set<Long>? = null, // null: as que a série sugere, até a pessoa mexer
    val materiasNovas: List<String> = emptyList(), // criadas aqui: entram no cadastro só no fim
) : Serializable {
    /** O nome é o único campo obrigatório do primeiro passo. */
    val podeConcluir: Boolean get() = nome.isNotBlank()

    val datasCertas: Boolean get() = problemasDasDatas(datas).all { it == null }

    // Trocar o ano ou o tipo refaz as datas: as de antes eram de outro calendário
    fun comAno(ano: Int) = copy(ano = ano, datas = datasPadrao(ano, tipoPeriodo))
    fun comTipo(tipo: TipoPeriodo) = copy(tipoPeriodo = tipo, datas = datasPadrao(ano, tipo))
    fun comDatas(indice: Int, novas: DatasPeriodo) = copy(datas = datas.toMutableList().also { it[indice] = novas })

    /** O − tira o último período; o + põe outro depois do último, com a mesma duração, como nos ajustes. */
    fun comQuantidade(quantidade: Int): FormularioOnboarding {
        val alvo = quantidade.coerceIn(1, FormularioAnoLetivo.MAX_PERIODOS)
        val novas = datas.take(alvo).toMutableList()
        while (novas.size < alvo) novas += periodoSeguinte(novas.last())
        return copy(datas = novas)
    }

    fun mudarMedia(sentido: Int) = copy(mediaMinima = passo(mediaMinima, PASSO_DA_MEDIA, sentido, FAIXA_DA_MEDIA))
    fun mudarFrequencia(sentido: Int) =
        copy(frequenciaMinima = passo(frequenciaMinima, PASSO_DA_FREQUENCIA, sentido, FAIXA_DA_FREQUENCIA))

    /** As marcadas: as que a pessoa escolheu ou, enquanto ela não mexe, as [sugeridas] pela série. */
    fun materiasMarcadas(sugeridas: Set<Long>): Set<Long> = materiaIds ?: sugeridas

    fun alternarMateria(id: Long, sugeridas: Set<Long>): FormularioOnboarding {
        val marcadas = materiasMarcadas(sugeridas)
        return copy(materiaIds = if (id in marcadas) marcadas - id else marcadas + id)
    }

    fun comMateriaNova(nome: String) = copy(materiasNovas = materiasNovas + nome.trim())
    fun semMateriaNova(nome: String) = copy(materiasNovas = materiasNovas - nome)

    /** Quantas vão para o ano: as marcadas do cadastro mais as criadas aqui. */
    fun totalDeMaterias(sugeridas: Set<Long>): Int = materiasMarcadas(sugeridas).size + materiasNovas.size

    /** O que vai para o banco; null enquanto falta o nome, alguma data está errada ou não há matéria. */
    fun paraGravar(sugeridas: Set<Long>): PrimeiroAno? {
        val materias = materiasMarcadas(sugeridas)
        if (!podeConcluir || !datasCertas || totalDeMaterias(sugeridas) == 0) return null
        return PrimeiroAno(
            nome = nome.trim(),
            ano = ano,
            serie = serie.texto(),
            tipoPeriodo = tipoPeriodo,
            periodos = datas.mapIndexed { i, d ->
                PeriodoDomain(anoLetivoId = 0, periodo = i + 1, dataInicio = d.inicio, dataFim = d.fim)
            },
            regra = REGRA_PADRAO.copy(
                anoLetivoId = 0,
                mediaMinima = mediaMinima,
                frequenciaMinima = frequenciaMinima,
                tipoMedia = tipoMedia,
                arredondamento = arredondamento,
            ),
            materiaIds = materias,
            materiasNovas = materiasNovas,
        )
    }
}

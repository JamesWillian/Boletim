package app.jammes.boletim.presentation.ui.onboarding

import app.jammes.boletim.domain.model.TipoArredondamento
import app.jammes.boletim.domain.model.TipoMedia
import app.jammes.boletim.domain.usecase.PrimeiroAno
import app.jammes.boletim.domain.usecase.REGRA_PADRAO
import app.jammes.boletim.presentation.ui.anoletivo.FAIXA_DA_FREQUENCIA
import app.jammes.boletim.presentation.ui.anoletivo.FAIXA_DA_MEDIA
import app.jammes.boletim.presentation.ui.anoletivo.PASSO_DA_FREQUENCIA
import app.jammes.boletim.presentation.ui.anoletivo.PASSO_DA_MEDIA
import app.jammes.boletim.presentation.ui.anoletivo.passo
import java.io.Serializable

/** Os campos do onboarding. É Serializable para o rememberSaveable guardar tudo se o app morrer no meio. */
data class FormularioOnboarding(
    val nome: String = "",
    val anoLetivo: FormularioNovoAnoLetivo, // a série, o ano e a divisão, como no ano novo do perfil
    val mediaMinima: Double = REGRA_PADRAO.mediaMinima,
    val frequenciaMinima: Double = REGRA_PADRAO.frequenciaMinima,
    val tipoMedia: TipoMedia = REGRA_PADRAO.tipoMedia,
    val arredondamento: TipoArredondamento = REGRA_PADRAO.arredondamento,
    val materiaIds: Set<Long>? = null, // null: as que a série sugere, até a pessoa mexer
    val materiasNovas: List<String> = emptyList(), // criadas aqui: entram no cadastro só no fim
) : Serializable {
    /** O nome é o único campo obrigatório do primeiro passo. */
    val podeConcluir: Boolean get() = nome.isNotBlank()

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
        if (!podeConcluir || !anoLetivo.datasCertas || totalDeMaterias(sugeridas) == 0) return null
        return PrimeiroAno(
            nome = nome.trim(),
            ano = anoLetivo.ano,
            serie = anoLetivo.serie.texto(),
            tipoPeriodo = anoLetivo.tipoPeriodo,
            periodos = anoLetivo.periodos(),
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

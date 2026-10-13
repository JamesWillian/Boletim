package app.jammes.boletim.domain.model

/**
 * A média de uma disciplina num filtro (um período, ou o ano letivo inteiro) e de onde ela veio.
 * A recuperação substitui a média do período, e a final a do ano, mas só quando são maiores.
 */
data class MediaNoFiltro(
    /** A que vale: a [regular], ou a nota da [substituta] quando ela substituiu. null sem nenhuma nota. */
    val media: Double?,
    /** A das avaliações normais. No ano, a média dos períodos, já com a recuperação de cada um. */
    val regular: Double?,
    /** A recuperação do período, ou a final do ano, quando já tem nota. */
    val substituta: Substituta?,
)

/**
 * A nota da recuperação ou da final, levada para a escala de 10 e arredondada como uma média.
 * [substituiu] diz se ela tomou o lugar da média regular: por ser maior, ou por não haver uma.
 */
data class Substituta(
    val tipo: TipoAvaliacao,
    val nota: Double,
    val substituiu: Boolean,
)

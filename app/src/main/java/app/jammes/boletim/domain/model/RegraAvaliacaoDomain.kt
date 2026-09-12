package app.jammes.boletim.domain.model

data class RegraAvaliacaoDomain(
    val id: Long = 0L,
    val anoLetivoId: Long,
    val disciplinaId: Long?,
    val mediaMinima: Double,
    val mediaRecuperacao: Double?,
    val frequenciaMinima: Double,
    val tipoMedia: TipoMedia = TipoMedia.PONDERADA,
    val arredondamento: TipoArredondamento = TipoArredondamento.NENHUM,
    val descartarMenorNota: Boolean = false
)

package app.jammes.boletim.domain.model

data class MateriaDomain(
    val id: Long = 0L,
    val nome: String,
    val abreviacao: String?,
    val cor: Int = 0
)

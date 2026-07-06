package app.jammes.boletim.domain.model

data class AlunoDomain(
    val id : String = "",
    val nome : String,
    val anoLetivoId: String? = null,
    val periodoId: String? = null
)

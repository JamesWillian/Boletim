package app.jammes.boletim.domain.model

data class AlunoDomain(
    val id : String = "",
    val nome : String,
    val anoLetivoId: String? = null,
    val anoLetivo: String? = null,
    val periodoId: String? = null,
    val periodo: String? = null,
    val periodoType: PeriodoType = PeriodoType.UNIDADE
)

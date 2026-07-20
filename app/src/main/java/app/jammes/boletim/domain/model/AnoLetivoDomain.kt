package app.jammes.boletim.domain.model

data class AnoLetivoDomain(
    val id : String = "",
    val descricao : String,
    val periodo: List<PeriodoDomain>,
    val periodoType: PeriodoType = PeriodoType.UNIDADE
)

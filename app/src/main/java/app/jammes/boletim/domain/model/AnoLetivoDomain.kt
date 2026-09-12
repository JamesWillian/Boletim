package app.jammes.boletim.domain.model

data class AnoLetivoDomain(
    val id : Long = 0L,
    val ano : Int,
    val serie: String?,
    val periodo: List<PeriodoDomain>,
    val tipoPeriodo: TipoPeriodo = TipoPeriodo.UNIDADE,
    val qtdPeriodos: Int,
    val ativo: Int = 0,
    val alunoId: Long
)

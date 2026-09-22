package app.jammes.boletim.domain.model

data class Boletim(
    val disciplinas: List<DisciplinaResumo>,
    val mediaGeral: Double?,
    val totalFaltas: Int,
)
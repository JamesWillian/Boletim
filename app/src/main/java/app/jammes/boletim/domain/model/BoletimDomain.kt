package app.jammes.boletim.domain.model

data class BoletimDomain(
    val id: String,
    val disciplina: DisciplinaDomain,
    val periodoId: String,
    val nota: Double
)

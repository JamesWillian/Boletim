package app.jammes.boletim.domain.model

data class DisciplinaResumo(
    val id: Long,
    val nome: String,
    val cor: Int,
    val media: Double?,
    val status: StatusDisciplina,
    val faltas: Int,
    val limiteFaltas: Int?
) {
    val emRiscoPorFalta: Boolean
        get() = limiteFaltas != null && faltas >= limiteFaltas
}
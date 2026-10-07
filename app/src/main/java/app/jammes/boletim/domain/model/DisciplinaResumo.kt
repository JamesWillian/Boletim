package app.jammes.boletim.domain.model

data class DisciplinaResumo(
    val id: Long,
    val nome: String,
    val cor: Int,
    val icone: IconeMateria,
    val media: Double?,
    val mediaMinima: Double, // da regra da disciplina: a marca na barra do card
    val status: StatusDisciplina,
    val faltas: Int,
    val limiteFaltas: Int?
) {
    val emRiscoPorFalta: Boolean
        get() = limiteFaltas != null && faltas >= limiteFaltas
}
package app.jammes.boletim.domain.model

data class DisciplinaResumo(
    val id: Long,
    val nome: String,
    val cor: Int,
    val icone: IconeMateria,
    val media: Double?, // a que vale: com a recuperação (no período) ou a final (no ano), se forem maiores
    val mediaRegular: Double?, // só das avaliações normais: o detalhe embaixo, quando há substituta
    val substituta: Substituta?,
    val mediaMinima: Double, // da regra da disciplina: a marca na barra do card
    val status: StatusDisciplina,
    val faltas: Int,
    val limiteFaltas: Int?,
    val situacao: StatusDisciplina?, // aprovado ou reprovado, depois de finalizada; null enquanto em curso
) {
    val emRiscoPorFalta: Boolean
        get() = limiteFaltas != null && faltas >= limiteFaltas
}
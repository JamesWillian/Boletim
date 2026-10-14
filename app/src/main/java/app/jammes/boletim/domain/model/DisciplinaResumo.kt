package app.jammes.boletim.domain.model

data class DisciplinaResumo(
    val id: Long,
    val nome: String,
    val abreviacao: String?, // a sigla da linha dela no gráfico do Boletim
    val cor: Int,
    val icone: IconeMateria,
    val media: Double?, // a que vale: com a recuperação (no período) ou a final (no ano), se forem maiores
    val mediaMinima: Double, // da regra da disciplina: a marca no arco do card e o tracejado no gráfico
    val status: StatusDisciplina,
    val faltas: Int,
    val limiteFaltas: Int?,
    val situacao: StatusDisciplina?, // aprovado ou reprovado, depois de finalizada; null enquanto em curso
) {
    val emRiscoPorFalta: Boolean
        get() = limiteFaltas != null && faltas >= limiteFaltas
}
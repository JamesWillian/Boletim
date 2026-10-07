package app.jammes.boletim.domain.model

data class Boletim(
    val periodoId: Long?, // null: as médias são do ano letivo inteiro
    val disciplinas: List<DisciplinaResumo>,
    val mediaGeral: Double?,
    val mediaMinima: Double, // da regra padrão do ano: cada disciplina pode ter a sua
    val status: StatusDisciplina, // da média geral contra a mediaMinima
    val totalFaltas: Int,
)

package app.jammes.boletim.domain.model

data class Boletim(
    val periodoId: Long?, // null: as médias são do ano letivo inteiro
    val disciplinas: List<DisciplinaResumo>,
    val mediaMinima: Double, // da regra padrão do ano: cada disciplina pode ter a sua
)

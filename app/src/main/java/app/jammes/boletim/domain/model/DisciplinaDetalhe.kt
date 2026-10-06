package app.jammes.boletim.domain.model

/** Uma disciplina vista no período do Contexto: as avaliações dele e a média que elas dão. */
data class DisciplinaDetalhe(
    val disciplina: DisciplinaDomain,
    val periodoId: Long,
    val regra: RegraAvaliacaoDomain,
    val avaliacoes: List<AvaliacaoDomain>, // só as do período
    val media: Double?,
    val status: StatusDisciplina,
)

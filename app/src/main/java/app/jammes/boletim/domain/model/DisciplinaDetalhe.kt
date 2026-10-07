package app.jammes.boletim.domain.model

/**
 * Uma disciplina vista no filtro do Contexto: um período, ou o ano letivo inteiro.
 * As avaliações vêm separadas por período, e a média é a que elas dão nesse filtro.
 */
data class DisciplinaDetalhe(
    val disciplina: DisciplinaDomain,
    val periodoId: Long?, // null: o ano letivo inteiro
    val tipoPeriodo: TipoPeriodo,
    val periodos: List<PeriodoDomain>, // todos os do ano, em ordem
    val regra: RegraAvaliacaoDomain,
    val avaliacoesPorPeriodo: List<AvaliacoesDoPeriodo>, // só os períodos que têm avaliação
    val media: Double?,
    val status: StatusDisciplina,
    val faltas: Int, // ano inteiro, como no card do Boletim
    val limiteFaltas: Int?,
) {
    val avaliacoes: List<AvaliacaoDomain>
        get() = avaliacoesPorPeriodo.flatMap { it.avaliacoes }

    val emRiscoPorFalta: Boolean
        get() = limiteFaltas != null && faltas >= limiteFaltas
}

data class AvaliacoesDoPeriodo(
    val periodo: PeriodoDomain,
    val avaliacoes: List<AvaliacaoDomain>,
)

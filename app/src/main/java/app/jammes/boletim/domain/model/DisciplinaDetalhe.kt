package app.jammes.boletim.domain.model

/**
 * Uma disciplina vista no filtro do Contexto: um período, ou o ano letivo inteiro.
 * As avaliações e as faltas vêm separadas por período, e a média é a que as avaliações dão nesse filtro.
 */
data class DisciplinaDetalhe(
    val disciplina: DisciplinaDomain,
    val periodoId: Long?, // null: o ano letivo inteiro
    val tipoPeriodo: TipoPeriodo,
    val periodos: List<PeriodoDomain>, // todos os do ano, em ordem
    val regra: RegraAvaliacaoDomain,
    val avaliacoesPorPeriodo: List<AvaliacoesDoPeriodo>, // só os períodos que têm avaliação
    val faltasPorPeriodo: List<FaltasDoPeriodo>, // só os períodos que têm falta
    val media: Double?,
    val status: StatusDisciplina,
    val faltas: Int, // ano inteiro, como no card do Boletim
    val limiteFaltas: Int?,
) {
    val avaliacoes: List<AvaliacaoDomain>
        get() = avaliacoesPorPeriodo.flatMap { it.avaliacoes }

    /** As faltas do filtro: cada lançamento é um dia, com quantas faltas teve nele. */
    val diasDeFalta: List<FaltaDomain>
        get() = faltasPorPeriodo.flatMap { it.faltas }

    /** Os períodos que a tela mostra: o escolhido, ou todos no ano letivo inteiro. */
    val periodosNoFiltro: List<PeriodoDomain>
        get() = periodos.noFiltro(periodoId)

    val emRiscoPorFalta: Boolean
        get() = limiteFaltas != null && faltas >= limiteFaltas
}

data class AvaliacoesDoPeriodo(
    val periodo: PeriodoDomain,
    val avaliacoes: List<AvaliacaoDomain>,
)

data class FaltasDoPeriodo(
    val periodo: PeriodoDomain,
    val faltas: List<FaltaDomain>,
)

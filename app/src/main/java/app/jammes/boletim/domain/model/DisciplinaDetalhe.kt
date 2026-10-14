package app.jammes.boletim.domain.model

/**
 * Uma disciplina vista no filtro do Contexto: um período, ou o ano letivo inteiro.
 * As avaliações e as faltas vêm separadas por período, e a média é a que as avaliações dão nesse filtro.
 * A final é do ano, não de um período: fica em [finais], que só aparece no ano letivo inteiro.
 */
data class DisciplinaDetalhe(
    val disciplina: DisciplinaDomain,
    val periodoId: Long?, // null: o ano letivo inteiro
    val tipoPeriodo: TipoPeriodo,
    val periodos: List<PeriodoDomain>, // todos os do ano, em ordem
    val regra: RegraAvaliacaoDomain,
    val avaliacoesPorPeriodo: List<AvaliacoesDoPeriodo>, // só os períodos que têm avaliação
    val finais: List<AvaliacaoDomain>, // a final do ano, no grupo dela; vazia num período
    // Todas, de qualquer período, e a final: o formulário decide por elas se cabe recuperação ou
    // final, mesmo com a tela num período só
    val avaliacoesDoAno: List<AvaliacaoDomain>,
    val faltasPorPeriodo: List<FaltasDoPeriodo>, // só os períodos que têm falta
    val mediasPorPeriodo: List<MediaDoPeriodo>, // todos os do ano, em ordem, mesmo sem nota: o gráfico do resumo
    val media: Double?, // a que vale: com a recuperação (no período) ou a final (no ano), se forem maiores
    val mediaRegular: Double?, // só das avaliações normais
    val substituta: Substituta?,
    val status: StatusDisciplina,
    val faltas: Int, // ano inteiro, como no card do Boletim
    val limiteFaltas: Int?,
    /** Como o ano fecha com as notas de agora, em qualquer filtro. null: nenhum período tem nota, e não dá para finalizar. */
    val fechamento: Fechamento?,
) {
    /** Finalizada (ativa = 0): as notas e as faltas não podem mais ser lançadas, nem mudadas. */
    val finalizada: Boolean
        get() = !disciplina.ativa

    /** Aprovado ou reprovado, depois de finalizada; null enquanto ela está em curso. */
    val situacao: StatusDisciplina?
        get() = if (finalizada) fechamento?.situacao else null

    /** As avaliações do filtro, a final junto no ano letivo inteiro. */
    val avaliacoes: List<AvaliacaoDomain>
        get() = avaliacoesPorPeriodo.flatMap { it.avaliacoes } + finais

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

/** A média que vale num período (com a recuperação, se for maior) e a situação dela contra a mínima. */
data class MediaDoPeriodo(
    val periodo: PeriodoDomain,
    val media: Double?, // null: o período ainda não tem nota
    val status: StatusDisciplina,
)

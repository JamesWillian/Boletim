package app.jammes.boletim.domain.model

/**
 * Tudo o que a tela de ajustes edita num ano letivo, e o que já foi lançado em cada período e
 * disciplina: é isso que impede de tirá-los do ano.
 */
data class AjustesAnoLetivo(
    val anoLetivo: AnoLetivoDomain, // com os períodos, em ordem
    val regra: RegraAvaliacaoDomain, // a padrão do ano, a que vale para as disciplinas
    val materias: List<MateriaDomain>, // todas: a tela escolhe quais entram no ano
    val disciplinas: List<DisciplinaDomain>, // as do ano, uma por matéria que já está nele
    val lancamentosPorPeriodo: Map<Long, Lancamentos>, // id do período → o que foi lançado nele
    val lancamentosPorDisciplina: Map<Long, Lancamentos>, // id da disciplina → o que foi lançado nela
)

/**
 * Avaliações e faltas já lançadas num período ou numa disciplina. Apagar um deles apagaria esses
 * dados junto (as chaves estrangeiras são CASCADE), então só pode sair do ano quando está vazio.
 */
data class Lancamentos(
    val avaliacoes: Int = 0,
    val faltas: Int = 0, // em aulas, como o Boletim conta
) {
    val vazio: Boolean
        get() = avaliacoes == 0 && faltas == 0
}

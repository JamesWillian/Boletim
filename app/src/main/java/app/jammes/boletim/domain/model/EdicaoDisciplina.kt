package app.jammes.boletim.domain.model

/**
 * O que a tela de edição precisa para editar uma disciplina: ela mesma e o que dá sentido aos campos.
 * A matéria de onde ela veio guarda a cor e o ícone originais (o "Redefinir"); o ano letivo, as datas
 * em que o período da disciplina pode cair; e as regras, a do ano e a própria da disciplina, se ela
 * tiver uma.
 */
data class EdicaoDisciplina(
    val disciplina: DisciplinaDomain,
    val materia: MateriaDomain?, // null se ela não for encontrada: aí não há o que redefinir
    val anoLetivo: AnoLetivoDomain?, // com os períodos, em ordem
    val regraDoAno: RegraAvaliacaoDomain, // a padrão, que vale para as disciplinas sem regra própria
    val regraPropria: RegraAvaliacaoDomain?, // a só desta disciplina, no lugar da do ano; null: segue a do ano
)

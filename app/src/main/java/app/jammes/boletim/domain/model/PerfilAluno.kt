package app.jammes.boletim.domain.model

/**
 * O que o perfil do aluno mostra: o aluno e o ano abertos no app, e para onde dá para trocar.
 */
data class PerfilAluno(
    val aluno: AlunoDomain, // o aberto no app
    val anoLetivo: AnoLetivoDomain?, // o aberto no app; null se ele não existir mais
    val anosLetivos: List<AnoLetivoDomain>, // os do aluno, do mais recente ao mais antigo, com os períodos
    val alunos: List<AlunoComAno>, // todos os deste aparelho, na ordem do cadastro
)

/**
 * Um aluno e o ano letivo que abre quando ele é escolhido: o mais recente; null se não tiver nenhum.
 * O aluno que já está aberto fica no ano aberto agora, porque escolhê-lo não troca nada.
 */
data class AlunoComAno(
    val aluno: AlunoDomain,
    val anoLetivo: AnoLetivoDomain?,
)

/** O que é de um aluno no aparelho: tudo isso sai junto quando ele é excluído. */
data class ResumoDoAluno(
    val anosLetivos: Int = 0,
    val disciplinas: Int = 0,
    val avaliacoes: Int = 0,
    val faltas: Int = 0, // em aulas, como o Boletim conta
)

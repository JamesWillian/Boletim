package app.jammes.boletim.domain.model

/** O que a notificação do celular mostra de um aluno: as notificações que ele ainda não viu no app. */
data class AvisoDoAluno(
    val alunoId: Long,
    val nome: String?, // como ele aparece no aviso; null com um aluno só no aparelho
    val notificacoes: List<NotificacaoDomain>, // da mais nova para a mais antiga
)

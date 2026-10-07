package app.jammes.boletim.domain.model

data class Contexto(
    val alunoId: Long,
    val anoLetivoId: Long,
    val periodoId: Long?, // null: o ano letivo inteiro (chip "Todos")
)

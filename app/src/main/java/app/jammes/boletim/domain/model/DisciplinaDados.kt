package app.jammes.boletim.domain.model

data class DisciplinaDados(
    val disciplinaId: Long,
    val nome: String,
    val abreviacao: String?, // a sigla do gráfico do Boletim; vazia, sai do nome
    val cor: Int,
    val icone: IconeMateria,
    val avaliacoes: List<AvaliacaoDomain>,
    val faltas: List<FaltaDomain>,
    val totalAulas: Int?,
    val ativa: Boolean, // false: finalizada, com a situação do fim do ano
)
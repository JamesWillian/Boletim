package app.jammes.boletim.domain.model

/**
 * Como a disciplina fecha o ano: a situação em que ela fica ao ser finalizada, e o que levou a ela.
 * Sai da média do ano (com a final, se ela for maior) contra a mínima, e das faltas contra o limite.
 *
 * Não é gravada. Finalizada, a disciplina não aceita mais notas nem faltas; o que ainda pode mudar
 * (a regra do ano, o total de aulas) muda a situação junto, e ela nunca contradiz o que a tela mostra.
 */
data class Fechamento(
    val mediaDoAno: Double,
    val mediaMinima: Double,
    val faltas: Int, // do ano inteiro
    val limiteFaltas: Int?, // null enquanto o total de aulas não foi informado: aí as faltas não reprovam
    /** Os períodos ainda sem nota, em ordem. Ficam fora da média do ano, e a confirmação avisa. */
    val periodosSemNota: List<PeriodoDomain>,
) {
    val abaixoDaMinima: Boolean
        get() = mediaDoAno < mediaMinima

    /** Mais faltas do que o permitido. No limite exato ainda passa: a frequência fica na mínima. */
    val passouDasFaltas: Boolean
        get() = limiteFaltas != null && faltas > limiteFaltas

    /** Só [StatusDisciplina.APROVADO] ou [StatusDisciplina.REPROVADO]: os outros são de quem está em curso. */
    val situacao: StatusDisciplina
        get() = if (abaixoDaMinima || passouDasFaltas) StatusDisciplina.REPROVADO else StatusDisciplina.APROVADO
}

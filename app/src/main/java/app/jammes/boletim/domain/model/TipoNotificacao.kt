package app.jammes.boletim.domain.model

/**
 * Do que a notificação trata. Dá o ícone na lista e, no resumo da notificação do celular, junta as do
 * mesmo tipo ("2 notas para lançar"). Gravado pelo nome; um nome que não existe mais vira [AVISO].
 */
enum class TipoNotificacao {
    AVALIACAO, // uma prova, trabalho ou seminário chegando
    NOTA, // uma avaliação que já passou e ainda está sem nota
    MEDIA, // a média abaixo da mínima, ou perto dela
    FALTAS, // perto do limite de faltas
    PERIODO, // o período terminando
    AVISO; // qualquer outro aviso

    companion object {
        fun fromString(value: String?): TipoNotificacao =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: AVISO

        fun toString(value: TipoNotificacao): String = value.name
    }
}

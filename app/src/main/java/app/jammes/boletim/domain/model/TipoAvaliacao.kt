package app.jammes.boletim.domain.model

enum class TipoAvaliacao(val displayname: String) {

    NORMAL("Normal"),
    RECUPERACAO("Recuperação"),
    FINAL("Final");

    companion object {
        fun fromString(value: String?): TipoAvaliacao {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: NORMAL
        }

        fun toString(value: TipoAvaliacao): String = value.name.uppercase()
    }
}
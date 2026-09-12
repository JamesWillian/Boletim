package app.jammes.boletim.domain.model

enum class TipoPeriodo(val displayName: String) {
    UNIDADE("Unidade"),
    BIMESTRE("Bimestre"),
    TRIMESTRE("Trimestre"),
    SEMESTRE("Semestre");

    companion object {
        fun fromString(value: String?): TipoPeriodo {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: UNIDADE
        }

        fun toString(value: TipoPeriodo): String = value.name.uppercase()
    }
}
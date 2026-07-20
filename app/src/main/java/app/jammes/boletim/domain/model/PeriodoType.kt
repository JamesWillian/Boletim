package app.jammes.boletim.domain.model

enum class PeriodoType(val displayName: String) {
    UNIDADE("Unidade"),
    BIMESTRE("Bimestre"),
    TRIMESTRE("Trimestre"),
    SEMESTRE("Semestre");

    companion object {
        fun fromString(value: String?): PeriodoType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: UNIDADE
        }
    }
}
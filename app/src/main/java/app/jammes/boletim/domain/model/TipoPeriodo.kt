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

// Os nomes dos períodos ficam aqui, e não só na tela, porque as notificações também os escrevem

/** Unidade é palavra feminina (1ª Unidade); bimestre, trimestre e semestre, masculinas (1º Bimestre). */
val TipoPeriodo.nomeFeminino: Boolean
    get() = this == TipoPeriodo.UNIDADE

/** "1ª" ou "1º", conforme o tipo. */
fun TipoPeriodo.ordinalDe(numero: Int): String = "$numero${if (nomeFeminino) "ª" else "º"}"

/** "3º Bimestre", "1ª Unidade". */
fun TipoPeriodo.nomeDe(numero: Int): String = "${ordinalDe(numero)} $displayName"
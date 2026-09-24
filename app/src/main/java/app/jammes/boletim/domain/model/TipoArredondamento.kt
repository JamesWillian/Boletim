package app.jammes.boletim.domain.model

enum class TipoArredondamento(val displayName: String) {

    NENHUM("Nenhum"), //guarda 2 casas, exibe com 1 ou 2 casas. Default.
    MEIO_PONTO("Meio Ponto"), //arredonda pro 0,5 mais próximo. 7,3 → 7,5.
    INTEIRO("Inteiro"), //arredonda pro inteiro mais próximo. 7,4 → 7 e 7,5 → 8.
    CIMA("Para Cima (1 casa)"), //sempre pra cima, na 1ª casa. 6,62 → 6,7.
    BAIXO("Para Baixo (1 casa)"); //sempre pra baixo, na 1ª casa (trunca). 6,68 → 6,6.

    companion object {
        fun fromString(value: String?): TipoArredondamento {
            return entries.firstOrNull { it.name.equals( value, ignoreCase = true) } ?: NENHUM
        }

        fun toString(value: TipoArredondamento): String {
            return value.name.uppercase()
        }
    }
}
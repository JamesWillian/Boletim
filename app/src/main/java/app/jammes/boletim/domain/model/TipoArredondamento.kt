package app.jammes.boletim.domain.model

enum class TipoArredondamento(val displayName: String) {

    NENHUM("Nenhum"), //guarda o valor cheio, exibe com 1 ou 2 casas. Default.
    MEIO_PONTO("Meio Ponto"), //arredonda pro 0,5 mais próximo. 7,3 → 7,5.
    INTEIRO("Inteiro"), //arredonda pro inteiro mais próximo. 7,4 → 7 e 7,5 → 8.
    CIMA("Para Cima"), //sempre pra cima
    BAIXO("Para Baixo"); //sempre pra baixo

    companion object {
        fun fromString(value: String?): TipoArredondamento {
            return entries.firstOrNull { it.name.equals( value, ignoreCase = true) } ?: NENHUM
        }

        fun toString(value: TipoArredondamento): String {
            return value.name.uppercase()
        }
    }
}
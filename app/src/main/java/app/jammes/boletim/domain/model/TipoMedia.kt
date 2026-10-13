package app.jammes.boletim.domain.model

enum class TipoMedia(val displayName: String) {

    PONDERADA("Ponderada"), //cada avaliação conta pelo seu peso. Σ(nota × peso) / Σ(peso), com cada nota na escala de 10 (nota × 10 / máxima).
    SIMPLES("Simples"), //média aritmética, ignora o campo peso. Σ(nota) / n, também na escala de 10.
    SOMA("Soma"); //não divide, só soma os pontos como foram lançados (a máxima não entra). Cobre a escola que distribui 10 pontos entre as avaliações do período ("prova 6, trabalho 4").

    companion object {
        fun fromString(value: String?): TipoMedia {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: PONDERADA
        }

        fun toString(value: TipoMedia): String = value.name.uppercase()
    }
}
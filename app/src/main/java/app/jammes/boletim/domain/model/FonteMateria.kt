package app.jammes.boletim.domain.model

/**
 * De onde a matéria entrou no cadastro. Por enquanto só fica gravada: nenhuma tela mostra.
 *
 * O banco guarda o [codigo], então um código não pode mudar nem ser reaproveitado. Um código que não
 * existe vira [USUARIO]: sem saber de onde a matéria veio, ela é tratada como da pessoa.
 */
enum class FonteMateria(val codigo: Int) {
    APP(1), // as matérias padrão, que o app grava quando o banco é criado
    USUARIO(2), // criada pela pessoa (onboarding, matérias do ano, cadastro)
    REMOTA(3); // ainda não usada: as que vierem do Remote Config ou de uma Cloud Function

    companion object {
        fun fromInt(value: Int?): FonteMateria = entries.firstOrNull { it.codigo == value } ?: USUARIO

        fun toInt(value: FonteMateria): Int = value.codigo
    }
}

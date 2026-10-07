package app.jammes.boletim.domain.model

/**
 * O ícone de uma matéria. Só identifica o ícone, pelo que ele mostra: qual imagem desenhar é a
 * camada de apresentação que decide (IconesDisciplina). Trocar a imagem não mexe no banco.
 *
 * O banco guarda o nome da constante, então renomear uma delas faz as matérias que a usam
 * voltarem para o [PADRAO].
 */
enum class IconeMateria {
    // Os das matérias padrão
    LIVRO,
    CALCULADORA,
    PERGAMINHO,
    GLOBO,
    MICROSCOPIO,
    IDIOMA,
    PALETA,
    BOLA,
    RAIO,
    FRASCO,
    FOLHA,
    MENTE,
    PESSOAS,

    // Para as matérias que o usuário criar
    ESCOLA,
    NOTA_MUSICAL,
    COMPUTADOR,
    CODIGO,
    REDACAO,
    DINHEIRO,
    ROBO,
    TEATRO,
    REGUA;

    companion object {
        /** O de uma matéria nova, e o que vale quando o banco tem um nome que não existe mais. */
        val PADRAO = ESCOLA

        fun fromString(value: String?): IconeMateria {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: PADRAO
        }

        fun toString(value: IconeMateria): String = value.name.uppercase()
    }
}

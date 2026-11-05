package app.jammes.boletim.domain.model

import java.io.Serializable

/**
 * Uma matéria do cadastro. É Serializable porque a criada no onboarding ou nas matérias do ano fica no
 * formulário, ainda sem id, até o Salvar, e o rememberSaveable guarda os formulários.
 */
data class MateriaDomain(
    val id: Long = 0L,
    val nome: String = "",
    val abreviacao: String? = null,
    val cor: Int = 0,
    val icone: IconeMateria = IconeMateria.PADRAO,
    val fonte: FonteMateria = FonteMateria.USUARIO
) : Serializable {

    /** Criada pela pessoa. As outras (as padrão e as que vierem de fora) são "do app". */
    val propria: Boolean
        get() = fonte == FonteMateria.USUARIO
}

package app.jammes.boletim.domain.model

import java.io.Serializable

/** Uma matéria do cadastro e em quantos anos letivos ela está em uso, de qualquer aluno. */
data class MateriaDoCadastro(
    val materia: MateriaDomain,
    val anosEmUso: Int = 0,
) : Serializable {

    /** Por que a matéria não pode sair do cadastro; null quando pode. */
    val impedimentoDeExcluir: ImpedimentoDeExcluir?
        get() = when {
            !materia.propria -> ImpedimentoDeExcluir.DoApp
            anosEmUso > 0 -> ImpedimentoDeExcluir.EmUso(anosEmUso)
            else -> null
        }
}

/** O que segura uma matéria no cadastro. */
sealed interface ImpedimentoDeExcluir {
    /** As que o app traz ficam sempre, em qualquer plano. */
    data object DoApp : ImpedimentoDeExcluir

    /** Uma disciplina precisa da matéria (a chave estrangeira não deixa apagar). */
    data class EmUso(val anos: Int) : ImpedimentoDeExcluir
}

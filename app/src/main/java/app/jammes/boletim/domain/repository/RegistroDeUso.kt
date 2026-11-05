package app.jammes.boletim.domain.repository

/**
 * Como o app é usado, em números: o desenvolvedor fica sabendo quais telas as pessoas abrem. Só vão
 * nomes fixos, escritos no código: nunca nome, matéria, nota ou falta.
 */
interface RegistroDeUso {

    fun telaAberta(nome: String)
}

package app.jammes.boletim.domain.repository

/**
 * Os erros que o app contorna sem fechar, mas que não deviam acontecer: o desenvolvedor fica sabendo
 * deles. O que a própria pessoa pode causar, como a senha errada, não vem para cá. O erro vai inteiro,
 * com a mensagem: ela nunca leva nome, matéria, nota ou falta.
 */
interface RegistroDeFalhas {

    fun registrar(erro: Throwable)
}

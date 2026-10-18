package app.jammes.boletim.avisos

import app.jammes.boletim.domain.model.AvisoDoAluno

/** Quantas notificações o aviso aberto lista; as outras viram "e mais N". */
internal const val LINHAS_NO_AVISO = 5

// O aviso de um aluno ------------------------------------------------------------------------

/** O título do aviso de um aluno: o da notificação, quando ela é uma só; senão, quantas são. */
internal fun tituloDoAviso(aviso: AvisoDoAluno): String =
    aviso.notificacoes.singleOrNull()?.titulo ?: quantosAvisos(aviso.notificacoes.size)

/** A linha de baixo do aviso fechado: o texto da notificação, ou os títulos de todas. */
internal fun textoDoAviso(aviso: AvisoDoAluno): String =
    aviso.notificacoes.singleOrNull()?.texto ?: aviso.notificacoes.joinToString(" · ") { it.titulo }

/**
 * As linhas do aviso aberto, com várias notificações: um título por linha, das mais novas para as
 * mais antigas, e "e mais N" no fim quando nem todas cabem.
 */
internal fun linhasDoAviso(aviso: AvisoDoAluno): List<String> {
    val titulos = aviso.notificacoes.map { it.titulo }
    if (titulos.size <= LINHAS_NO_AVISO) return titulos
    // A última linha dá lugar ao "e mais": o aviso aberto nunca passa de LINHAS_NO_AVISO
    return titulos.take(LINHAS_NO_AVISO - 1) + "e mais ${titulos.size - (LINHAS_NO_AVISO - 1)}"
}

// O resumo do grupo, com um aluno por linha ---------------------------------------------------

/** "7 avisos"; com um só, "1 aviso". */
internal fun tituloDoResumo(avisos: List<AvisoDoAluno>): String = quantosAvisos(avisos.sumOf { it.notificacoes.size })

/**
 * A linha de um aluno no resumo: "James: Prova de Matemática amanhã e mais 2". Com um aluno só no
 * aparelho, sem o nome.
 */
internal fun linhaDoResumo(aviso: AvisoDoAluno): String {
    val primeira = aviso.notificacoes.first().titulo
    val outras = aviso.notificacoes.size - 1
    val texto = if (outras > 0) "$primeira e mais $outras" else primeira
    return aviso.nome?.let { "$it: $texto" } ?: texto
}

private fun quantosAvisos(quantos: Int): String = if (quantos == 1) "1 aviso" else "$quantos avisos"

package app.jammes.boletim.domain.model

import kotlin.time.Instant

/** O que um arquivo de backup traz, para a pessoa conferir antes de importar. */
data class ResumoDoBackup(
    val exportadoEm: Instant,
    val alunos: List<String>, // os nomes, na ordem do cadastro
    val anosLetivos: Int,
    val avaliacoes: Int,
)

/**
 * Um arquivo de backup já lido e conferido: o [resumo] para a confirmação e o [conteudo], que é o
 * texto do arquivo, para gravar depois que a pessoa confirmar.
 */
data class BackupLido(
    val resumo: ResumoDoBackup,
    val conteudo: String,
)

/** O arquivo não dá para importar; a mensagem diz por quê, do jeito que a tela mostra. */
class BackupInvalidoException(mensagem: String) : Exception(mensagem)

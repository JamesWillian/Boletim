package app.jammes.boletim.domain.repository

import app.jammes.boletim.domain.model.ArquivoLido
import app.jammes.boletim.domain.model.BackupComSenha
import app.jammes.boletim.domain.model.BackupInvalidoException
import app.jammes.boletim.domain.model.BackupLido
import app.jammes.boletim.domain.model.SenhaIncorretaException

/**
 * O backup em arquivo: os dados de todos os alunos num arquivo só, que a pessoa guarda onde quiser
 * e importa depois, neste aparelho ou em outro. Os arquivos chegam pelo endereço que o seletor de
 * arquivos do Android devolve (a uri, em texto).
 */
interface BackupRepository {

    /**
     * Grava no arquivo [destino] os dados de todos os alunos. Com a [senha], eles vão
     * criptografados, e só ela abre o arquivo depois; null, vão abertos, para qualquer um ler.
     */
    suspend fun exportar(destino: String, senha: String?)

    /**
     * Lê o arquivo [origem] e confere se ele é um backup que dá para importar, sem gravar nada. Um
     * backup com senha volta fechado, para o [abrir]. Se não der para importar, sai
     * [BackupInvalidoException] com o motivo.
     */
    suspend fun ler(origem: String): ArquivoLido

    /**
     * Abre com a [senha] o [backup] que o [ler] devolveu fechado, e confere ele do mesmo jeito. Com
     * a senha errada sai [SenhaIncorretaException].
     */
    suspend fun abrir(backup: BackupComSenha, senha: String): BackupLido

    /**
     * Apaga os dados do aparelho e grava os do [backup] no lugar, com os mesmos ids. É tudo de uma
     * vez: se algo falhar no meio, os dados de antes continuam como estavam.
     */
    suspend fun substituirTudo(backup: BackupLido)
}

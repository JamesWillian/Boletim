package app.jammes.boletim.domain.repository

import app.jammes.boletim.domain.model.BackupInvalidoException
import app.jammes.boletim.domain.model.BackupLido

/**
 * O backup em arquivo: os dados de todos os alunos num arquivo só, que a pessoa guarda onde quiser
 * e importa depois, neste aparelho ou em outro. Os arquivos chegam pelo endereço que o seletor de
 * arquivos do Android devolve (a uri, em texto).
 */
interface BackupRepository {

    /** Grava no arquivo [destino] os dados de todos os alunos. */
    suspend fun exportar(destino: String)

    /**
     * Lê o arquivo [origem] e confere se ele é um backup que dá para importar, sem gravar nada.
     * Se não for, sai [BackupInvalidoException] com o motivo.
     */
    suspend fun ler(origem: String): BackupLido

    /**
     * Apaga os dados do aparelho e grava os do [backup] no lugar, com os mesmos ids. É tudo de uma
     * vez: se algo falhar no meio, os dados de antes continuam como estavam.
     */
    suspend fun substituirTudo(backup: BackupLido)
}

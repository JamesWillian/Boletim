package app.jammes.boletim.data.repository

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import android.net.Uri
import androidx.room.withTransaction
import app.jammes.boletim.data.backup.BACKUP_ALTERADO
import app.jammes.boletim.data.backup.CopiaDoBanco
import app.jammes.boletim.data.backup.NAO_E_BACKUP
import app.jammes.boletim.data.backup.escreverBackup
import app.jammes.boletim.data.backup.lerBackup
import app.jammes.boletim.data.backup.resumo
import app.jammes.boletim.data.local.AppDatabase
import app.jammes.boletim.data.local.dao.BackupDao
import app.jammes.boletim.domain.model.BackupInvalidoException
import app.jammes.boletim.domain.model.BackupLido
import app.jammes.boletim.domain.repository.BackupRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.time.Clock

class BackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val dao: BackupDao,
) : BackupRepository {

    override suspend fun exportar(destino: String) {
        // Numa transação, a cópia é de um momento só, mesmo que algo seja gravado no meio
        val copia = db.withTransaction { copiarTudo() }
        val texto = withContext(Dispatchers.Default) { escreverBackup(copia, exportadoEm = Clock.System.now()) }
        withContext(Dispatchers.IO) {
            abrirParaEscrever(Uri.parse(destino)).use { it.write(texto.toByteArray(Charsets.UTF_8)) }
        }
    }

    override suspend fun ler(origem: String): BackupLido {
        val texto = withContext(Dispatchers.IO) {
            val entrada = context.contentResolver.openInputStream(Uri.parse(origem))
                ?: throw IOException("O arquivo não abriu")
            entrada.use { it.lerTexto() }
        }
        val arquivo = withContext(Dispatchers.Default) { lerBackup(texto) }
        return BackupLido(resumo = arquivo.resumo(), conteudo = texto)
    }

    override suspend fun substituirTudo(backup: BackupLido) {
        val copia = withContext(Dispatchers.Default) { lerBackup(backup.conteudo).copia }
        try {
            db.withTransaction {
                apagarTudo()
                gravar(copia)
            }
        } catch (e: SQLiteConstraintException) {
            // O que a conferência do arquivo não pega, como dois períodos com o mesmo número no ano.
            // A transação já desfez tudo: os dados de antes continuam
            throw BackupInvalidoException(BACKUP_ALTERADO)
        } catch (e: SQLiteException) {
            throw IOException("Os dados não foram gravados", e) // sem espaço no aparelho, por exemplo
        }
    }

    private suspend fun copiarTudo() = CopiaDoBanco(
        materias = dao.materias(),
        alunos = dao.alunos(),
        anosLetivos = dao.anosLetivos(),
        periodos = dao.periodos(),
        disciplinas = dao.disciplinas(),
        regras = dao.regras(),
        avaliacoes = dao.avaliacoes(),
        faltas = dao.faltas(),
    )

    // Dos filhos para os pais, como o BackupDao pede
    private suspend fun apagarTudo() {
        dao.apagarFaltas()
        dao.apagarAvaliacoes()
        dao.apagarRegras()
        dao.apagarDisciplinas()
        dao.apagarPeriodos()
        dao.apagarAnosLetivos()
        dao.apagarAlunos()
        dao.apagarMaterias()
    }

    // Dos pais para os filhos: a regra de uma disciplina entra depois dela
    private suspend fun gravar(copia: CopiaDoBanco) {
        dao.inserirMaterias(copia.materias)
        dao.inserirAlunos(copia.alunos)
        dao.inserirAnosLetivos(copia.anosLetivos)
        dao.inserirPeriodos(copia.periodos)
        dao.inserirDisciplinas(copia.disciplinas)
        dao.inserirRegras(copia.regras)
        dao.inserirAvaliacoes(copia.avaliacoes)
        dao.inserirFaltas(copia.faltas)
    }

    /**
     * "wt" esvazia o arquivo antes de escrever, para não sobrar nada de um conteúdo anterior. Nem todo
     * app de arquivos aceita esse modo; aí vai "w", que serve para o arquivo novo que o seletor criou.
     */
    private fun abrirParaEscrever(uri: Uri): OutputStream {
        val resolver = context.contentResolver
        val saida = try {
            resolver.openOutputStream(uri, "wt")
        } catch (e: FileNotFoundException) {
            null
        }
        return saida ?: resolver.openOutputStream(uri, "w") ?: throw IOException("O arquivo não abriu para gravar")
    }
}

// Bem mais que qualquer backup: um arquivo maior que isso nem é lido até o fim
private const val LIMITE_DO_ARQUIVO = 32 * 1024 * 1024

/** O arquivo inteiro como texto, sem a marca de UTF-8 que alguns editores põem no começo. */
private fun InputStream.lerTexto(): String {
    val bytes = ByteArrayOutputStream()
    val pedaco = ByteArray(8 * 1024)
    while (true) {
        val lidos = read(pedaco)
        if (lidos < 0) break
        bytes.write(pedaco, 0, lidos)
        if (bytes.size() > LIMITE_DO_ARQUIVO) throw BackupInvalidoException(NAO_E_BACKUP)
    }
    return String(bytes.toByteArray(), Charsets.UTF_8).removePrefix("\uFEFF")
}

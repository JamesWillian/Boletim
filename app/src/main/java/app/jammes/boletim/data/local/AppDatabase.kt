package app.jammes.boletim.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import app.jammes.boletim.data.local.dao.AlunoDao
import app.jammes.boletim.data.local.dao.AnoLetivoDao
import app.jammes.boletim.data.local.dao.AvaliacaoDao
import app.jammes.boletim.data.local.dao.BackupDao
import app.jammes.boletim.data.local.dao.DisciplinaDao
import app.jammes.boletim.data.local.dao.FaltaDao
import app.jammes.boletim.data.local.dao.MateriaDao
import app.jammes.boletim.data.local.dao.NotificacaoDao
import app.jammes.boletim.data.local.dao.PeriodoDao
import app.jammes.boletim.data.local.dao.RegraAvaliacaoDao
import app.jammes.boletim.data.local.entity.AlunoEntity
import app.jammes.boletim.data.local.entity.AnoLetivoEntity
import app.jammes.boletim.data.local.entity.AvaliacaoEntity
import app.jammes.boletim.data.local.entity.DisciplinaEntity
import app.jammes.boletim.data.local.entity.FaltaEntity
import app.jammes.boletim.data.local.entity.MateriaEntity
import app.jammes.boletim.data.local.entity.NotificacaoEntity
import app.jammes.boletim.data.local.entity.PeriodoEntity
import app.jammes.boletim.data.local.entity.RegraAvaliacaoEntity
import app.jammes.boletim.data.mapper.Converters

/**
 * Toda mudança na estrutura (tabela, coluna, índice, chave estrangeira) é uma versão nova do banco, com o
 * caminho para sair da anterior. O schema de cada versão fica em app/schemas e vai pro git.
 *
 * Para mudar a estrutura:
 * 1. Aumentar a `version` junto com a mudança e compilar: o Room grava o N.json da versão nova.
 * 2. Dizer como chegar nela: uma AutoMigration em `autoMigrations`, quando o Room deduz sozinho (tabela ou
 *    coluna nova; renomear ou apagar pede um AutoMigrationSpec), ou uma Migration escrita à mão em MIGRACOES,
 *    quando tem dado para preencher ou converter.
 * 3. Fixar o hash da versão nova no SchemasTest e testar o caminho no MigracoesTest.
 * 4. Se a tabela vai pro arquivo de backup, atualizar o ArquivoDeBackup e a VERSAO_DO_ARQUIVO.
 *
 * Não tem fallbackToDestructiveMigration: faltar uma migração derruba o app em vez de apagar os dados.
 */
@Database(
    entities = [
        AlunoEntity::class,
        AnoLetivoEntity::class,
        AvaliacaoEntity::class,
        DisciplinaEntity::class,
        FaltaEntity::class,
        MateriaEntity::class,
        PeriodoEntity::class,
        RegraAvaliacaoEntity::class,
        NotificacaoEntity::class
    ],
    version = 1, // 1: a primeira com o schema exportado
    autoMigrations = [],
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun alunoDao() : AlunoDao
    abstract fun anoLetivoDao() : AnoLetivoDao
    abstract fun avaliacaoDao() : AvaliacaoDao
    abstract fun disciplinaDao() : DisciplinaDao
    abstract fun faltaDao() : FaltaDao
    abstract fun materiaDao() : MateriaDao
    abstract fun periodoDao() : PeriodoDao
    abstract fun regraAvaliacaoDao() : RegraAvaliacaoDao
    abstract fun backupDao() : BackupDao
    abstract fun notificacaoDao() : NotificacaoDao

    companion object {

        const val NAME = "boletim.db"

        // As AutoMigrations o Room registra sozinho; aqui só as escritas à mão
        val MIGRACOES = arrayOf<Migration>()

    }
}
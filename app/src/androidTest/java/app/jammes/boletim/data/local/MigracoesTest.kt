package app.jammes.boletim.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * As migrações do banco, a partir dos schemas de app/schemas (o plugin do Room põe esses arquivos nos assets do teste).
 * Cada Migration escrita à mão ganha aqui também um teste com dados: helper.createDatabase na versão de antes,
 * linhas inseridas por SQL, e conferir o que sai de helper.runMigrationsAndValidate.
 * Usa um arquivo de banco separado, então não mexe no boletim.db do app.
 */
@RunWith(AndroidJUnit4::class)
class MigracoesTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @get:Rule
    val helper = MigrationTestHelper(instrumentation, AppDatabase::class.java)

    @After fun apagarBanco() {
        context.deleteDatabase(NOME_BANCO)
    }

    // Depois de migrar, o Room confere cada tabela, coluna, índice e chave estrangeira: se o caminho
    // não chega exatamente no banco atual, ele não abre
    @Test fun de_qualquer_versao_chega_no_banco_atual() {
        val versoes = versoesExportadas()
        assertTrue(versoes.isNotEmpty())

        versoes.forEach { versao ->
            context.deleteDatabase(NOME_BANCO)
            helper.createDatabase(NOME_BANCO, versao).close()

            val db = Room.databaseBuilder(context, AppDatabase::class.java, NOME_BANCO)
                .addMigrations(*AppDatabase.MIGRACOES)
                .build()
            db.openHelper.writableDatabase // abrir é o que migra e valida
            db.close()
        }
    }

    private fun versoesExportadas() = instrumentation.context.assets
        .list(AppDatabase::class.java.name).orEmpty()
        .map { arquivo -> arquivo.removeSuffix(".json").toInt() }
        .sorted()

    companion object {
        private const val NOME_BANCO = "migracoes-test.db"
    }
}

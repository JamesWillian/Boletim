package app.jammes.boletim.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

// Usa um arquivo de banco separado, então não mexe no boletim.db do app.
@RunWith(AndroidJUnit4::class)
class MateriasPadraoCallbackTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun abrirBanco() = Room.databaseBuilder(context, AppDatabase::class.java, NOME_BANCO)
        .addCallback(MateriasPadraoCallback())
        .build()

    @After fun apagarBanco() {
        context.deleteDatabase(NOME_BANCO)
    }

    @Test fun banco_novo_vem_com_as_materias_padrao() = runBlocking {
        val db = abrirBanco()
        val materias = db.materiaDao().observar().first()
        db.close()

        assertEquals(MateriasPadraoCallback.MATERIAS, materias.map { it.copy(id = 0L) })
    }

    @Test fun materias_apagadas_nao_voltam_ao_reabrir_o_banco() = runBlocking {
        val db = abrirBanco()
        db.materiaDao().observar().first().forEach { db.materiaDao().delete(it) }
        db.close()

        val reaberto = abrirBanco()
        val materias = reaberto.materiaDao().observar().first()
        reaberto.close()

        assertTrue(materias.isEmpty())
    }

    companion object {
        private const val NOME_BANCO = "materias-padrao-test.db"
    }
}

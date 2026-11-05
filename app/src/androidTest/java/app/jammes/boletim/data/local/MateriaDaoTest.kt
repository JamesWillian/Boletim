package app.jammes.boletim.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.jammes.boletim.data.local.entity.AlunoEntity
import app.jammes.boletim.data.local.entity.AnoLetivoEntity
import app.jammes.boletim.data.local.entity.DisciplinaEntity
import app.jammes.boletim.data.local.entity.MateriaEntity
import app.jammes.boletim.data.local.entity.UsoDaMateria
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Instant

/**
 * Em quantos anos letivos cada matéria está, e a chave estrangeira que segura a matéria em uso.
 * Usa um arquivo de banco separado, então não mexe no boletim.db do app.
 */
@RunWith(AndroidJUnit4::class)
class MateriaDaoTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: AppDatabase

    @Before fun abrirBanco() {
        context.deleteDatabase(NOME_BANCO)
        db = Room.databaseBuilder(context, AppDatabase::class.java, NOME_BANCO).build()
    }

    @After fun apagarBanco() {
        db.close()
        context.deleteDatabase(NOME_BANCO)
    }

    @Test fun conta_os_anos_de_cada_materia_de_qualquer_aluno() = runBlocking {
        val espanhol = gravarMateria("Espanhol")
        val robotica = gravarMateria("Robótica")
        val teatro = gravarMateria("Teatro")
        val ana = gravarAluno("Ana")
        val bia = gravarAluno("Bia")
        gravarDisciplina(espanhol, gravarAno(ana, 2025))
        gravarDisciplina(espanhol, gravarAno(ana, 2026))
        gravarDisciplina(espanhol, gravarAno(bia, 2026))
        gravarDisciplina(robotica, gravarAno(bia, 2027))

        val usos = db.materiaDao().observarAnosEmUso().first().toSet()

        // O Teatro não está em nenhum ano: fica de fora
        assertEquals(setOf(UsoDaMateria(espanhol, 3), UsoDaMateria(robotica, 1)), usos)
        assertTrue(usos.none { it.materiaId == teatro })
    }

    @Test fun materia_em_uso_nao_sai_do_cadastro() = runBlocking<Unit> {
        val espanhol = gravarMateria("Espanhol")
        gravarDisciplina(espanhol, gravarAno(gravarAluno("Ana"), 2026))

        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking { db.materiaDao().delete(entidade("Espanhol", espanhol)) }
        }
    }

    @Test fun materia_fora_dos_anos_sai_do_cadastro() = runBlocking {
        val teatro = gravarMateria("Teatro")

        db.materiaDao().delete(entidade("Teatro", teatro))

        assertTrue(db.materiaDao().observar().first().isEmpty())
    }

    private fun entidade(nome: String, id: Long = 0L) =
        MateriaEntity(id = id, nome = nome, abreviacao = nome.take(3).uppercase(), icone = "PADRAO", fonte = 2)

    private suspend fun gravarMateria(nome: String): Long = db.materiaDao().insert(entidade(nome))

    private suspend fun gravarAluno(nome: String): Long =
        db.alunoDao().insert(AlunoEntity(nome = nome, avatar = null, criadoEm = Instant.fromEpochMilliseconds(0)))

    private suspend fun gravarAno(alunoId: Long, ano: Int): Long =
        db.anoLetivoDao().insert(AnoLetivoEntity(alunoId = alunoId, ano = ano, serie = null, qtdPeriodos = 1))

    private suspend fun gravarDisciplina(materiaId: Long, anoLetivoId: Long) {
        db.disciplinaDao().insert(
            DisciplinaEntity(
                materiaId = materiaId,
                anoLetivoId = anoLetivoId,
                nome = "Disciplina",
                abreviacao = null,
                icone = "PADRAO",
                totalAulas = null,
                professor = null,
                periodoInicio = null,
                periodoFim = null,
            )
        )
    }

    private companion object {
        const val NOME_BANCO = "teste-materia.db"
    }
}

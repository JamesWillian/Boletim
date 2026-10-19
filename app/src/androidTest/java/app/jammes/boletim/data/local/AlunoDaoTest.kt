package app.jammes.boletim.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.jammes.boletim.data.local.entity.AlunoEntity
import app.jammes.boletim.data.local.entity.AnoLetivoEntity
import app.jammes.boletim.data.local.entity.AvaliacaoEntity
import app.jammes.boletim.data.local.entity.ContagemDoAluno
import app.jammes.boletim.data.local.entity.DisciplinaEntity
import app.jammes.boletim.data.local.entity.FaltaEntity
import app.jammes.boletim.data.local.entity.MateriaEntity
import app.jammes.boletim.data.local.entity.NotificacaoEntity
import app.jammes.boletim.data.local.entity.PeriodoEntity
import app.jammes.boletim.data.local.entity.RegraAvaliacaoEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import kotlin.time.Instant

/**
 * A contagem do que é de um aluno e a exclusão dele, que leva tudo junto pelas chaves estrangeiras.
 * Usa um arquivo de banco separado, então não mexe no boletim.db do app.
 */
@RunWith(AndroidJUnit4::class)
class AlunoDaoTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: AppDatabase
    // Uma matéria para cada disciplina do ano: a mesma matéria não entra duas vezes no mesmo ano
    private var materiaIds = emptyList<Long>()

    @Before fun abrirBanco() = runBlocking {
        context.deleteDatabase(NOME_BANCO)
        db = Room.databaseBuilder(context, AppDatabase::class.java, NOME_BANCO).build()
        materiaIds = listOf("Português", "Matemática", "História").map { nome ->
            db.materiaDao().insert(MateriaEntity(nome = nome, abreviacao = nome.take(3).uppercase(), icone = "PADRAO", fonte = 2))
        }
    }

    @After fun apagarBanco() {
        db.close()
        context.deleteDatabase(NOME_BANCO)
    }

    @Test fun contagem_tem_so_o_que_e_do_aluno() = runBlocking {
        // prepara: a Ana com 2 anos, a Bia com 1
        val ana = gravarAluno(
            "Ana",
            AnoParaGravar(2025, avaliacoesPorDisciplina = listOf(3, 0), aulasFaltadasPorDisciplina = listOf(listOf(2), emptyList())),
            AnoParaGravar(2026, avaliacoesPorDisciplina = listOf(1), aulasFaltadasPorDisciplina = listOf(listOf(1))),
        )
        val bia = gravarAluno(
            "Bia",
            AnoParaGravar(2026, avaliacoesPorDisciplina = listOf(2), aulasFaltadasPorDisciplina = listOf(listOf(4, 1))),
        )

        // executa e confere: as faltas contam as aulas, não os dias
        assertEquals(ContagemDoAluno(anosLetivos = 2, disciplinas = 3, avaliacoes = 4, faltas = 3), contagem(ana))
        assertEquals(ContagemDoAluno(anosLetivos = 1, disciplinas = 1, avaliacoes = 2, faltas = 5), contagem(bia))
    }

    @Test fun aluno_que_nao_existe_tem_tudo_zerado() = runBlocking {
        assertEquals(ContagemDoAluno(anosLetivos = 0, disciplinas = 0, avaliacoes = 0, faltas = 0), contagem(99))
    }

    @Test fun excluir_o_aluno_apaga_tudo_dele_e_deixa_o_dos_outros() = runBlocking {
        val ana = gravarAluno(
            "Ana",
            AnoParaGravar(2025, avaliacoesPorDisciplina = listOf(3, 1), aulasFaltadasPorDisciplina = listOf(listOf(2), listOf(1))),
            AnoParaGravar(2026, avaliacoesPorDisciplina = listOf(1), aulasFaltadasPorDisciplina = listOf(listOf(1))),
        )
        val bia = gravarAluno(
            "Bia",
            AnoParaGravar(2026, avaliacoesPorDisciplina = listOf(2), aulasFaltadasPorDisciplina = listOf(listOf(4))),
        )
        val backup = db.backupDao()
        val disciplinasDaBia = backup.disciplinas().filter { it.anoLetivoId in anosDe(bia) }.map { it.id }.toSet()

        db.alunoDao().delete(backup.alunos().single { it.id == ana })

        assertEquals(listOf(bia), backup.alunos().map { it.id })
        assertTrue(backup.anosLetivos().all { it.alunoId == bia })
        assertTrue(backup.periodos().all { it.anoLetivoId in anosDe(bia) })
        assertTrue(backup.regras().all { it.anoLetivoId in anosDe(bia) })
        assertEquals(disciplinasDaBia, backup.disciplinas().map { it.id }.toSet())
        assertTrue(backup.avaliacoes().all { it.disciplinaId in disciplinasDaBia })
        assertTrue(backup.faltas().all { it.disciplinaId in disciplinasDaBia })
        assertEquals(2, backup.avaliacoes().size)
        assertEquals(1, backup.faltas().size)
        assertTrue(db.notificacaoDao().doAluno(ana).isEmpty())
        assertEquals(1, db.notificacaoDao().doAluno(bia).size)
        // As matérias são do aparelho: ficam
        assertEquals(materiaIds, backup.materias().map { it.id })
        assertEquals(ContagemDoAluno(0, 0, 0, 0), contagem(ana))
    }

    // Preparação -------------------------------------------------------------------------------

    /** Um ano letivo com um período só e uma disciplina por item das listas (até 3, uma por matéria). */
    private class AnoParaGravar(
        val ano: Int,
        val avaliacoesPorDisciplina: List<Int>,
        val aulasFaltadasPorDisciplina: List<List<Int>>, // cada número é um dia de falta, com as aulas dele
    )

    /** Grava o aluno com os [anos], a regra de cada ano e uma notificação; devolve o id dele. */
    private suspend fun gravarAluno(nome: String, vararg anos: AnoParaGravar): Long {
        val alunoId = db.alunoDao().insert(AlunoEntity(nome = nome, avatar = null, criadoEm = CRIADO_EM))
        for (paraGravar in anos) {
            val anoId = db.anoLetivoDao().insert(
                AnoLetivoEntity(alunoId = alunoId, ano = paraGravar.ano, serie = null, qtdPeriodos = 1)
            )
            val periodoId = db.periodoDao().insert(
                PeriodoEntity(
                    anoLetivoId = anoId,
                    periodo = 1,
                    dataInicio = LocalDate.of(paraGravar.ano, 2, 1),
                    dataFim = LocalDate.of(paraGravar.ano, 12, 15),
                )
            )
            db.regraAvaliacaoDao().insert(RegraAvaliacaoEntity(anoLetivoId = anoId, disciplinaId = null))
            paraGravar.avaliacoesPorDisciplina.forEachIndexed { i, avaliacoes ->
                val disciplinaId = db.disciplinaDao().insert(
                    DisciplinaEntity(
                        materiaId = materiaIds[i],
                        anoLetivoId = anoId,
                        nome = "Disciplina ${i + 1}",
                        abreviacao = null,
                        icone = "PADRAO",
                        totalAulas = null,
                        professor = null,
                        periodoInicio = null,
                        periodoFim = null,
                    )
                )
                repeat(avaliacoes) { n ->
                    db.avaliacaoDao().insert(
                        AvaliacaoEntity(
                            disciplinaId = disciplinaId,
                            periodoId = periodoId,
                            nome = "Prova ${n + 1}",
                            nota = 7.0,
                            notaMaxima = 10.0,
                            peso = 1.0,
                            data = LocalDate.of(paraGravar.ano, 3, n + 1),
                            criadoEm = CRIADO_EM,
                        )
                    )
                }
                paraGravar.aulasFaltadasPorDisciplina[i].forEachIndexed { dia, aulas ->
                    db.faltaDao().insert(
                        FaltaEntity(
                            disciplinaId = disciplinaId,
                            periodoId = periodoId,
                            data = LocalDate.of(paraGravar.ano, 4, dia + 1),
                            qtdAulas = aulas,
                        )
                    )
                }
            }
        }
        db.notificacaoDao().inserir(
            listOf(
                NotificacaoEntity(
                    alunoId = alunoId,
                    tipo = "NOTA",
                    chave = "teste:$alunoId",
                    titulo = "Aviso de $nome",
                    texto = "",
                    criadaEm = CRIADO_EM,
                )
            )
        )
        return alunoId
    }

    private suspend fun anosDe(alunoId: Long): Set<Long> =
        db.backupDao().anosLetivos().filter { it.alunoId == alunoId }.map { it.id }.toSet()

    private suspend fun contagem(alunoId: Long) = db.alunoDao().observarContagem(alunoId).first()

    companion object {
        private const val NOME_BANCO = "aluno-dao-test.db"
        private val CRIADO_EM = Instant.fromEpochSeconds(1_760_000_000)
    }
}

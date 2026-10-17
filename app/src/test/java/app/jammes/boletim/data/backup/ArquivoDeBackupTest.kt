package app.jammes.boletim.data.backup

import app.jammes.boletim.data.local.entity.AlunoEntity
import app.jammes.boletim.data.local.entity.AnoLetivoEntity
import app.jammes.boletim.data.local.entity.AvaliacaoEntity
import app.jammes.boletim.data.local.entity.DisciplinaEntity
import app.jammes.boletim.data.local.entity.FaltaEntity
import app.jammes.boletim.data.local.entity.MateriaEntity
import app.jammes.boletim.data.local.entity.PeriodoEntity
import app.jammes.boletim.data.local.entity.RegraAvaliacaoEntity
import app.jammes.boletim.domain.model.BackupInvalidoException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier
import java.time.LocalDate
import kotlin.time.Instant

/** O formato do arquivo de backup: o que sai volta igual, e o que não serve é recusado com o motivo. */
class ArquivoDeBackupTest {

    // Ida e volta --------------------------------------------------------------------------

    @Test fun `o que sai no arquivo volta igual`() {
        // executa
        val lido = lerBackup(escreverBackup(COPIA, EXPORTADO_EM))

        // confere
        assertEquals(COPIA, lido.copia)
        assertEquals(EXPORTADO_EM, lido.exportadoEm)
    }

    @Test fun `o arquivo diz de que app e de que versao ele e`() {
        val texto = escreverBackup(COPIA, EXPORTADO_EM)

        assertTrue(texto.contains("\"app\": \"app.jammes.boletim\""))
        assertTrue(texto.contains("\"versao\": 1"))
    }

    @Test fun `as datas vao no formato ISO, e nao como o banco grava`() {
        val texto = escreverBackup(COPIA, EXPORTADO_EM)

        assertTrue(texto.contains("\"dataInicio\": \"2026-02-01\""))
        assertTrue(texto.contains("\"exportadoEm\": \"2026-10-15T13:45:00.123Z\""))
    }

    @Test fun `cada coluna do banco tem um campo no arquivo`() {
        // Uma coluna nova numa entidade, esquecida no formato, sairia do backup sem ninguém ver
        val pares = listOf(
            MateriaEntity::class.java to MateriaJson::class.java,
            AlunoEntity::class.java to AlunoJson::class.java,
            AnoLetivoEntity::class.java to AnoLetivoJson::class.java,
            PeriodoEntity::class.java to PeriodoJson::class.java,
            DisciplinaEntity::class.java to DisciplinaJson::class.java,
            RegraAvaliacaoEntity::class.java to RegraJson::class.java,
            AvaliacaoEntity::class.java to AvaliacaoJson::class.java,
            FaltaEntity::class.java to FaltaJson::class.java,
        )

        pares.forEach { (entidade, json) ->
            assertEquals("${entidade.simpleName} × ${json.simpleName}", campos(entidade), campos(json))
        }
    }

    @Test fun `o resumo tem os nomes na ordem do cadastro e as contagens`() {
        val copia = COPIA.copy(
            alunos = listOf(aluno(id = 2, nome = "Maria Clara"), aluno(id = 1, nome = "James Willian")),
        )

        val resumo = lerBackup(escreverBackup(copia, EXPORTADO_EM)).resumo()

        assertEquals(listOf("James Willian", "Maria Clara"), resumo.alunos)
        assertEquals(1, resumo.anosLetivos)
        assertEquals(2, resumo.avaliacoes)
        assertEquals(EXPORTADO_EM, resumo.exportadoEm)
    }

    // O que não é backup -------------------------------------------------------------------

    @Test fun `texto que nao e JSON nao e backup`() {
        assertEquals(NAO_E_BACKUP, motivo("Prova de matemática: 8,5"))
    }

    @Test fun `JSON que nao e um objeto nao e backup`() {
        assertEquals(NAO_E_BACKUP, motivo("[1, 2, 3]"))
    }

    @Test fun `JSON de outro app nao e backup`() {
        assertEquals(NAO_E_BACKUP, motivo("""{"app": "com.outro.app", "versao": 1}"""))
    }

    @Test fun `backup de uma versao mais nova pede para atualizar o app`() {
        val texto = escreverBackup(COPIA, EXPORTADO_EM).replace("\"versao\": 1", "\"versao\": 2")

        assertEquals(VERSAO_MAIS_NOVA, motivo(texto))
    }

    // Backup estragado ---------------------------------------------------------------------

    @Test fun `backup sem as tabelas foi alterado`() {
        assertEquals(BACKUP_ALTERADO, motivo("""{"app": "app.jammes.boletim", "versao": 1}"""))
    }

    @Test fun `backup sem a versao foi alterado`() {
        val texto = escreverBackup(COPIA, EXPORTADO_EM).replace("\"versao\": 1,", "")

        assertEquals(BACKUP_ALTERADO, motivo(texto))
    }

    @Test fun `data que nao se le e backup alterado`() {
        val texto = escreverBackup(COPIA, EXPORTADO_EM).replace("\"2026-02-01\"", "\"1º de fevereiro\"")

        assertEquals(BACKUP_ALTERADO, motivo(texto))
    }

    @Test fun `avaliacao de uma disciplina que nao esta no arquivo e backup alterado`() {
        val copia = COPIA.copy(avaliacoes = COPIA.avaliacoes + COPIA.avaliacoes[0].copy(id = 3, disciplinaId = 99))

        assertEquals(BACKUP_ALTERADO, motivo(escreverBackup(copia, EXPORTADO_EM)))
    }

    @Test fun `falta de um periodo que nao esta no arquivo e backup alterado`() {
        val copia = COPIA.copy(faltas = listOf(COPIA.faltas[0].copy(periodoId = 99)))

        assertEquals(BACKUP_ALTERADO, motivo(escreverBackup(copia, EXPORTADO_EM)))
    }

    @Test fun `id repetido numa tabela e backup alterado`() {
        val copia = COPIA.copy(alunos = COPIA.alunos + aluno(id = 1, nome = "Outro com o mesmo id"))

        assertEquals(BACKUP_ALTERADO, motivo(escreverBackup(copia, EXPORTADO_EM)))
    }

    @Test fun `sem nenhum ano letivo nao ha o que abrir`() {
        val soOAluno = COPIA.copy(
            anosLetivos = emptyList(),
            periodos = emptyList(),
            disciplinas = emptyList(),
            regras = emptyList(),
            avaliacoes = emptyList(),
            faltas = emptyList(),
        )

        assertEquals(SEM_ANO_LETIVO, motivo(escreverBackup(soOAluno, EXPORTADO_EM)))
    }

    // Peças --------------------------------------------------------------------------------

    /** A mensagem do [BackupInvalidoException] que ler o [texto] lança. */
    private fun motivo(texto: String): String? =
        assertThrows(BackupInvalidoException::class.java) { lerBackup(texto) }.message

    private fun aluno(id: Long, nome: String) =
        AlunoEntity(id = id, nome = nome, avatar = null, ativo = 1, criadoEm = CRIADO_EM)

    /** Os nomes das propriedades da classe (os campos estáticos são do serializador, não dela). */
    private fun campos(classe: Class<*>): Set<String> =
        classe.declaredFields.filterNot { Modifier.isStatic(it.modifiers) }.map { it.name }.toSet()

    private companion object {
        val EXPORTADO_EM = Instant.parse("2026-10-15T13:45:00.123Z")
        val CRIADO_EM = Instant.parse("2026-02-01T10:00:00Z")

        // Uma linha em cada tabela, com os campos opcionais preenchidos numa e vazios na outra
        val COPIA = CopiaDoBanco(
            materias = listOf(
                MateriaEntity(id = 1, nome = "Português", abreviacao = "POR", cor = 3, icone = "LIVRO"),
                MateriaEntity(id = 16, nome = "Robótica", abreviacao = null, cor = 0, icone = "CALCULADORA"),
            ),
            alunos = listOf(
                AlunoEntity(id = 1, nome = "James Willian", avatar = null, ativo = 1, criadoEm = CRIADO_EM),
            ),
            anosLetivos = listOf(
                AnoLetivoEntity(
                    id = 1, alunoId = 1, ano = 2026, serie = "1º Ano EM", tipoPeriodo = "BIMESTRE",
                    qtdPeriodos = 2, ativo = 1,
                ),
            ),
            periodos = listOf(
                PeriodoEntity(
                    id = 1, anoLetivoId = 1, periodo = 1,
                    dataInicio = LocalDate.of(2026, 2, 1), dataFim = LocalDate.of(2026, 6, 30),
                ),
                PeriodoEntity(
                    id = 2, anoLetivoId = 1, periodo = 2,
                    dataInicio = LocalDate.of(2026, 8, 1), dataFim = LocalDate.of(2026, 12, 15),
                ),
            ),
            disciplinas = listOf(
                DisciplinaEntity(
                    id = 1, materiaId = 1, anoLetivoId = 1, nome = "Português", abreviacao = "POR", cor = 3,
                    icone = "LIVRO", totalAulas = 80, professor = "Prof. Maria", periodoInicio = null,
                    periodoFim = null, ativa = 1, ordem = 1,
                ),
                DisciplinaEntity(
                    id = 2, materiaId = 16, anoLetivoId = 1, nome = "Robótica", abreviacao = null, cor = 0,
                    icone = "CALCULADORA", totalAulas = null, professor = null,
                    periodoInicio = LocalDate.of(2026, 8, 1), periodoFim = LocalDate.of(2026, 12, 15),
                    ativa = 0, ordem = 2,
                ),
            ),
            regras = listOf(
                RegraAvaliacaoEntity(
                    id = 1, anoLetivoId = 1, disciplinaId = null, mediaMinima = 6.0, mediaRecuperacao = 5.0,
                    frequenciaMinima = 75.0, tipoMedia = "PONDERADA", arredondamento = "NENHUM", descartarMenorNota = 0,
                ),
                RegraAvaliacaoEntity(
                    id = 2, anoLetivoId = 1, disciplinaId = 2, mediaMinima = 7.0, mediaRecuperacao = null,
                    frequenciaMinima = 80.0, tipoMedia = "SIMPLES", arredondamento = "MEIO_PONTO", descartarMenorNota = 1,
                ),
            ),
            avaliacoes = listOf(
                AvaliacaoEntity(
                    id = 1, disciplinaId = 1, periodoId = 1, nome = "Prova 1", nota = 8.5, notaMaxima = 10.0,
                    peso = 2.0, tipo = "NORMAL", data = LocalDate.of(2026, 3, 10), criadoEm = CRIADO_EM,
                ),
                AvaliacaoEntity(
                    id = 2, disciplinaId = 1, periodoId = null, nome = "Prova final", nota = null, notaMaxima = 10.0,
                    peso = 1.0, tipo = "FINAL", data = null, criadoEm = CRIADO_EM,
                ),
            ),
            faltas = listOf(
                FaltaEntity(id = 1, disciplinaId = 2, periodoId = 2, data = LocalDate.of(2026, 9, 3), qtdAulas = 2),
            ),
        )
    }
}

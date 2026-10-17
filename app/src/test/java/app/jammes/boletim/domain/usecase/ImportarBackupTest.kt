package app.jammes.boletim.domain.usecase

import app.jammes.boletim.domain.model.AlunoDomain
import app.jammes.boletim.domain.model.AnoLetivoDomain
import app.jammes.boletim.domain.model.Contexto
import app.jammes.boletim.domain.model.PeriodoDomain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import kotlin.time.Instant

/** Onde o app abre depois de importar um backup. */
class ImportarBackupTest {

    @Test fun `o aluno e o ano abertos continuam quando o arquivo tem eles`() {
        // prepara: um backup deste mesmo aparelho, com a Maria aberta
        val anos = listOf(ano(id = 10, alunoId = JAMES.id, ano = 2026), ano(id = 20, alunoId = MARIA.id, ano = 2025))

        // executa
        val contexto = contextoDepoisDeImportar(Contexto(MARIA.id, 20, periodoId = null), listOf(JAMES, MARIA), anos, HOJE)

        // confere
        assertEquals(MARIA.id, contexto?.alunoId)
        assertEquals(20L, contexto?.anoLetivoId)
    }

    @Test fun `o periodo aberto continua, mesmo nao sendo o de hoje`() {
        val anos = listOf(ano(id = 10, alunoId = JAMES.id, ano = 2026))

        // Estava no 1º período; hoje (15/10) é o 2º
        val contexto = contextoDepoisDeImportar(Contexto(JAMES.id, 10, periodoId = 101), listOf(JAMES), anos, HOJE)

        assertEquals(101L, contexto?.periodoId)
    }

    @Test fun `o ano inteiro aberto continua`() {
        val anos = listOf(ano(id = 10, alunoId = JAMES.id, ano = 2026))

        val contexto = contextoDepoisDeImportar(Contexto(JAMES.id, 10, periodoId = null), listOf(JAMES), anos, HOJE)

        assertEquals(Contexto(JAMES.id, anoLetivoId = 10, periodoId = null), contexto)
    }

    @Test fun `periodo aberto que nao esta mais no ano vira o de hoje`() {
        val anos = listOf(ano(id = 10, alunoId = JAMES.id, ano = 2026))

        val contexto = contextoDepoisDeImportar(Contexto(JAMES.id, 10, periodoId = 999), listOf(JAMES), anos, HOJE)

        assertEquals(102L, contexto?.periodoId)
    }

    @Test fun `abrindo outro aluno, num ano que ja acabou fica o ano inteiro`() {
        val anos = listOf(ano(id = 10, alunoId = JAMES.id, ano = 2024))

        val contexto = contextoDepoisDeImportar(null, listOf(JAMES), anos, HOJE)

        assertNull(contexto?.periodoId)
        assertEquals(10L, contexto?.anoLetivoId)
    }

    @Test fun `sem o aberto no arquivo abre o primeiro aluno com ano letivo, no ano mais recente`() {
        val anos = listOf(
            ano(id = 1, alunoId = JAMES.id, ano = 2025),
            ano(id = 2, alunoId = JAMES.id, ano = 2026),
            ano(id = 3, alunoId = MARIA.id, ano = 2026),
        )

        val contexto = contextoDepoisDeImportar(Contexto(alunoId = 7, anoLetivoId = 70, periodoId = null), listOf(MARIA, JAMES), anos, HOJE)

        assertEquals(Contexto(JAMES.id, anoLetivoId = 2, periodoId = 22), contexto)
    }

    @Test fun `sem nada aberto tambem abre o primeiro aluno`() {
        val anos = listOf(ano(id = 3, alunoId = MARIA.id, ano = 2026))

        val contexto = contextoDepoisDeImportar(null, listOf(JAMES, MARIA), anos, HOJE)

        assertEquals(MARIA.id, contexto?.alunoId) // o James não tem ano letivo: fica para trás
    }

    @Test fun `ano aberto que no arquivo e de outro aluno nao vale`() {
        // O id 10 era do James neste aparelho; no arquivo, é um ano da Maria
        val anos = listOf(ano(id = 10, alunoId = MARIA.id, ano = 2026), ano(id = 11, alunoId = JAMES.id, ano = 2026))

        val contexto = contextoDepoisDeImportar(Contexto(JAMES.id, 10, periodoId = null), listOf(JAMES, MARIA), anos, HOJE)

        assertEquals(Contexto(JAMES.id, anoLetivoId = 11, periodoId = 112), contexto)
    }

    @Test fun `sem nenhum ano letivo nao ha o que abrir`() {
        assertNull(contextoDepoisDeImportar(null, listOf(JAMES, MARIA), emptyList(), HOJE))
    }

    private companion object {
        val HOJE: LocalDate = LocalDate.of(2026, 10, 15)
        val CRIADO_EM = Instant.parse("2026-02-01T10:00:00Z")
        val JAMES = AlunoDomain(id = 1, nome = "James Willian", criadoEm = CRIADO_EM)
        val MARIA = AlunoDomain(id = 2, nome = "Maria Clara", criadoEm = CRIADO_EM)

        /** Dois semestres; os ids dos períodos são o do ano com o número no fim: 101 e 102 no ano 10. */
        fun ano(id: Long, alunoId: Long, ano: Int) = AnoLetivoDomain(
            id = id,
            ano = ano,
            serie = null,
            periodo = listOf(
                PeriodoDomain(id * 10 + 1, id, 1, LocalDate.of(ano, 2, 1), LocalDate.of(ano, 7, 15)),
                PeriodoDomain(id * 10 + 2, id, 2, LocalDate.of(ano, 8, 1), LocalDate.of(ano, 12, 15)),
            ),
            qtdPeriodos = 2,
            alunoId = alunoId,
        )
    }
}

package app.jammes.boletim.presentation.ui.disciplina

import app.jammes.boletim.domain.model.AvaliacaoDomain
import app.jammes.boletim.domain.model.RegraAvaliacaoDomain
import app.jammes.boletim.domain.model.TipoAvaliacao
import app.jammes.boletim.domain.model.TipoMedia
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Instant

/** Qual avaliação gravada deixa a pessoa num bom momento para o pedido de avaliação na loja. */
class DisciplinaViewModelTest {

    @Test fun `nota com folga ou perto da minima e hora boa`() {
        assertTrue(horaDePedirAvaliacaoNaLoja(avaliacao(nota = 9.5), SIMPLES))
        assertTrue(horaDePedirAvaliacaoNaLoja(avaliacao(nota = 7.0), SIMPLES))
    }

    @Test fun `nota abaixo da minima espera a proxima`() {
        assertFalse(horaDePedirAvaliacaoNaLoja(avaliacao(nota = 6.9), SIMPLES))
        assertFalse(horaDePedirAvaliacaoNaLoja(avaliacao(nota = 0.0), SIMPLES))
    }

    @Test fun `agendada nao tem nota, entao nao e hora`() {
        assertFalse(horaDePedirAvaliacaoNaLoja(avaliacao(nota = null), SIMPLES))
    }

    @Test fun `a final nunca e hora, porque a tela ja pergunta se a disciplina acabou`() {
        assertFalse(horaDePedirAvaliacaoNaLoja(avaliacao(nota = 10.0, tipo = TipoAvaliacao.FINAL), SIMPLES))
    }

    @Test fun `a recuperacao vale pela propria nota`() {
        assertTrue(horaDePedirAvaliacaoNaLoja(avaliacao(nota = 8.0, tipo = TipoAvaliacao.RECUPERACAO), SIMPLES))
        assertFalse(horaDePedirAvaliacaoNaLoja(avaliacao(nota = 4.0, tipo = TipoAvaliacao.RECUPERACAO), SIMPLES))
    }

    @Test fun `a nota e comparada na escala de 10`() {
        assertTrue(horaDePedirAvaliacaoNaLoja(avaliacao(nota = 4.0, notaMaxima = 5.0), SIMPLES))
        assertFalse(horaDePedirAvaliacaoNaLoja(avaliacao(nota = 3.0, notaMaxima = 5.0), SIMPLES))
    }

    @Test fun `na soma, uma nota normal e so uma parte do total e nao segura o pedido`() {
        assertTrue(horaDePedirAvaliacaoNaLoja(avaliacao(nota = 1.0, notaMaxima = 3.0), SOMA))
    }

    private fun avaliacao(nota: Double?, notaMaxima: Double = 10.0, tipo: TipoAvaliacao = TipoAvaliacao.NORMAL) =
        AvaliacaoDomain(
            disciplinaId = 1,
            periodoId = 1,
            nome = "Prova",
            nota = nota,
            notaMaxima = notaMaxima,
            peso = 1.0,
            tipo = tipo,
            data = null,
            criadoEm = Instant.fromEpochMilliseconds(0),
        )

    private companion object {
        val SIMPLES = RegraAvaliacaoDomain(
            anoLetivoId = 1,
            disciplinaId = null,
            mediaMinima = 7.0,
            mediaRecuperacao = 5.0,
            frequenciaMinima = 75.0,
            tipoMedia = TipoMedia.SIMPLES,
        )
        val SOMA = SIMPLES.copy(tipoMedia = TipoMedia.SOMA)
    }
}

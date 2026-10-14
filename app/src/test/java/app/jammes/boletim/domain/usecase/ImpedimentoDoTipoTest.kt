package app.jammes.boletim.domain.usecase

import app.jammes.boletim.domain.model.AvaliacaoDomain
import app.jammes.boletim.domain.model.Impedimento
import app.jammes.boletim.domain.model.RegraAvaliacaoDomain
import app.jammes.boletim.domain.model.TipoAvaliacao
import app.jammes.boletim.domain.model.TipoMedia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Instant

class ImpedimentoDoTipoTest {

    private val regra = RegraAvaliacaoDomain(
        anoLetivoId = 1, disciplinaId = null, mediaMinima = 7.0, mediaRecuperacao = null,
        frequenciaMinima = 75.0, tipoMedia = TipoMedia.SIMPLES,
    )
    private val nova = avaliacao(id = 0, periodoId = 1, nota = null) // o formulário de uma nova

    @Test fun `recuperacao so cabe com a media do periodo abaixo da minima`() {
        val lancadas = listOf(
            avaliacao(id = 1, periodoId = 1, nota = 5.0),
            avaliacao(id = 2, periodoId = 1, nota = 6.0), // 1º: 5,5
            avaliacao(id = 3, periodoId = 2, nota = 8.0), // 2º: 8,0
        )

        assertNull(lancadas.impedimentoDoTipo(TipoAvaliacao.RECUPERACAO, periodoId = 1, nova, regra))
        assertEquals(
            Impedimento.MediaSuficiente(media = 8.0, minima = 7.0),
            lancadas.impedimentoDoTipo(TipoAvaliacao.RECUPERACAO, periodoId = 2, nova, regra),
        )
    }

    @Test fun `media exatamente na minima nao pede recuperacao`() {
        val lancadas = listOf(avaliacao(id = 1, periodoId = 1, nota = 7.0))

        assertEquals(
            Impedimento.MediaSuficiente(media = 7.0, minima = 7.0),
            lancadas.impedimentoDoTipo(TipoAvaliacao.RECUPERACAO, periodoId = 1, nova, regra),
        )
    }

    @Test fun `sem nota no periodo ainda nao da para saber`() {
        val lancadas = listOf(avaliacao(id = 1, periodoId = 1, nota = null)) // agendada

        assertEquals(Impedimento.SemMedia, lancadas.impedimentoDoTipo(TipoAvaliacao.RECUPERACAO, periodoId = 1, nova, regra))
        assertEquals(Impedimento.SemMedia, lancadas.impedimentoDoTipo(TipoAvaliacao.FINAL, periodoId = 1, nova, regra))
    }

    @Test fun `final so cabe com a media do ano abaixo da minima`() {
        val abaixo = listOf(avaliacao(id = 1, periodoId = 1, nota = 5.5), avaliacao(id = 2, periodoId = 2, nota = 8.0))
        val acima = listOf(avaliacao(id = 1, periodoId = 1, nota = 8.0), avaliacao(id = 2, periodoId = 2, nota = 9.0))

        assertNull(abaixo.impedimentoDoTipo(TipoAvaliacao.FINAL, periodoId = 1, nova, regra)) // ano: 6,75
        assertEquals(
            Impedimento.MediaSuficiente(media = 8.5, minima = 7.0),
            acima.impedimentoDoTipo(TipoAvaliacao.FINAL, periodoId = 1, nova, regra),
        )
    }

    @Test fun `a recuperacao de um periodo entra na media do ano que decide a final`() {
        val lancadas = listOf(
            avaliacao(id = 1, periodoId = 1, nota = 5.0),
            avaliacao(id = 2, periodoId = 1, nota = 9.0, tipo = TipoAvaliacao.RECUPERACAO), // 1º: 9,0
            avaliacao(id = 3, periodoId = 2, nota = 7.0), // 2º: 7,0
        )

        assertEquals(
            Impedimento.MediaSuficiente(media = 8.0, minima = 7.0), // e não 6,0, sem a recuperação
            lancadas.impedimentoDoTipo(TipoAvaliacao.FINAL, periodoId = 2, nova, regra),
        )
    }

    @Test fun `a vaga ocupada vem antes da media`() {
        val recuperacao = avaliacao(id = 2, periodoId = 1, nota = null, tipo = TipoAvaliacao.RECUPERACAO)
        val lancadas = listOf(avaliacao(id = 1, periodoId = 1, nota = 5.0), recuperacao)

        assertEquals(
            Impedimento.VagaOcupada(recuperacao),
            lancadas.impedimentoDoTipo(TipoAvaliacao.RECUPERACAO, periodoId = 1, nova, regra),
        )
    }

    @Test fun `a normal que vira recuperacao nao conta na media que ela substituiria`() {
        val unica = avaliacao(id = 1, periodoId = 1, nota = 5.0)

        assertEquals(Impedimento.SemMedia, listOf(unica).impedimentoDoTipo(TipoAvaliacao.RECUPERACAO, periodoId = 1, unica, regra))
    }

    @Test fun `a recuperacao ja gravada continua podendo ser editada, mesmo com a media acima`() {
        val recuperacao = avaliacao(id = 2, periodoId = 1, nota = 9.0, tipo = TipoAvaliacao.RECUPERACAO)
        val lancadas = listOf(avaliacao(id = 1, periodoId = 1, nota = 8.0), recuperacao) // a nota subiu depois

        assertNull(lancadas.impedimentoDoTipo(TipoAvaliacao.RECUPERACAO, periodoId = 1, recuperacao, regra))
        // Mudando de período, ela é vista de novo, como uma nova ali
        assertEquals(Impedimento.SemMedia, lancadas.impedimentoDoTipo(TipoAvaliacao.RECUPERACAO, periodoId = 2, recuperacao, regra))
    }

    @Test fun `normal sempre pode`() {
        assertNull(emptyList<AvaliacaoDomain>().impedimentoDoTipo(TipoAvaliacao.NORMAL, periodoId = 1, nova, regra))
    }
}

private fun avaliacao(
    id: Long,
    periodoId: Long?,
    nota: Double?,
    tipo: TipoAvaliacao = TipoAvaliacao.NORMAL,
) = AvaliacaoDomain(
    id = id,
    disciplinaId = 1,
    periodoId = periodoId,
    nome = "Prova $id",
    nota = nota,
    notaMaxima = 10.0,
    peso = 1.0,
    tipo = tipo,
    data = null,
    criadoEm = Instant.fromEpochMilliseconds(0),
)

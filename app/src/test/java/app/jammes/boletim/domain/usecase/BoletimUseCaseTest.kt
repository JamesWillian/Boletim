package app.jammes.boletim.domain.usecase

import app.jammes.boletim.domain.model.AvaliacaoDomain
import app.jammes.boletim.domain.model.RegraAvaliacaoDomain
import app.jammes.boletim.domain.model.StatusDisciplina
import app.jammes.boletim.domain.model.TipoArredondamento
import app.jammes.boletim.domain.model.TipoAvaliacao
import app.jammes.boletim.domain.model.TipoMedia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Instant

class BoletimUseCaseTest {

    @Test fun `media simples ignora o peso`() {
        // prepara
        val avaliacoes = listOf(avaliacao(nota = 6.0, peso = 1.0), avaliacao(nota = 8.0, peso = 3.0))

        // executa
        val media = CalcularMediaDisciplina(avaliacoes, regra(TipoMedia.SIMPLES))

        // confere
        assertEquals(7.0, media!!, 0.001)
    }

    @Test fun `media ponderada usa o peso de cada avaliacao`() {
        val avaliacoes = listOf(avaliacao(nota = 6.0, peso = 1.0), avaliacao(nota = 8.0, peso = 3.0))

        val media = CalcularMediaDisciplina(avaliacoes, regra(TipoMedia.PONDERADA))

        assertEquals(7.5, media!!, 0.001) // (6×1 + 8×3) / 4
    }

    @Test fun `sem avaliacoes a media fica nula`() {
        assertNull(CalcularMediaDisciplina(emptyList(), regra()))
    }

    @Test fun `avaliacao sem nota fica fora da media`() {
        val avaliacoes = listOf(
            avaliacao(nota = 6.0),
            avaliacao(nota = 8.0),
            avaliacao(nota = null), // prova marcada, ainda não aconteceu
        )

        val media = CalcularMediaDisciplina(avaliacoes, regra(TipoMedia.SIMPLES))

        assertEquals(7.0, media!!, 0.001) // e não 4,67, como seria contando a vazia como zero
    }

    @Test fun `so avaliacoes sem nota deixam a media nula`() {
        val avaliacoes = listOf(avaliacao(nota = null), avaliacao(nota = null))

        assertNull(CalcularMediaDisciplina(avaliacoes, regra(TipoMedia.PONDERADA)))
    }

    @Test fun `media abaixo da minima fica ABAIXO`() {
        assertEquals(StatusDisciplina.ABAIXO, statusDaMedia(6.9, regra(mediaMinima = 7.0)))
    }

    @Test fun `media exatamente na minima nao fica ABAIXO`() {
        val avaliacoes = listOf(
            avaliacao(nota = 6.7, peso = 1.0), // trabalho
            avaliacao(nota = 7.1, peso = 3.0), // prova
        )
        val regra = regra(TipoMedia.PONDERADA)

        val media = CalcularMediaDisciplina(avaliacoes, regra) // (6,7×1 + 7,1×3) / 4 = 7,0

        assertEquals(StatusDisciplina.ATENCAO, statusDaMedia(media, regra))
    }

    @Test fun `75 por cento de frequencia em 40 aulas permite 10 faltas`() {
        assertEquals(10, CalcularFrequencia.limiteFaltas(totalAulas = 40, regra(frequenciaMinima = 75.0)))
    }

    // Média anual ---------------------------------------------------------------------------

    @Test fun `media anual e a media das medias dos periodos`() {
        val avaliacoes = listOf(
            avaliacao(nota = 5.0, periodoId = 1),
            avaliacao(nota = 7.0, periodoId = 1), // 1º período: 6,0
            avaliacao(nota = 8.0, periodoId = 2), // 2º período: 8,0
        )

        val media = CalcularMediaAnual(avaliacoes, regra(TipoMedia.SIMPLES))

        assertEquals(7.0, media!!, 0.001) // e não 6,67, como seria juntando as três notas
    }

    @Test fun `periodo sem nota fica fora da media anual`() {
        val avaliacoes = listOf(
            avaliacao(nota = 6.0, periodoId = 1),
            avaliacao(nota = null, periodoId = 2), // 2º período ainda não teve prova
        )

        val media = CalcularMediaAnual(avaliacoes, regra(TipoMedia.SIMPLES))

        assertEquals(6.0, media!!, 0.001) // e não 3,0, como seria contando o 2º como zero
    }

    @Test fun `sem nota em nenhum periodo a media anual fica nula`() {
        val avaliacoes = listOf(avaliacao(nota = null, periodoId = 1), avaliacao(nota = null, periodoId = 2))

        assertNull(CalcularMediaAnual(avaliacoes, regra()))
    }

    @Test fun `na soma a media anual e a media das somas de cada periodo`() {
        val avaliacoes = listOf(
            avaliacao(nota = 6.0, periodoId = 1),
            avaliacao(nota = 4.0, periodoId = 1), // 1º período: 10
            avaliacao(nota = 3.0, periodoId = 2),
            avaliacao(nota = 5.0, periodoId = 2), // 2º período: 8
        )

        val media = CalcularMediaAnual(avaliacoes, regra(TipoMedia.SOMA))

        assertEquals(9.0, media!!, 0.001) // e não 18, a soma do ano todo
    }

    @Test fun `media anual passa pelo arredondamento da regra`() {
        val avaliacoes = listOf(avaliacao(nota = 6.5, periodoId = 1), avaliacao(nota = 7.0, periodoId = 2))
        val regra = regra(TipoMedia.SIMPLES, arredondamento = TipoArredondamento.MEIO_PONTO)

        val media = CalcularMediaAnual(avaliacoes, regra) // (6,5 + 7,0) / 2 = 6,75

        assertEquals(7.0, media!!, 0.001)
        assertEquals(StatusDisciplina.ATENCAO, statusDaMedia(media, regra))
    }

    @Test fun `com periodo escolhido a media usa so as avaliacoes dele`() {
        val avaliacoes = listOf(avaliacao(nota = 6.0, periodoId = 1), avaliacao(nota = 8.0, periodoId = 2))

        assertEquals(8.0, calcularMediaNoFiltro(avaliacoes, regra(), periodoId = 2)!!, 0.001)
        assertEquals(7.0, calcularMediaNoFiltro(avaliacoes, regra(), periodoId = null)!!, 0.001)
    }

    // Nota máxima ---------------------------------------------------------------------------

    @Test fun `nota vai para a escala de 10 pela nota maxima`() {
        assertEquals(8.0, notaNaEscalaDe10(4.0, notaMaxima = 5.0)!!, 0.0)
        assertEquals(8.5, notaNaEscalaDe10(85.0, notaMaxima = 100.0)!!, 0.0)
        assertEquals(5.7, notaNaEscalaDe10(57.0, notaMaxima = 100.0)!!, 0.0) // exata, e não 5,699…
        assertNull(notaNaEscalaDe10(4.0, notaMaxima = 0.0))
    }

    @Test fun `media simples usa as notas na escala de 10`() {
        val avaliacoes = listOf(avaliacao(nota = 8.0), avaliacao(nota = 4.0, notaMaxima = 5.0))

        val media = CalcularMediaDisciplina(avaliacoes, regra(TipoMedia.SIMPLES))

        assertEquals(8.0, media!!, 0.001) // 4 de 5 conta como 8; antes saía 6,0
    }

    @Test fun `media ponderada converte a nota antes de aplicar o peso`() {
        val avaliacoes = listOf(
            avaliacao(nota = 4.0, notaMaxima = 5.0, peso = 1.0), // 8 na escala de 10
            avaliacao(nota = 6.0, peso = 3.0),
        )

        val media = CalcularMediaDisciplina(avaliacoes, regra(TipoMedia.PONDERADA))

        assertEquals(6.5, media!!, 0.001) // (8×1 + 6×3) / 4
    }

    @Test fun `na soma a nota maxima nao muda os pontos`() {
        val avaliacoes = listOf(avaliacao(nota = 4.0, notaMaxima = 5.0), avaliacao(nota = 3.0, notaMaxima = 5.0))

        val media = CalcularMediaDisciplina(avaliacoes, regra(TipoMedia.SOMA))

        assertEquals(7.0, media!!, 0.001) // 4 + 3, e não 8 + 6
    }

    @Test fun `nota exatamente na minima pela escala de 10 nao fica ABAIXO`() {
        val regra = regra(TipoMedia.SIMPLES, mediaMinima = 7.0)

        val media = CalcularMediaDisciplina(listOf(avaliacao(nota = 3.5, notaMaxima = 5.0)), regra)

        assertEquals(StatusDisciplina.ATENCAO, statusDaMedia(media, regra)) // 3,5 de 5 = 7,0
    }

    @Test fun `avaliacao sem nota maxima valida fica fora da media`() {
        val avaliacoes = listOf(avaliacao(nota = 8.0), avaliacao(nota = 5.0, notaMaxima = 0.0))

        assertEquals(8.0, CalcularMediaDisciplina(avaliacoes, regra(TipoMedia.SIMPLES))!!, 0.001)
    }

    @Test fun `media anual usa as notas na escala de 10`() {
        val avaliacoes = listOf(
            avaliacao(nota = 4.0, notaMaxima = 5.0, periodoId = 1), // 1º período: 8,0
            avaliacao(nota = 9.0, periodoId = 2), // 2º período: 9,0
        )

        val media = CalcularMediaAnual(avaliacoes, regra(TipoMedia.SIMPLES))

        assertEquals(8.5, media!!, 0.001)
    }
}

// Preenche os campos que o cálculo não usa, pra cada teste mostrar só o que importa.
private fun avaliacao(
    nota: Double?,
    notaMaxima: Double = 10.0,
    peso: Double = 1.0,
    tipo: TipoAvaliacao = TipoAvaliacao.NORMAL,
    periodoId: Long = 1,
) = AvaliacaoDomain(
    disciplinaId = 1,
    periodoId = periodoId,
    nome = "Prova",
    nota = nota,
    notaMaxima = notaMaxima,
    peso = peso,
    tipo = tipo,
    data = null,
    criadoEm = Instant.fromEpochMilliseconds(0),
)

private fun regra(
    tipoMedia: TipoMedia = TipoMedia.SIMPLES,
    mediaMinima: Double = 7.0,
    frequenciaMinima: Double = 75.0,
    arredondamento: TipoArredondamento = TipoArredondamento.NENHUM,
) = RegraAvaliacaoDomain(
    anoLetivoId = 1,
    disciplinaId = null,
    mediaMinima = mediaMinima,
    mediaRecuperacao = 5.0,
    frequenciaMinima = frequenciaMinima,
    tipoMedia = tipoMedia,
    arredondamento = TipoArredondamento.MEIO_PONTO
)

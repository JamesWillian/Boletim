package app.jammes.boletim.domain.usecase

import app.jammes.boletim.domain.model.AvaliacaoDomain
import app.jammes.boletim.domain.model.RegraAvaliacaoDomain
import app.jammes.boletim.domain.model.StatusDisciplina
import app.jammes.boletim.domain.model.Substituta
import app.jammes.boletim.domain.model.TipoArredondamento
import app.jammes.boletim.domain.model.TipoAvaliacao
import app.jammes.boletim.domain.model.TipoMedia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

        val media = CalcularMediaAnual(avaliacoes, regra(TipoMedia.SIMPLES)).media

        assertEquals(7.0, media!!, 0.001) // e não 6,67, como seria juntando as três notas
    }

    @Test fun `periodo sem nota fica fora da media anual`() {
        val avaliacoes = listOf(
            avaliacao(nota = 6.0, periodoId = 1),
            avaliacao(nota = null, periodoId = 2), // 2º período ainda não teve prova
        )

        val media = CalcularMediaAnual(avaliacoes, regra(TipoMedia.SIMPLES)).media

        assertEquals(6.0, media!!, 0.001) // e não 3,0, como seria contando o 2º como zero
    }

    @Test fun `sem nota em nenhum periodo a media anual fica nula`() {
        val avaliacoes = listOf(avaliacao(nota = null, periodoId = 1), avaliacao(nota = null, periodoId = 2))

        assertNull(CalcularMediaAnual(avaliacoes, regra()).media)
    }

    @Test fun `na soma a media anual e a media das somas de cada periodo`() {
        val avaliacoes = listOf(
            avaliacao(nota = 6.0, periodoId = 1),
            avaliacao(nota = 4.0, periodoId = 1), // 1º período: 10
            avaliacao(nota = 3.0, periodoId = 2),
            avaliacao(nota = 5.0, periodoId = 2), // 2º período: 8
        )

        val media = CalcularMediaAnual(avaliacoes, regra(TipoMedia.SOMA)).media

        assertEquals(9.0, media!!, 0.001) // e não 18, a soma do ano todo
    }

    @Test fun `media anual passa pelo arredondamento da regra`() {
        val avaliacoes = listOf(avaliacao(nota = 6.5, periodoId = 1), avaliacao(nota = 7.0, periodoId = 2))
        val regra = regra(TipoMedia.SIMPLES, arredondamento = TipoArredondamento.MEIO_PONTO)

        val media = CalcularMediaAnual(avaliacoes, regra).media // (6,5 + 7,0) / 2 = 6,75

        assertEquals(7.0, media!!, 0.001)
        assertEquals(StatusDisciplina.ATENCAO, statusDaMedia(media, regra))
    }

    @Test fun `com periodo escolhido a media usa so as avaliacoes dele`() {
        val avaliacoes = listOf(avaliacao(nota = 6.0, periodoId = 1), avaliacao(nota = 8.0, periodoId = 2))

        assertEquals(8.0, calcularMediaNoFiltro(avaliacoes, regra(), periodoId = 2).media!!, 0.001)
        assertEquals(7.0, calcularMediaNoFiltro(avaliacoes, regra(), periodoId = null).media!!, 0.001)
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

        val media = CalcularMediaAnual(avaliacoes, regra(TipoMedia.SIMPLES)).media

        assertEquals(8.5, media!!, 0.001)
    }

    // Recuperação e final --------------------------------------------------------------------
    // Os valores caem em meio ponto: passam com ou sem o arredondamento que o regra() força.

    @Test fun `recuperacao maior substitui a media do periodo`() {
        val avaliacoes = listOf(
            avaliacao(nota = 5.0),
            avaliacao(nota = 6.0), // média do período: 5,5
            avaliacao(nota = 7.0, tipo = TipoAvaliacao.RECUPERACAO),
        )

        val media = CalcularMediaDoPeriodo(avaliacoes, regra(TipoMedia.SIMPLES))

        assertEquals(7.0, media.media!!, 0.001)
        assertEquals(5.5, media.regular!!, 0.001)
        assertEquals(Substituta(TipoAvaliacao.RECUPERACAO, nota = 7.0, substituiu = true), media.substituta)
    }

    @Test fun `recuperacao menor nao muda a media do periodo`() {
        val avaliacoes = listOf(avaliacao(nota = 6.0), avaliacao(nota = 5.0, tipo = TipoAvaliacao.RECUPERACAO))

        val media = CalcularMediaDoPeriodo(avaliacoes, regra(TipoMedia.SIMPLES))

        assertEquals(6.0, media.media!!, 0.001)
        assertEquals(Substituta(TipoAvaliacao.RECUPERACAO, nota = 5.0, substituiu = false), media.substituta)
    }

    @Test fun `recuperacao agendada ainda nao conta`() {
        val avaliacoes = listOf(avaliacao(nota = 5.0), avaliacao(nota = null, tipo = TipoAvaliacao.RECUPERACAO))

        val media = CalcularMediaDoPeriodo(avaliacoes, regra(TipoMedia.SIMPLES))

        assertEquals(5.0, media.media!!, 0.001)
        assertNull(media.substituta)
    }

    @Test fun `recuperacao vale pela nota na escala de 10, mesmo na soma`() {
        val avaliacoes = listOf(
            avaliacao(nota = 3.0, notaMaxima = 6.0),
            avaliacao(nota = 2.0, notaMaxima = 4.0), // soma do período: 5
            avaliacao(nota = 4.0, notaMaxima = 5.0, tipo = TipoAvaliacao.RECUPERACAO), // 8 de 10
        )

        assertEquals(8.0, CalcularMediaDoPeriodo(avaliacoes, regra(TipoMedia.SOMA)).media!!, 0.001)
    }

    @Test fun `recuperacao passa pelo arredondamento da regra`() {
        val regra = regra(TipoMedia.SIMPLES, arredondamento = TipoArredondamento.MEIO_PONTO)
        val avaliacoes = listOf(avaliacao(nota = 5.0), avaliacao(nota = 6.8, tipo = TipoAvaliacao.RECUPERACAO))

        assertEquals(7.0, CalcularMediaDoPeriodo(avaliacoes, regra).media!!, 0.001) // 6,8 vira 7,0
    }

    @Test fun `sem media regular a recuperacao e a media`() {
        val media = CalcularMediaDoPeriodo(listOf(avaliacao(nota = 7.0, tipo = TipoAvaliacao.RECUPERACAO)), regra())

        assertEquals(7.0, media.media!!, 0.001)
        assertNull(media.regular)
        assertTrue(media.substituta!!.substituiu)
    }

    @Test fun `a recuperacao de um periodo nao mexe nos outros`() {
        val avaliacoes = listOf(
            avaliacao(nota = 5.0, periodoId = 1),
            avaliacao(nota = 7.0, periodoId = 1, tipo = TipoAvaliacao.RECUPERACAO), // 1º período: 7,0
            avaliacao(nota = 8.0, periodoId = 2), // 2º período: 8,0
        )

        assertEquals(8.0, calcularMediaNoFiltro(avaliacoes, regra(), periodoId = 2).media!!, 0.001)
        assertEquals(7.5, calcularMediaNoFiltro(avaliacoes, regra(), periodoId = null).media!!, 0.001) // (7 + 8) / 2
    }

    @Test fun `final maior substitui a media do ano`() {
        val avaliacoes = listOf(
            avaliacao(nota = 5.0, periodoId = 1),
            avaliacao(nota = 6.0, periodoId = 2), // ano: 5,5
            avaliacao(nota = 7.0, periodoId = null, tipo = TipoAvaliacao.FINAL),
        )

        val media = CalcularMediaAnual(avaliacoes, regra(TipoMedia.SIMPLES))

        assertEquals(7.0, media.media!!, 0.001)
        assertEquals(5.5, media.regular!!, 0.001)
        assertEquals(Substituta(TipoAvaliacao.FINAL, nota = 7.0, substituiu = true), media.substituta)
    }

    @Test fun `final menor nao muda a media do ano`() {
        val avaliacoes = listOf(
            avaliacao(nota = 8.0, periodoId = 1),
            avaliacao(nota = 6.0, periodoId = null, tipo = TipoAvaliacao.FINAL),
        )

        val media = CalcularMediaAnual(avaliacoes, regra(TipoMedia.SIMPLES))

        assertEquals(8.0, media.media!!, 0.001)
        assertFalse(media.substituta!!.substituiu)
    }

    @Test fun `a final nao entra em nenhum periodo, nem quando ficou com um`() {
        val avaliacoes = listOf(
            avaliacao(nota = 6.0, periodoId = 2),
            avaliacao(nota = 9.0, periodoId = 2, tipo = TipoAvaliacao.FINAL), // gravada antes, com período
        )

        val doPeriodo = calcularMediaNoFiltro(avaliacoes, regra(), periodoId = 2)

        assertEquals(6.0, doPeriodo.media!!, 0.001)
        assertNull(doPeriodo.substituta)
        assertEquals(9.0, calcularMediaNoFiltro(avaliacoes, regra(), periodoId = null).media!!, 0.001) // no ano, sim
    }

    @Test fun `no ano a recuperacao de cada periodo entra antes da final`() {
        val avaliacoes = listOf(
            avaliacao(nota = 4.0, periodoId = 1),
            avaliacao(nota = 6.0, periodoId = 1, tipo = TipoAvaliacao.RECUPERACAO), // 1º: 6,0
            avaliacao(nota = 7.0, periodoId = 2), // 2º: 7,0
            avaliacao(nota = 6.0, periodoId = null, tipo = TipoAvaliacao.FINAL),
        )

        val media = CalcularMediaAnual(avaliacoes, regra(TipoMedia.SIMPLES))

        assertEquals(6.5, media.regular!!, 0.001) // (6 + 7) / 2, já com a recuperação
        assertEquals(6.5, media.media!!, 0.001) // a final, 6,0, é menor
    }
}

// Preenche os campos que o cálculo não usa, pra cada teste mostrar só o que importa.
private fun avaliacao(
    nota: Double?,
    notaMaxima: Double = 10.0,
    peso: Double = 1.0,
    tipo: TipoAvaliacao = TipoAvaliacao.NORMAL,
    periodoId: Long? = 1,
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

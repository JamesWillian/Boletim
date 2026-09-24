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
}

// Preenche os campos que o cálculo não usa, pra cada teste mostrar só o que importa.
private fun avaliacao(
    nota: Double,
    peso: Double = 1.0,
    tipo: TipoAvaliacao = TipoAvaliacao.NORMAL,
) = AvaliacaoDomain(
    disciplinaId = 1,
    periodoId = 1,
    nome = "Prova",
    nota = nota,
    notaMaxima = 10.0,
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

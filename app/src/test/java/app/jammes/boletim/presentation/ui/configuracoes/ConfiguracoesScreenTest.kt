package app.jammes.boletim.presentation.ui.configuracoes

import app.jammes.boletim.domain.model.EstiloDasAbas
import app.jammes.boletim.domain.model.Tema
import org.junit.Assert.assertEquals
import org.junit.Test

/** Os textos das opções das configurações. */
class ConfiguracoesScreenTest {

    @Test fun `cada tema diz o que faz`() {
        assertEquals(
            listOf("Sempre claro", "Sempre escuro", "Igual ao do aparelho"),
            Tema.entries.map(::resumoDoTema),
        )
    }

    @Test fun `estilo da sigla mostra as tres primeiras do ano`() {
        assertEquals(
            "POR, MAT, HIS…",
            resumoDoEstilo(EstiloDasAbas.ICONES_E_SIGLA, listOf("POR", "MAT", "HIS", "GEO")),
        )
        assertEquals("POR, MAT", resumoDoEstilo(EstiloDasAbas.ICONES_E_SIGLA, listOf("POR", "MAT")))
    }

    @Test fun `siglas repetidas aparecem uma vez`() {
        assertEquals("MAT, POR", resumoDoEstilo(EstiloDasAbas.ICONES_E_SIGLA, listOf("MAT", "MAT", "POR")))
    }

    @Test fun `sem disciplinas o estilo da sigla usa exemplos`() {
        assertEquals("MAT, POR, HIS", resumoDoEstilo(EstiloDasAbas.ICONES_E_SIGLA, emptyList()))
    }

    @Test fun `os nomes sao os das opcoes pedidas`() {
        assertEquals(
            listOf("Apenas ícones", "Ícones + sigla", "Ícones + descrição"),
            EstiloDasAbas.entries.map(::nomeDoEstilo),
        )
        assertEquals(listOf("Claro", "Escuro", "Sistema"), Tema.entries.map(::nomeDoTema))
    }
}

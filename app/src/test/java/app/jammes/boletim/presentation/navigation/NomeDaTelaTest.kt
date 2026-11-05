package app.jammes.boletim.presentation.navigation

import kotlinx.serialization.serializer
import org.junit.Assert.assertEquals
import org.junit.Test

/** O nome com que cada tela aparece no registro de uso. */
class NomeDaTelaTest {

    private val routes = "app.jammes.boletim.presentation.navigation.Routes"

    @Test fun `rota sem argumentos fica so com o nome`() {
        assertEquals("Configuracoes", nomeDaTela("$routes.Configuracoes"))
    }

    @Test fun `rota dentro de um grafo leva o grafo no nome`() {
        assertEquals("Boletim.Geral", nomeDaTela("$routes.Boletim.Geral"))
    }

    @Test fun `argumento no caminho fica de fora`() {
        assertEquals("EditarDisciplina", nomeDaTela("$routes.EditarDisciplina/{disciplinaId}"))
    }

    @Test fun `argumentos no caminho e na query ficam de fora`() {
        assertEquals(
            "Boletim.Disciplina",
            nomeDaTela("$routes.Boletim.Disciplina/{disciplinaId}?avaliacaoId={avaliacaoId}&abaDeFaltas={abaDeFaltas}"),
        )
    }

    @Test fun `argumento so na query fica de fora`() {
        assertEquals("Exemplo", nomeDaTela("$routes.Exemplo?abaDeFaltas={abaDeFaltas}"))
    }

    // O modelo da rota começa pelo serialName da classe, e é dele que o nome sai
    @Test fun `o nome sai do serialName das rotas`() {
        assertEquals("Aluno", nomeDaTela(serializer<Routes.Aluno>().descriptor.serialName))
        assertEquals("Boletim.Disciplina", nomeDaTela(serializer<Routes.Boletim.Disciplina>().descriptor.serialName))
    }
}

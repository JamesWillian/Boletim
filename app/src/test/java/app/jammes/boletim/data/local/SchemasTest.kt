package app.jammes.boletim.data.local

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Cada versão do banco tem uma estrutura só. O build regrava o N.json da versão atual a cada compilação, então
 * mudar uma entidade sem aumentar a version passaria calado, e quem já tem o banco nessa versão veria o app
 * fechar ao abrir ("Room cannot verify the data integrity").
 */
class SchemasTest {

    @Test fun `a estrutura de uma versao nao muda depois de criada`() {
        val exportados = PASTA.listFiles { arquivo -> arquivo.extension == "json" }.orEmpty()
            .associate { arquivo -> versaoEHash(arquivo) }

        assertTrue("Nenhum schema em ${PASTA.absolutePath}", exportados.isNotEmpty())

        val mudaram = HASHES.keys.filter { versao -> versao in exportados && exportados[versao] != HASHES[versao] }
        assertTrue(
            "A estrutura da versão $mudaram mudou depois de criada. Aumente a version do AppDatabase e escreva a " +
                "migração; depois volte o N.json pelo git e rode ./gradlew clean, porque o build guarda uma cópia " +
                "do schema errado em app/build/intermediates/room e a copiaria de volta.",
            mudaram.isEmpty(),
        )
        assertEquals("Versão nova? Fixe o hash dela em HASHES. Sumiu um N.json? Volte ele pelo git.", HASHES, exportados)
    }

    private fun versaoEHash(arquivo: File): Pair<Int, String> {
        val banco = Json.parseToJsonElement(arquivo.readText()).jsonObject.getValue("database").jsonObject
        return banco.getValue("version").jsonPrimitive.int to banco.getValue("identityHash").jsonPrimitive.content
    }

    companion object {
        // O teste roda com a pasta do módulo (app/) como diretório de trabalho
        private val PASTA = File("schemas/${AppDatabase::class.java.name}")

        // Versão → identityHash do N.json. Cada versão nova ganha uma linha, e as que já estão aqui não mudam mais.
        private val HASHES = mapOf(
            1 to "2dc9fb5fb6b81ddcac095f98a2d12575",
        )
    }
}

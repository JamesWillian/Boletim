package app.jammes.boletim.data.backup

import app.jammes.boletim.data.local.preferences.DATASTORE_DO_PLANO
import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * O que as regras do Auto Backup (res/xml) levam. O plano fica de fora em todas: quem diz se a conta
 * comprou o Pro é a Google Play, e não um backup restaurado num aparelho novo.
 */
class RegrasDoBackupTest {

    @Test fun `ate o Android 11, o plano fica fora do backup`() {
        assertEquals(listOf(PLANO), excluidos(regras("backup_rules.xml")))
    }

    @Test fun `do Android 12 em diante, o plano fica fora da nuvem e da copia entre aparelhos`() {
        val regras = regras("data_extraction_rules.xml")

        assertEquals(listOf(PLANO), excluidos(secao(regras, "cloud-backup")))
        assertEquals(listOf(PLANO), excluidos(secao(regras, "device-transfer")))
    }

    @Test fun `sem include, o resto do app continua no backup`() {
        // Um <include> faz o backup levar só o que ele lista: o banco, por exemplo, ficaria de fora
        for (arquivo in listOf("backup_rules.xml", "data_extraction_rules.xml")) {
            assertEquals("<include> em $arquivo", 0, regras(arquivo).getElementsByTagName("include").length)
        }
    }

    // Os testes rodam na pasta do módulo app, como o SchemasTest
    private fun regras(arquivo: String): Element =
        DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File("src/main/res/xml/$arquivo"))
            .documentElement

    private fun secao(regras: Element, nome: String): Element =
        regras.getElementsByTagName(nome).item(0) as? Element ?: error("Sem a seção <$nome>")

    // Cada <exclude> como "domínio:caminho"
    private fun excluidos(elemento: Element): List<String> {
        val nos = elemento.getElementsByTagName("exclude")
        return (0 until nos.length)
            .map { nos.item(it) as Element }
            .map { "${it.getAttribute("domain")}:${it.getAttribute("path")}" }
    }

    private companion object {
        // O arquivo do DataStore do PlanoPreferences
        val PLANO = "file:datastore/$DATASTORE_DO_PLANO.preferences_pb"
    }
}

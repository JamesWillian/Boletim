package app.jammes.boletim.data.backup

import app.jammes.boletim.domain.model.BackupInvalidoException
import app.jammes.boletim.domain.model.SenhaIncorretaException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.security.SecureRandom
import java.text.Normalizer
import java.util.Base64
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/*
 * O backup com senha também é um JSON com a marca do app, mas no lugar das tabelas vai o backup
 * inteiro criptografado: o mesmo texto que sairia sem senha. A chave sai da senha pelo PBKDF2 (HMAC-
 * SHA256, com um sal aleatório) e criptografa com AES-256-GCM, que também percebe qualquer mudança
 * no arquivo. Tudo do próprio Android, sem biblioteca.
 *
 * O sal, o IV e as iterações vão abertos no arquivo: sem a senha não servem para nada, e assim um
 * arquivo antigo continua abrindo mesmo que o padrão mude. A versão do backup lá dentro também vai
 * aberta, para um app mais velho pedir para atualizar antes de pedir a senha.
 */

/** O [texto] de um backup criptografado com a [senha], já como o texto do arquivo. */
fun protegerComSenha(
    texto: String,
    senha: String,
    iteracoes: Int = ITERACOES,
    aleatorio: SecureRandom = SecureRandom(),
): String {
    // Sal e IV novos a cada arquivo: a mesma senha nunca repete a chave nem a cifra
    val sal = ByteArray(TAMANHO_DO_SAL).also(aleatorio::nextBytes)
    val iv = ByteArray(TAMANHO_DO_IV).also(aleatorio::nextBytes)
    val dados = cifra(Cipher.ENCRYPT_MODE, senha, VERSAO_DO_ARQUIVO, iteracoes, sal, iv)
        .doFinal(texto.toByteArray(Charsets.UTF_8))

    val json = BackupComSenhaJson(
        app = MARCA_DO_APP,
        versao = VERSAO_DO_ARQUIVO,
        protecao = ProtecaoJson(ALGORITMO, iteracoes, BASE64.encodeToString(sal), BASE64.encodeToString(iv)),
        dados = BASE64.encodeToString(dados),
    )
    return JSON.encodeToString(BackupComSenhaJson.serializer(), json)
}

/**
 * Se o [texto] é de um backup com senha. Antes de pedir a senha, confere o que dá para conferir sem
 * ela: se a versão é uma que este app entende e se a proteção está inteira. Se não der, sai
 * [BackupInvalidoException] com o motivo. O que não é backup com senha dá false, e quem confere
 * ele é o lerBackup.
 */
fun temSenha(texto: String): Boolean = lerProtegido(texto) != null

/**
 * O texto do backup que estava protegido, como ele saiu do escreverBackup. Com a senha errada sai
 * [SenhaIncorretaException]; o que tem dentro, quem confere é o lerBackup.
 */
fun abrirComSenha(texto: String, senha: String): String {
    val protegido = lerProtegido(texto) ?: throw BackupInvalidoException(NAO_E_BACKUP)
    if (senha.isEmpty()) throw SenhaIncorretaException()
    val aberto = try {
        cifra(Cipher.DECRYPT_MODE, senha, protegido.versao, protegido.iteracoes, protegido.sal, protegido.iv)
            .doFinal(protegido.dados)
    } catch (e: AEADBadTagException) {
        // A conferência do GCM não fecha. Com a senha errada ou com o arquivo alterado, é igual:
        // não dá para saber qual dos dois, e a senha errada é o que acontece de verdade
        throw SenhaIncorretaException()
    }
    return String(aberto, Charsets.UTF_8)
}

internal const val ALGORITMO = "PBKDF2WithHmacSHA256/AES-256-GCM"

// O que a OWASP pede para o PBKDF2 com SHA-256. Cada senha testada custa isso, para quem tentar
// adivinhar também: uns 0,3 s num celular de 2024. No build de debug, uns 10 s (ver chaveDaSenha)
internal const val ITERACOES = 600_000

// Um arquivo pedindo mais que isso travaria o app por muito tempo: é um arquivo alterado
private const val ITERACOES_MAXIMAS = 10_000_000

private const val TAMANHO_DO_SAL = 16 // bytes
private const val TAMANHO_DO_IV = 12 // bytes, o que o GCM usa
private const val TAMANHO_DA_ETIQUETA = 128 // bits: a conferência que vai no fim dos dados

private val JSON = Json { prettyPrint = true }
private val BASE64 = Base64.getEncoder()

/** O arquivo com senha já conferido por fora, com os bytes prontos para abrir. */
private class Protegido(
    val versao: Int,
    val iteracoes: Int,
    val sal: ByteArray,
    val iv: ByteArray,
    val dados: ByteArray,
)

/** null quando o [texto] não é um backup com senha: nem JSON, de outro app ou o backup sem senha. */
private fun lerProtegido(texto: String): Protegido? {
    val raiz = try {
        JSON.parseToJsonElement(texto) as? JsonObject
    } catch (e: SerializationException) {
        null
    }
    if (raiz == null || "protecao" !in raiz || (raiz["app"] as? JsonPrimitive)?.contentOrNull != MARCA_DO_APP) {
        return null
    }

    val json = try {
        JSON.decodeFromJsonElement(BackupComSenhaJson.serializer(), raiz)
    } catch (e: IllegalArgumentException) {
        throw BackupInvalidoException(BACKUP_ALTERADO) // campo faltando ou com tipo errado
    }
    // Outro algoritmo só pode ser de um app mais novo, que protege de outro jeito
    if (json.versao > VERSAO_DO_ARQUIVO || json.protecao.algoritmo != ALGORITMO) {
        throw BackupInvalidoException(VERSAO_MAIS_NOVA)
    }

    val decodificador = Base64.getDecoder()
    val protegido = try {
        Protegido(
            versao = json.versao,
            iteracoes = json.protecao.iteracoes,
            sal = decodificador.decode(json.protecao.sal),
            iv = decodificador.decode(json.protecao.iv),
            dados = decodificador.decode(json.dados),
        )
    } catch (e: IllegalArgumentException) {
        throw BackupInvalidoException(BACKUP_ALTERADO) // não é Base64
    }
    val inteiro = protegido.iteracoes in 1..ITERACOES_MAXIMAS &&
        protegido.sal.size == TAMANHO_DO_SAL &&
        protegido.iv.size == TAMANHO_DO_IV &&
        protegido.dados.size >= TAMANHO_DA_ETIQUETA / 8
    if (!inteiro) throw BackupInvalidoException(BACKUP_ALTERADO)
    return protegido
}

/**
 * O AES-GCM com a chave da [senha]. A marca, a versão e o algoritmo, que vão abertos, entram na
 * conferência do GCM: mudar um deles no arquivo é como mudar os dados. O sal, o IV e as iterações
 * não precisam, porque mudar um deles já muda a chave ou a cifra.
 */
private fun cifra(modo: Int, senha: String, versao: Int, iteracoes: Int, sal: ByteArray, iv: ByteArray): Cipher {
    val chave = SecretKeySpec(chaveDaSenha(senha, sal, iteracoes), "AES")
    return Cipher.getInstance("AES/GCM/NoPadding").apply {
        init(modo, chave, GCMParameterSpec(TAMANHO_DA_ETIQUETA, iv))
        updateAAD("$MARCA_DO_APP/$versao/$ALGORITMO".toByteArray(Charsets.UTF_8))
    }
}

/**
 * A chave AES-256 da [senha]: o PBKDF2 com HMAC-SHA256 da RFC 8018. Os 32 bytes da chave são
 * exatamente um bloco do PBKDF2, então a conta é só a do primeiro: o HMAC do sal com o número do
 * bloco, e depois o HMAC do anterior, [iteracoes] vezes, somando tudo com XOR.
 *
 * O SecretKeyFactory("PBKDF2WithHmacSHA256") faz a mesma conta, e os testes conferem que dá o mesmo
 * resultado. Feita aqui, ela é mais rápida no Android (o HMAC é o nativo, do BoringSSL, e não o do
 * BouncyCastle, em Java: 0,3 s contra 0,5 s num Xiaomi de 2024), e os bytes da senha são sempre
 * UTF-8. Isso não depende do Android: no 4.4, o SecretKeyFactory mudou como converte os acentos.
 *
 * No build de debug, as duas levam uns 10 s no emulador: o Android roda o código dele sem a
 * compilação de antes e confere cada chamada nativa (CheckJNI). No build de release, não.
 */
internal fun chaveDaSenha(senha: String, sal: ByteArray, iteracoes: Int): ByteArray {
    // Um "é" pode chegar como uma letra só ou como "e" mais o acento, dependendo do teclado.
    // Normalizada, a mesma senha dá a mesma chave nos dois casos
    val bytesDaSenha = Normalizer.normalize(senha, Normalizer.Form.NFC).toByteArray(Charsets.UTF_8)
    val hmac = Mac.getInstance("HmacSHA256")
    try {
        hmac.init(SecretKeySpec(bytesDaSenha, "HmacSHA256"))
    } finally {
        bytesDaSenha.fill(0) // a senha não fica esquecida na memória além do necessário
    }

    val u = ByteArray(hmac.macLength)
    hmac.update(sal)
    hmac.update(byteArrayOf(0, 0, 0, 1)) // o número do bloco: só há o primeiro
    hmac.doFinal(u, 0)
    val chave = u.copyOf()
    repeat(iteracoes - 1) {
        hmac.update(u)
        hmac.doFinal(u, 0)
        for (i in chave.indices) chave[i] = (chave[i].toInt() xor u[i].toInt()).toByte()
    }
    return chave
}

// O formato ------------------------------------------------------------------------------------

@Serializable
private data class BackupComSenhaJson(
    val app: String,
    val versao: Int, // a do backup que vai dentro
    val protecao: ProtecaoJson,
    val dados: String, // o backup criptografado, em Base64, com a etiqueta do GCM no fim
)

@Serializable
private data class ProtecaoJson(
    val algoritmo: String,
    val iteracoes: Int,
    val sal: String, // Base64
    val iv: String, // Base64
)

package app.jammes.boletim.data.local.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * O nome do DataStore do plano, que vira o arquivo files/datastore/plano.preferences_pb. As regras de
 * backup (res/xml) deixam esse arquivo de fora: quem diz se a conta comprou o Pro é a Google Play.
 */
internal const val DATASTORE_DO_PLANO = "plano"

// Separado das configurações: o plano não é uma escolha da pessoa
private val Context.dataStore by preferencesDataStore(name = DATASTORE_DO_PLANO)

@Singleton
class PlanoPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val PRO_DE_TESTE = booleanPreferencesKey("pro_de_teste")
        val PRO_COMPRADO = booleanPreferencesKey("pro_comprado")
    }

    /** A chave "Pro de teste" das Configurações do debug. */
    val proDeTeste: Flow<Boolean> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[Keys.PRO_DE_TESTE] == true }
        .distinctUntilChanged()

    suspend fun salvarProDeTeste(ligado: Boolean) {
        context.dataStore.edit { prefs ->
            if (ligado) prefs[Keys.PRO_DE_TESTE] = true else prefs.remove(Keys.PRO_DE_TESTE)
        }
    }

    /**
     * A compra do Pro confirmada pela Play, guardada para o app abrir já no Pro, mesmo quando a Play não
     * responde (sem rede, por exemplo). Quem muda é só a loja, com o que a Play confirmou.
     */
    val comprado: Flow<Boolean> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[Keys.PRO_COMPRADO] == true }
        .distinctUntilChanged()

    suspend fun salvarComprado(comprado: Boolean) {
        context.dataStore.edit { prefs ->
            if (comprado) prefs[Keys.PRO_COMPRADO] = true else prefs.remove(Keys.PRO_COMPRADO)
        }
    }
}

package app.jammes.boletim.data.local.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.LocalDate

// Separado das dicas: este não é da pessoa dispensar, é o app lembrando quando já pediu
private val Context.dataStore by preferencesDataStore(name = "avaliacao_na_loja")

@Singleton
class AvaliacaoNaLojaPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val ULTIMO_PEDIDO = longPreferencesKey("ultimo_pedido") // o dia, em LocalDate.toEpochDay
    }

    /** O dia do último pedido; null se o app nunca pediu. */
    val ultimoPedido: Flow<LocalDate?> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs[Keys.ULTIMO_PEDIDO]?.let(LocalDate::ofEpochDay) }
        .distinctUntilChanged()

    suspend fun salvarUltimoPedido(dia: LocalDate) {
        context.dataStore.edit { prefs -> prefs[Keys.ULTIMO_PEDIDO] = dia.toEpochDay() }
    }
}

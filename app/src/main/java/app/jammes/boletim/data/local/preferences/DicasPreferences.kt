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

// Separado do contexto: são preferências da pessoa, não do aluno aberto
private val Context.dataStore by preferencesDataStore(name = "dicas")

@Singleton
class DicasPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val PRIMEIRA_NOTA = booleanPreferencesKey("primeira_nota")
    }

    val primeiraNota: Flow<Boolean> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it[Keys.PRIMEIRA_NOTA] == true }
        .distinctUntilChanged()

    suspend fun salvarPrimeiraNota(pedida: Boolean) {
        context.dataStore.edit { prefs ->
            if (pedida) prefs[Keys.PRIMEIRA_NOTA] = true else prefs.remove(Keys.PRIMEIRA_NOTA)
        }
    }
}

package app.jammes.boletim.data.local.preferences

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.jammes.boletim.domain.model.Contexto
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okio.IOException

private val Context.dataStore by preferencesDataStore(name = "contexto")

@Singleton
class ContextoPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val ALUNO = longPreferencesKey("aluno_id")
        val ANO = longPreferencesKey("ano_letivo_id")
        val PERIODO = longPreferencesKey("periodo_id")
    }

    val contexto: Flow<Contexto?> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it.toContexto() }
        .distinctUntilChanged()

    suspend fun salvar(contexto: Contexto) {
        context.dataStore.edit { it.escrever(contexto) }
    }

    suspend fun atualizar(bloco: (Contexto) -> Contexto) {
        context.dataStore.edit { prefs ->
            prefs.toContexto()?.let { prefs.escrever(bloco(it)) }
        }
    }

    private fun Preferences.toContexto(): Contexto? {
        val aluno = this[Keys.ALUNO] ?: return null
        val ano = this[Keys.ANO] ?: return null
        val periodo = this[Keys.PERIODO] ?: return null
        return Contexto(aluno, ano, periodo)
    }

    private fun MutablePreferences.escrever(contexto: Contexto) {
        this[Keys.ALUNO] = contexto.alunoId
        this[Keys.ANO] = contexto.anoLetivoId
        this[Keys.PERIODO] = contexto.periodoId
    }
}
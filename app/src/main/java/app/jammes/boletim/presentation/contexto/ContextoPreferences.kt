package app.jammes.boletim.presentation.contexto

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.first

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

    suspend fun ler(): Contexto? {
        val prefs = context.dataStore.data.first()
        val aluno = prefs[Keys.ALUNO] ?: return null
        val ano = prefs[Keys.ANO] ?: return null
        val periodo = prefs[Keys.PERIODO] ?: return null
        return Contexto(aluno, ano, periodo)
    }

    suspend fun salvar(contexto: Contexto) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ALUNO] = contexto.alunoId
            prefs[Keys.ANO] = contexto.anoLetivoId
            prefs[Keys.PERIODO] = contexto.periodoId
        }
    }
}
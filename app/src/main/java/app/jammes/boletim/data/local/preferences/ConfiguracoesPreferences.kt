package app.jammes.boletim.data.local.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.jammes.boletim.domain.model.Configuracoes
import app.jammes.boletim.domain.model.EstiloDasAbas
import app.jammes.boletim.domain.model.Tema
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

// Separado do contexto, como as dicas: são preferências da pessoa, não do aluno aberto
private val Context.dataStore by preferencesDataStore(name = "configuracoes")

@Singleton
class ConfiguracoesPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Gravados pelo nome do enum. Um nome que não existe mais volta ao padrão
    private object Keys {
        val TEMA = stringPreferencesKey("tema")
        val ESTILO_DAS_ABAS = stringPreferencesKey("estilo_das_abas")
    }

    val configuracoes: Flow<Configuracoes> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it.toConfiguracoes() }
        .distinctUntilChanged()

    suspend fun salvarTema(tema: Tema) {
        context.dataStore.edit { it[Keys.TEMA] = tema.name }
    }

    suspend fun salvarEstiloDasAbas(estilo: EstiloDasAbas) {
        context.dataStore.edit { it[Keys.ESTILO_DAS_ABAS] = estilo.name }
    }

    private fun Preferences.toConfiguracoes(): Configuracoes {
        val padrao = Configuracoes()
        return Configuracoes(
            tema = Tema.entries.find { it.name == this[Keys.TEMA] } ?: padrao.tema,
            estiloDasAbas = EstiloDasAbas.entries.find { it.name == this[Keys.ESTILO_DAS_ABAS] }
                ?: padrao.estiloDasAbas,
        )
    }
}

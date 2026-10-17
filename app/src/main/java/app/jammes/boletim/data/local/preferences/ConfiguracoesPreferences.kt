package app.jammes.boletim.data.local.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.jammes.boletim.domain.model.Configuracoes
import app.jammes.boletim.domain.model.EstiloDasAbas
import app.jammes.boletim.domain.model.HORARIOS_PADRAO
import app.jammes.boletim.domain.model.Tema
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

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
        val BACKUP_AUTOMATICO = booleanPreferencesKey("backup_automatico")
        val NOTIFICACOES_ATIVAS = booleanPreferencesKey("notificacoes_ativas")
        val HORARIOS_DAS_NOTIFICACOES = stringSetPreferencesKey("horarios_das_notificacoes") // "19:00"
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

    suspend fun salvarBackupAutomatico(ligado: Boolean) {
        context.dataStore.edit { it[Keys.BACKUP_AUTOMATICO] = ligado }
    }

    suspend fun salvarNotificacoesAtivas(ativas: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICACOES_ATIVAS] = ativas }
    }

    /** Lê e grava os horários na mesma edição: duas mudanças seguidas não se atropelam. */
    suspend fun editarHorariosDasNotificacoes(mudar: (List<LocalTime>) -> List<LocalTime>) {
        context.dataStore.edit { prefs ->
            val novos = mudar(lerHorarios(prefs[Keys.HORARIOS_DAS_NOTIFICACOES]))
            prefs[Keys.HORARIOS_DAS_NOTIFICACOES] = novos.map(::escreverHorario).toSet()
        }
    }

    private fun Preferences.toConfiguracoes(): Configuracoes {
        val padrao = Configuracoes()
        return Configuracoes(
            tema = Tema.entries.find { it.name == this[Keys.TEMA] } ?: padrao.tema,
            estiloDasAbas = EstiloDasAbas.entries.find { it.name == this[Keys.ESTILO_DAS_ABAS] }
                ?: padrao.estiloDasAbas,
            backupAutomatico = this[Keys.BACKUP_AUTOMATICO] ?: padrao.backupAutomatico,
            notificacoesAtivas = this[Keys.NOTIFICACOES_ATIVAS] ?: padrao.notificacoesAtivas,
            horariosDasNotificacoes = lerHorarios(this[Keys.HORARIOS_DAS_NOTIFICACOES]),
        )
    }
}

/**
 * Os horários gravados, em ordem. Um texto que não se lê fica de fora; sem nenhum (nunca foram
 * escolhidos), vale o padrão.
 */
internal fun lerHorarios(gravados: Set<String>?): List<LocalTime> =
    gravados.orEmpty()
        .mapNotNull { texto ->
            try {
                LocalTime.parse(texto).truncatedTo(ChronoUnit.MINUTES)
            } catch (e: DateTimeParseException) {
                null
            }
        }
        .distinct()
        .sorted()
        .ifEmpty { HORARIOS_PADRAO }

/** "07:05": horas e minutos, sempre com dois dígitos. */
internal fun escreverHorario(horario: LocalTime): String = horario.format(FORMATO_DO_HORARIO)

private val FORMATO_DO_HORARIO = DateTimeFormatter.ofPattern("HH:mm")

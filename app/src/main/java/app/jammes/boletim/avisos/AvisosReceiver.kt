package app.jammes.boletim.avisos

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import app.jammes.boletim.domain.usecase.AvisarNotificacoes
import app.jammes.boletim.domain.usecase.ReagendarAvisos
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.time.ZonedDateTime

/** O despertador do horário das notificações tocou: gera e mostra os avisos, e marca o próximo horário. */
class AvisosReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val dependencias = dependenciasDosAvisos(context)
        emSegundoPlano {
            try {
                withTimeout(LIMITE_PARA_AVISAR) { dependencias.avisarNotificacoes()(ZonedDateTime.now()) }
            } finally {
                // O próximo fica marcado mesmo se o deste horário falhar ou demorar demais
                withContext(NonCancellable) { dependencias.reagendarAvisos()() }
            }
        }
    }
}

/**
 * O celular ligou, o relógio ou o fuso mudou, ou o app foi atualizado: o despertador do Android se
 * perde ou fica na hora errada, e precisa ser marcado de novo. Também quando a pessoa libera os
 * alarmes e lembretes: o despertador, que estava inexato, passa a ser exato.
 */
class ReagendarAvisosReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Só os avisos do sistema que o manifest pede
        if (intent.action !in ACOES_QUE_REAGENDAM) return
        val dependencias = dependenciasDosAvisos(context)
        emSegundoPlano { dependencias.reagendarAvisos()() }
    }
}

private val ACOES_QUE_REAGENDAM = setOf(
    Intent.ACTION_BOOT_COMPLETED,
    Intent.ACTION_TIME_CHANGED,
    Intent.ACTION_TIMEZONE_CHANGED,
    Intent.ACTION_MY_PACKAGE_REPLACED,
    // AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED, que só existe do Android 12 em diante
    "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
)

/** O que os receivers usam, tirado do Hilt: eles não são criados por ele. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface DependenciasDosAvisos {
    fun avisarNotificacoes(): AvisarNotificacoes
    fun reagendarAvisos(): ReagendarAvisos
}

private fun dependenciasDosAvisos(context: Context): DependenciasDosAvisos =
    EntryPointAccessors.fromApplication(context.applicationContext, DependenciasDosAvisos::class.java)

/**
 * Roda [bloco] fora da thread principal, com o receiver vivo até ele terminar. Uma falha fica no log,
 * em vez de derrubar o app em segundo plano.
 */
private fun BroadcastReceiver.emSegundoPlano(bloco: suspend () -> Unit) {
    val pendente = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
        try {
            bloco()
        } catch (e: Exception) {
            Log.e(TAG, "Os avisos falharam", e)
        } finally {
            pendente.finish()
        }
    }
}

// O Android dá uns 10 segundos a um receiver; marcar o próximo horário fica com a folga
private const val LIMITE_PARA_AVISAR = 8_000L
private const val TAG = "Avisos"

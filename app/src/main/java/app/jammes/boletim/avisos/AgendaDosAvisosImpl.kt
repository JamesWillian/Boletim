package app.jammes.boletim.avisos

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import app.jammes.boletim.domain.repository.AgendaDosAvisos
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.time.ZonedDateTime

/**
 * O despertador do horário das notificações, no AlarmManager. Acorda o app mesmo com o celular
 * parado há horas (Doze). Só um fica marcado: marcar de novo troca o anterior.
 *
 * É exato quando o Android deixa sem perguntar nada à pessoa: até o Android 11, sempre; no 12 e no
 * 13, com a permissão de alarmes e lembretes, que vem ligada. Do 14 em diante ela vem desligada, e o
 * despertador fica inexato: o Android pode atrasar até uma hora, o que pode cair no horário das aulas.
 */
@Singleton
class AgendaDosAvisosImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : AgendaDosAvisos {

    private val alarmes = context.getSystemService(AlarmManager::class.java)

    @SuppressLint("MissingPermission") // podeSerExato() confere a permissão antes
    override fun marcar(quando: ZonedDateTime) {
        val millis = quando.toInstant().toEpochMilli()
        if (podeSerExato()) {
            alarmes.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, despertar())
        } else {
            alarmes.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, despertar())
        }
    }

    private fun podeSerExato(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmes.canScheduleExactAlarms()

    override fun desmarcar() {
        alarmes.cancel(despertar())
    }

    // Sempre o mesmo, para o novo tomar o lugar do anterior
    private fun despertar(): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, AvisosReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}

package app.jammes.boletim.avisos

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat

// Os avisos do celular são uma notificação por aluno, todas no mesmo grupo, e o resumo do grupo. Cada
// uma é achada pela tag e pelo id: a do aluno tem o id dele, e o resumo, um id que nenhum aluno tem
internal const val CANAL_DOS_AVISOS = "avisos"
internal const val GRUPO_DOS_AVISOS = "app.jammes.boletim.AVISOS"
internal const val TAG_DO_ALUNO = "aluno"
internal const val TAG_DO_RESUMO = "resumo"
internal const val ID_DO_RESUMO = 0

/**
 * O canal dos avisos, que aparece nas configurações do Android como "Avisos do Boletim Escolar". Criar de
 * novo não muda o que a pessoa já ajustou nele, então dá para chamar sempre que o app abre.
 */
fun criarCanalDosAvisos(context: Context) {
    NotificationManagerCompat.from(context).createNotificationChannel(
        NotificationChannelCompat.Builder(CANAL_DOS_AVISOS, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName("Avisos do Boletim Escolar")
            .setDescription("Provas chegando, notas para lançar, médias abaixo da mínima e faltas")
            .build()
    )
}

/**
 * Se o Android deixa o app mostrar os avisos: a permissão (do Android 13 em diante), as notificações
 * do app ligadas e o canal dos avisos ligado.
 */
fun notificacoesLiberadas(context: Context): Boolean {
    val gerente = NotificationManagerCompat.from(context)
    if (!gerente.areNotificationsEnabled()) return false
    // Antes de o canal existir, vale o que o app inteiro diz
    val canal = gerente.getNotificationChannelCompat(CANAL_DOS_AVISOS) ?: return true
    return canal.importance != NotificationManagerCompat.IMPORTANCE_NONE
}

package app.jammes.boletim.avisos

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.jammes.boletim.R
import app.jammes.boletim.domain.model.AvisoDoAluno
import app.jammes.boletim.domain.repository.AvisosNoCelular
import app.jammes.boletim.presentation.ui.theme.BoletimColors
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton

/**
 * Os avisos na barra do celular: uma notificação por aluno, todas no grupo dos avisos, e o resumo do
 * grupo. As dos alunos não tocam; quem toca é o resumo, uma vez a cada horário com novidade. Com um
 * aluno só na barra, o Android mostra só a notificação dele.
 *
 * O resumo fica até o app tirar: o toque nele abre a lista do aluno aberto, e os avisos dos outros
 * alunos continuam valendo.
 */
@Singleton
class AvisosNoCelularImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : AvisosNoCelular {

    private val gerente = NotificationManagerCompat.from(context)

    override fun podeMostrar(): Boolean = notificacoesLiberadas(context)

    @SuppressLint("MissingPermission") // podeMostrar() confere a permissão antes
    override fun mostrar(novos: List<AvisoDoAluno>, pendentes: List<AvisoDoAluno>) {
        if (novos.isEmpty() || !podeMostrar()) return
        criarCanalDosAvisos(context)
        // As que acabaram de ser postadas ainda podem não aparecer entre as ativas: entram à mão
        val naBarra = alunosNaBarra() + novos.map { it.alunoId }
        novos.forEach { aviso -> gerente.notify(TAG_DO_ALUNO, idDoAluno(aviso.alunoId), avisoDoAluno(aviso)) }
        refazerResumo(pendentes.filter { it.alunoId in naBarra }, silencioso = false)
    }

    @SuppressLint("MissingPermission")
    override fun retirar(alunoId: Long, pendentes: List<AvisoDoAluno>) {
        val naBarra = alunosNaBarra()
        if (alunoId in naBarra) gerente.cancel(TAG_DO_ALUNO, idDoAluno(alunoId))
        // O resumo é refeito mesmo quando o aviso do aluno já tinha saído: o toque nele tira ele da barra
        // antes de o app abrir, e o resumo não pode ficar sozinho, nem citando quem já saiu
        if (!resumoNaBarra()) return
        val ficam = pendentes.filter { it.alunoId in naBarra && it.alunoId != alunoId }
        if (ficam.isEmpty() || !podeMostrar()) {
            gerente.cancel(TAG_DO_RESUMO, ID_DO_RESUMO)
        } else {
            refazerResumo(ficam, silencioso = true)
        }
    }

    @SuppressLint("MissingPermission")
    private fun refazerResumo(avisos: List<AvisoDoAluno>, silencioso: Boolean) {
        if (avisos.isEmpty()) return
        gerente.notify(TAG_DO_RESUMO, ID_DO_RESUMO, resumo(avisos, silencioso))
    }

    /** Os alunos que já têm aviso na barra: a pessoa pode ter dispensado alguns. */
    private fun alunosNaBarra(): Set<Long> =
        naBarra().filter { it.tag == TAG_DO_ALUNO }.map { it.id.toLong() }.toSet()

    private fun resumoNaBarra(): Boolean = naBarra().any { it.tag == TAG_DO_RESUMO }

    private fun naBarra() = context.getSystemService(NotificationManager::class.java).activeNotifications

    private fun avisoDoAluno(aviso: AvisoDoAluno): Notification {
        val estilo = aviso.notificacoes.singleOrNull()
            ?.let { NotificationCompat.BigTextStyle().bigText(it.texto) }
            ?: NotificationCompat.InboxStyle().also { inbox -> linhasDoAviso(aviso).forEach(inbox::addLine) }
        return base()
            .setContentTitle(tituloDoAviso(aviso))
            .setContentText(textoDoAviso(aviso))
            // No cabeçalho, ao lado do nome do app: de qual aluno é. Com um aluno só, nada
            .setSubText(aviso.nome)
            .setStyle(estilo)
            .setNumber(aviso.notificacoes.size)
            .setWhen(aviso.notificacoes.first().criadaEm.toEpochMilliseconds())
            .setShowWhen(true)
            .setContentIntent(abrirNotificacoes(aviso.alunoId))
            .build()
    }

    private fun resumo(avisos: List<AvisoDoAluno>, silencioso: Boolean): Notification {
        val estilo = NotificationCompat.InboxStyle()
        avisos.forEach { estilo.addLine(linhaDoResumo(it)) }
        return base()
            .setContentTitle(tituloDoResumo(avisos))
            .setContentText(avisos.joinToString(" · ") { linhaDoResumo(it) })
            .setStyle(estilo)
            .setGroupSummary(true)
            // Refazer o resumo porque um aluno saiu da barra não toca de novo, nem muda a hora dele
            .setSilent(silencioso)
            .setWhen(avisos.maxOf { it.notificacoes.first().criadaEm }.toEpochMilliseconds())
            .setContentIntent(abrirNotificacoes(alunoId = null))
            .setAutoCancel(false)
            .build()
    }

    /** O que todos os avisos têm: o canal, o ícone, a cor do app e o grupo, onde só o resumo toca. */
    private fun base(): NotificationCompat.Builder =
        NotificationCompat.Builder(context, CANAL_DOS_AVISOS)
            .setSmallIcon(R.drawable.ic_notificacao)
            .setColor(BoletimColors.Primary.toArgb())
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setGroup(GRUPO_DOS_AVISOS)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .setAutoCancel(true)

    /**
     * O toque abre as notificações do aluno; no resumo, as do aluno aberto. Cada aluno tem o seu
     * PendingIntent: o código é o id dele, e o do resumo é o ID_DO_RESUMO, que nenhum aluno tem.
     */
    private fun abrirNotificacoes(alunoId: Long?): PendingIntent =
        PendingIntent.getActivity(
            context,
            alunoId?.let(::idDoAluno) ?: ID_DO_RESUMO,
            AberturaPeloAviso.intent(context, alunoId),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    // Os ids do Room começam em 1 e não chegam perto do limite de um Int
    private fun idDoAluno(alunoId: Long): Int = alunoId.toInt()
}

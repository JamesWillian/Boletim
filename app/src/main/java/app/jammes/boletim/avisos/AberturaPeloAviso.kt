package app.jammes.boletim.avisos

import android.content.Context
import android.content.Intent
import app.jammes.boletim.MainActivity

/**
 * O toque no aviso do celular abre o app nas notificações: o aviso de um aluno, nas dele; o resumo,
 * nas do aluno que estiver aberto. Aqui ficam o intent que o aviso leva e a leitura dele na MainActivity.
 */
object AberturaPeloAviso {

    private const val ACAO = "app.jammes.boletim.ABRIR_NOTIFICACOES"
    private const val EXTRA_ALUNO = "aluno_id"

    /** O intent do aviso do aluno [alunoId]; o do resumo vai sem aluno (null). */
    fun intent(context: Context, alunoId: Long?): Intent =
        Intent(context, MainActivity::class.java)
            .setAction(ACAO)
            // Com o app aberto, a mesma tela recebe o pedido, em vez de outra abrir por cima dela
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .apply { if (alunoId != null) putExtra(EXTRA_ALUNO, alunoId) }

    /**
     * O que o [intent] pede: abrir as notificações, de um aluno ou do aberto. null quando ele não veio
     * de um aviso, ou quando o app foi reaberto pelos recentes, que repetem o intent do toque.
     */
    fun ler(intent: Intent): Pedido? {
        if (intent.action != ACAO) return null
        if (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) return null
        return Pedido(alunoId = if (intent.hasExtra(EXTRA_ALUNO)) intent.getLongExtra(EXTRA_ALUNO, 0L) else null)
    }

    /** Abrir as notificações do aluno [alunoId], ou do que estiver aberto (null). */
    data class Pedido(val alunoId: Long?)
}

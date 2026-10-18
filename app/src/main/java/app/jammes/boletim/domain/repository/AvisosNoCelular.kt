package app.jammes.boletim.domain.repository

import app.jammes.boletim.domain.model.AvisoDoAluno

/**
 * A notificação do Android, na barra do celular: uma por aluno, todas no mesmo grupo, com um resumo
 * por cima. Só o resumo toca, e uma vez só, mesmo com vários alunos.
 */
interface AvisosNoCelular {

    /** Se o Android deixa o app mostrar notificações agora: a permissão e o canal dos avisos. */
    fun podeMostrar(): Boolean

    /**
     * Mostra o aviso de cada aluno de [novos], com tudo o que ele ainda não viu, e refaz o resumo com
     * os alunos que ficam na barra, a partir dos [pendentes]. Toca uma vez.
     */
    fun mostrar(novos: List<AvisoDoAluno>, pendentes: List<AvisoDoAluno>)

    /**
     * Tira da barra o aviso do aluno, que acabou de ver as notificações dele no app, e refaz o resumo
     * com os [pendentes] dos outros, sem tocar.
     */
    fun retirar(alunoId: Long, pendentes: List<AvisoDoAluno>)
}

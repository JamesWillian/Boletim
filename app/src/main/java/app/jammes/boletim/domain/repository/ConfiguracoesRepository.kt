package app.jammes.boletim.domain.repository

import app.jammes.boletim.domain.model.Configuracoes
import app.jammes.boletim.domain.model.EstiloDasAbas
import app.jammes.boletim.domain.model.Tema
import kotlinx.coroutines.flow.Flow
import java.time.LocalTime

/** As configurações do app: o tema, o estilo das abas laterais, o backup automático e as notificações. */
interface ConfiguracoesRepository {

    /** As gravadas; o que nunca foi escolhido vem no padrão. */
    val configuracoes: Flow<Configuracoes>

    suspend fun salvarTema(tema: Tema)

    suspend fun salvarEstiloDasAbas(estilo: EstiloDasAbas)

    suspend fun salvarBackupAutomatico(ligado: Boolean)

    suspend fun salvarNotificacoesAtivas(ativas: Boolean)

    /**
     * Troca os horários das notificações pelo que [mudar] devolver a partir dos gravados, na mesma
     * edição: dois toques seguidos não se atropelam.
     */
    suspend fun editarHorariosDasNotificacoes(mudar: (List<LocalTime>) -> List<LocalTime>)

    /** Se o app já mostrou o pedido de permissão de notificações do Android (Android 13 em diante). */
    val permissaoDeNotificacoesPedida: Flow<Boolean>

    suspend fun marcarPermissaoDeNotificacoesPedida()
}

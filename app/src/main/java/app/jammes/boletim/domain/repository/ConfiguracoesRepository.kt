package app.jammes.boletim.domain.repository

import app.jammes.boletim.domain.model.Configuracoes
import app.jammes.boletim.domain.model.EstiloDasAbas
import app.jammes.boletim.domain.model.Tema
import kotlinx.coroutines.flow.Flow

/** As configurações do app: o tema e o estilo das abas laterais. */
interface ConfiguracoesRepository {

    /** As gravadas; o que nunca foi escolhido vem no padrão. */
    val configuracoes: Flow<Configuracoes>

    suspend fun salvarTema(tema: Tema)

    suspend fun salvarEstiloDasAbas(estilo: EstiloDasAbas)
}

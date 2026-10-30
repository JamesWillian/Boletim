package app.jammes.boletim.domain.repository

import app.jammes.boletim.domain.model.Plano
import kotlinx.coroutines.flow.Flow

/** O plano do app: grátis, com os limites, ou Pro. */
interface PlanoRepository {

    /** O que vale agora. */
    val plano: Flow<Plano>

    /**
     * Só no build de debug, para testar os dois lados sem comprar: o app passa a agir como Pro
     * ([ligado]) ou como grátis. Fora do debug, não muda nada.
     */
    suspend fun salvarProDeTeste(ligado: Boolean)
}

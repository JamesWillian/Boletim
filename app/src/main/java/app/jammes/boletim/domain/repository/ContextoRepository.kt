package app.jammes.boletim.domain.repository

import app.jammes.boletim.domain.model.Contexto
import kotlinx.coroutines.flow.Flow

interface ContextoRepository {

    val contexto: Flow<Contexto?>

    suspend fun selecionarAluno(alunoId: Long, anoLetivoId: Long, periodoId: Long)
    suspend fun selecionarAnoLetivo(anoLetivoId: Long, periodoId: Long)
    suspend fun selecionarPeriodo(periodoId: Long)
}
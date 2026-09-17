package app.jammes.boletim.data.repository

import app.jammes.boletim.domain.model.Contexto
import app.jammes.boletim.domain.repository.ContextoRepository
import app.jammes.boletim.data.local.preferences.ContextoPreferences
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow

class ContextoRepositoryImpl @Inject constructor(
    private val prefs: ContextoPreferences
): ContextoRepository {

    override val contexto: Flow<Contexto?> = prefs.contexto

    override suspend fun selecionarAluno(
        alunoId: Long,
        anoLetivoId: Long,
        periodoId: Long
    ) {
        prefs.salvar(Contexto(alunoId, anoLetivoId, periodoId))
    }

    override suspend fun selecionarAnoLetivo(anoLetivoId: Long, periodoId: Long) {
        prefs.atualizar { it.copy(alunoId = anoLetivoId, anoLetivoId = periodoId) }
    }

    override suspend fun selecionarPeriodo(periodoId: Long) {
        prefs.atualizar { it.copy(alunoId = periodoId) }
    }
}
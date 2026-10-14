package app.jammes.boletim.data.repository

import app.jammes.boletim.data.local.preferences.ConfiguracoesPreferences
import app.jammes.boletim.domain.model.Configuracoes
import app.jammes.boletim.domain.model.EstiloDasAbas
import app.jammes.boletim.domain.model.Tema
import app.jammes.boletim.domain.repository.ConfiguracoesRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow

class ConfiguracoesRepositoryImpl @Inject constructor(
    private val prefs: ConfiguracoesPreferences
) : ConfiguracoesRepository {

    override val configuracoes: Flow<Configuracoes> = prefs.configuracoes

    override suspend fun salvarTema(tema: Tema) = prefs.salvarTema(tema)

    override suspend fun salvarEstiloDasAbas(estilo: EstiloDasAbas) = prefs.salvarEstiloDasAbas(estilo)
}

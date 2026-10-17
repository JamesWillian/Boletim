package app.jammes.boletim.data.repository

import app.jammes.boletim.data.local.preferences.ConfiguracoesPreferences
import app.jammes.boletim.domain.model.Configuracoes
import app.jammes.boletim.domain.model.EstiloDasAbas
import app.jammes.boletim.domain.model.Tema
import app.jammes.boletim.domain.repository.ConfiguracoesRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import java.time.LocalTime

class ConfiguracoesRepositoryImpl @Inject constructor(
    private val prefs: ConfiguracoesPreferences
) : ConfiguracoesRepository {

    override val configuracoes: Flow<Configuracoes> = prefs.configuracoes

    override suspend fun salvarTema(tema: Tema) = prefs.salvarTema(tema)

    override suspend fun salvarEstiloDasAbas(estilo: EstiloDasAbas) = prefs.salvarEstiloDasAbas(estilo)

    override suspend fun salvarBackupAutomatico(ligado: Boolean) = prefs.salvarBackupAutomatico(ligado)

    override suspend fun salvarNotificacoesAtivas(ativas: Boolean) = prefs.salvarNotificacoesAtivas(ativas)

    override suspend fun editarHorariosDasNotificacoes(mudar: (List<LocalTime>) -> List<LocalTime>) =
        prefs.editarHorariosDasNotificacoes(mudar)
}

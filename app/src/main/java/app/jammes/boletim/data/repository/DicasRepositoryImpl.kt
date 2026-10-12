package app.jammes.boletim.data.repository

import app.jammes.boletim.data.local.preferences.DicasPreferences
import app.jammes.boletim.domain.repository.DicasRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow

class DicasRepositoryImpl @Inject constructor(
    private val prefs: DicasPreferences
) : DicasRepository {

    override val dicaDaPrimeiraNota: Flow<Boolean> = prefs.primeiraNota

    override suspend fun pedirDicaDaPrimeiraNota() = prefs.salvarPrimeiraNota(true)

    override suspend fun dispensarDicaDaPrimeiraNota() = prefs.salvarPrimeiraNota(false)
}

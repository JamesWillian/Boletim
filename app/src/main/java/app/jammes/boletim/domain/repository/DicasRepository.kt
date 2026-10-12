package app.jammes.boletim.domain.repository

import kotlinx.coroutines.flow.Flow

/** As dicas que aparecem uma vez só e somem quando a pessoa dispensa. */
interface DicasRepository {

    /** A dica "Lance a primeira nota" está pedida e ainda não foi dispensada. */
    val dicaDaPrimeiraNota: Flow<Boolean>

    /** O fim do onboarding pede a dica: o boletim mostra ela da próxima vez que abrir. */
    suspend fun pedirDicaDaPrimeiraNota()

    suspend fun dispensarDicaDaPrimeiraNota()
}

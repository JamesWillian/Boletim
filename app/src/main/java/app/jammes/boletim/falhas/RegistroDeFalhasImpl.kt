package app.jammes.boletim.falhas

import app.jammes.boletim.domain.repository.RegistroDeFalhas
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import jakarta.inject.Inject

/** O registro no Crashlytics, que envia quando o app abre de novo. No debug, a coleta fica desligada. */
class RegistroDeFalhasImpl @Inject constructor() : RegistroDeFalhas {

    override fun registrar(erro: Throwable) {
        Firebase.crashlytics.recordException(erro)
    }
}

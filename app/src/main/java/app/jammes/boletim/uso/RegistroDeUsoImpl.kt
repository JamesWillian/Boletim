package app.jammes.boletim.uso

import app.jammes.boletim.domain.repository.RegistroDeUso
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import jakarta.inject.Inject

/** O registro no Google Analytics, que envia em lotes. No debug, a coleta fica desligada. */
class RegistroDeUsoImpl @Inject constructor() : RegistroDeUso {

    override fun telaAberta(nome: String) {
        Firebase.analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, nome)
        }
    }
}

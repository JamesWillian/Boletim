package app.jammes.boletim.data.review

import android.content.Context
import android.util.Log
import app.jammes.boletim.data.billing.TelaEmPrimeiroPlano
import app.jammes.boletim.data.local.preferences.AvaliacaoNaLojaPreferences
import app.jammes.boletim.domain.repository.AvaliacaoNaLoja
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * O cartão de avaliação da Google Play, pela In-App Review API. Ela tem uma cota própria: se já mostrou
 * o cartão há pouco tempo, responde que deu certo e não mostra nada. Por isso nenhum botão pode chamar
 * esse cartão, e a linha "Avaliar o app" do Perfil abre a página do app na loja.
 *
 * https://developer.android.com/guide/playcore/in-app-review
 */
@Singleton
class AvaliacaoNaGooglePlay @Inject constructor(
    @ApplicationContext context: Context,
    private val tela: TelaEmPrimeiroPlano,
    private val prefs: AvaliacaoNaLojaPreferences,
) : AvaliacaoNaLoja {

    private val manager: ReviewManager = ReviewManagerFactory.create(context)

    override suspend fun ultimoPedido(): LocalDate? = prefs.ultimoPedido.first()

    override suspend fun salvarUltimoPedido(dia: LocalDate) = prefs.salvarUltimoPedido(dia)

    override suspend fun mostrarCartao(): Boolean = try {
        val cartao = manager.requestReview()
        // A tela é lida só agora: a pessoa pode ter saído do app enquanto a Play respondia
        val activity = tela.activity
        if (activity != null) manager.launchReview(activity, cartao)
        activity != null
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        // Um cartão que não abre não pode atrapalhar quem acabou de lançar a nota: fica só no log
        Log.w(TAG, "A Play não mostrou o cartão de avaliação", e)
        false
    }

    private companion object {
        const val TAG = "AvaliacaoNaLoja"
    }
}

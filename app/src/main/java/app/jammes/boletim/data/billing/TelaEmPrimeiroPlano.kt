package app.jammes.boletim.data.billing

import android.app.Activity
import android.app.Application
import android.os.Bundle
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.lang.ref.WeakReference

/**
 * A Activity em primeiro plano. A compra da Play só abre por cima de uma tela, e assim a loja chega
 * nela sem a Activity passar pelo ViewModel nem pelo domínio. A BoletimApp registra no onCreate.
 *
 * Fica numa WeakReference: guardar a Activity em si prenderia ela na memória depois de fechada.
 */
@Singleton
class TelaEmPrimeiroPlano @Inject constructor() : Application.ActivityLifecycleCallbacks {

    @Volatile
    private var atual: WeakReference<Activity>? = null

    /** A tela que está à vista; null com o app em segundo plano. */
    val activity: Activity?
        get() = atual?.get()

    override fun onActivityResumed(activity: Activity) {
        atual = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        // Só sai se for ela: na troca, a próxima já pode ter entrado
        if (atual?.get() === activity) atual = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}

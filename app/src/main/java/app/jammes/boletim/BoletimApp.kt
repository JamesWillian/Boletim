package app.jammes.boletim

import android.app.Application
import app.jammes.boletim.avisos.criarCanalDosAvisos
import app.jammes.boletim.data.billing.TelaEmPrimeiroPlano
import app.jammes.boletim.domain.usecase.AcompanharHorariosDosAvisos
import dagger.hilt.android.HiltAndroidApp
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class BoletimApp: Application() {

    @Inject lateinit var acompanharHorariosDosAvisos: AcompanharHorariosDosAvisos
    @Inject lateinit var telaEmPrimeiroPlano: TelaEmPrimeiroPlano

    // Vive o tanto que o processo vive: o despertador dos avisos acompanha as configurações
    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // A Play abre a compra do Pro e o cartão de avaliação por cima da tela que estiver à vista
        registerActivityLifecycleCallbacks(telaEmPrimeiroPlano)
        criarCanalDosAvisos(this)
        // Marca o próximo horário agora e a cada mudança na chave ou nos horários
        escopo.launch { acompanharHorariosDosAvisos() }
    }
}

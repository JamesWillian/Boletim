package app.jammes.boletim

import android.app.Application
import app.jammes.boletim.data.local.DadosDeTesteCallback
import app.jammes.boletim.domain.repository.ContextoRepository
import dagger.hilt.android.HiltAndroidApp
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltAndroidApp
class BoletimApp: Application() {

    @Inject lateinit var contextoRepo: ContextoRepository

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) usarContextoDeTeste()
    }

    // Sem Contexto o AppRoot fica no Vazio, nenhuma tela consulta o Room e o banco nem é criado,
    // então o DadosDeTesteCallback não rodaria. Com o Contexto salvo, a primeira consulta cria o
    // banco já populado.
    private fun usarContextoDeTeste() {
        CoroutineScope(Dispatchers.IO).launch {
            if (contextoRepo.contexto.first() == null) {
                with(DadosDeTesteCallback.CONTEXTO) {
                    contextoRepo.selecionarAluno(alunoId, anoLetivoId, periodoId)
                }
            }
        }
    }
}

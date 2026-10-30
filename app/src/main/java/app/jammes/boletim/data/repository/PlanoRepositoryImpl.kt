package app.jammes.boletim.data.repository

import app.jammes.boletim.data.local.preferences.PlanoPreferences
import app.jammes.boletim.domain.model.Plano
import app.jammes.boletim.domain.repository.PlanoRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * O que o build decide sobre o plano. O PlanoModule lê do BuildConfig; os testes montam à mão.
 *
 * @property limitesLigados desligados, todo mundo é Pro. O release fica assim enquanto não dá para
 * comprar, para ninguém esbarrar num limite sem saída.
 * @property proDeTestePermitido a chave "Pro de teste" das Configurações vale. Só no debug.
 */
data class ChavesDoPlano(
    val limitesLigados: Boolean,
    val proDeTestePermitido: Boolean,
)

/** Ainda não dá para comprar o Pro: com os limites ligados, só a chave de teste do debug dá ele. */
class PlanoRepositoryImpl @Inject constructor(
    private val prefs: PlanoPreferences,
    private val chaves: ChavesDoPlano,
) : PlanoRepository {

    override val plano: Flow<Plano> = prefs.proDeTeste
        .map { proDeTeste -> planoDe(chaves, proDeTeste) }
        .distinctUntilChanged()

    override suspend fun salvarProDeTeste(ligado: Boolean) {
        if (chaves.proDeTestePermitido) prefs.salvarProDeTeste(ligado)
    }
}

/**
 * O plano que vale pelas [chaves] e pela chave de teste. Fora do debug ela não conta, nem quando um
 * backup do debug traz ela de volta.
 */
internal fun planoDe(chaves: ChavesDoPlano, proDeTeste: Boolean): Plano = when {
    !chaves.limitesLigados -> Plano.PRO
    chaves.proDeTestePermitido && proDeTeste -> Plano.PRO
    else -> Plano.GRATIS
}

package app.jammes.boletim.data.repository

import app.jammes.boletim.data.local.preferences.PlanoPreferences
import app.jammes.boletim.domain.model.Plano
import app.jammes.boletim.domain.repository.PlanoRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

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

/**
 * O plano que vale. Com os limites ligados, o Pro vem da compra confirmada pela Play, que a loja guarda
 * nas preferências, ou, só no debug, da chave de teste.
 */
class PlanoRepositoryImpl @Inject constructor(
    private val prefs: PlanoPreferences,
    private val chaves: ChavesDoPlano,
) : PlanoRepository {

    override val plano: Flow<Plano> = combine(
        prefs.proDeTeste,
        prefs.comprado,
    ) { proDeTeste, comprado ->
        planoDe(chaves, proDeTeste, comprado)
    }.distinctUntilChanged()

    override suspend fun salvarProDeTeste(ligado: Boolean) {
        if (chaves.proDeTestePermitido) prefs.salvarProDeTeste(ligado)
    }
}

/**
 * O plano que vale pelas [chaves], pela chave de teste e pela compra na Play ([comprado]). Fora do
 * debug a chave de teste não conta, nem quando um backup do debug traz ela de volta.
 */
internal fun planoDe(
    chaves: ChavesDoPlano,
    proDeTeste: Boolean,
    comprado: Boolean,
): Plano = when {
    !chaves.limitesLigados -> Plano.PRO
    chaves.proDeTestePermitido && proDeTeste -> Plano.PRO
    comprado -> Plano.PRO
    else -> Plano.GRATIS
}

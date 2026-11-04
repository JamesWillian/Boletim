package app.jammes.boletim.domain.usecase

import app.jammes.boletim.domain.repository.AvaliacaoNaLoja
import app.jammes.boletim.domain.repository.AvaliacaoRepository
import jakarta.inject.Inject
import java.time.LocalDate

/** Quantas notas lançadas, somando todas as matérias, até o app pedir a avaliação na loja. */
internal const val NOTAS_ANTES_DO_PEDIDO = 5

/** Quanto tempo depois de um pedido ele pode se repetir: o app não sabe se a pessoa avaliou. */
internal const val MESES_ENTRE_PEDIDOS = 4L

/** Se a pessoa já lançou notas o bastante para opinar e o último pedido não foi há pouco tempo. */
internal fun podePedirAvaliacaoNaLoja(notasLancadas: Int, ultimoPedido: LocalDate?, hoje: LocalDate): Boolean =
    notasLancadas >= NOTAS_ANTES_DO_PEDIDO &&
        (ultimoPedido == null || !hoje.isBefore(ultimoPedido.plusMonths(MESES_ENTRE_PEDIDOS)))

/**
 * Mostra o cartão de avaliação da loja, se já for a hora. Só guarda o dia quando a loja atendeu: sem
 * resposta dela, a próxima nota tenta de novo.
 */
class PedirAvaliacaoNaLoja @Inject constructor(
    private val avaliacaoRepository: AvaliacaoRepository,
    private val avaliacaoNaLoja: AvaliacaoNaLoja,
) {
    suspend operator fun invoke(hoje: LocalDate = LocalDate.now()) {
        val pode = podePedirAvaliacaoNaLoja(
            notasLancadas = avaliacaoRepository.contarComNota(),
            ultimoPedido = avaliacaoNaLoja.ultimoPedido(),
            hoje = hoje,
        )
        if (pode && avaliacaoNaLoja.mostrarCartao()) avaliacaoNaLoja.salvarUltimoPedido(hoje)
    }
}

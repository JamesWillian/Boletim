package app.jammes.boletim.domain.repository

import java.time.LocalDate

/**
 * O pedido para a pessoa avaliar o app na loja. O cartão é da loja, e é ela quem decide se ele
 * aparece: pode não mostrar nada, sem avisar, e nunca conta se a pessoa avaliou.
 */
interface AvaliacaoNaLoja {

    /** O dia do último pedido; null se o app nunca pediu. */
    suspend fun ultimoPedido(): LocalDate?

    suspend fun salvarUltimoPedido(dia: LocalDate)

    /**
     * Pede o cartão à loja e espera ele fechar. false: a loja não respondeu, ou o app saiu da tela
     * enquanto isso. O pedido não aconteceu, então vale tentar de novo depois.
     */
    suspend fun mostrarCartao(): Boolean
}

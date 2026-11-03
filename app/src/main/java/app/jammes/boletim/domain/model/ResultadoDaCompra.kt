package app.jammes.boletim.domain.model

/** Como terminou uma compra do Pro. Só a [Comprado] libera o Pro. */
sealed interface ResultadoDaCompra {
    data object Comprado : ResultadoDaCompra
    data object Pendente : ResultadoDaCompra // boleto ou pagamento lento: o Pro libera quando a loja confirmar
    data object Cancelado : ResultadoDaCompra
    data object Indisponivel : ResultadoDaCompra // sem a loja, sem o produto à venda ou um erro dela
}

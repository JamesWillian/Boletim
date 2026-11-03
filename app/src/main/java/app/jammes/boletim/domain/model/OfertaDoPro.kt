package app.jammes.boletim.domain.model

/** O preço do Pro como a loja informa. Numa promoção, vêm também o [precoCheio] e o [desconto], em %. */
data class OfertaDoPro(
    val preco: String,
    val precoCheio: String? = null,
    val desconto: Int? = null,
)

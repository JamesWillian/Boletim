package app.jammes.boletim.presentation.ui.plano

/** Embaixo do botão de criar matéria, no grátis: "Restam 2 grátis"; com uma só, "Resta 1 grátis". */
internal fun textoDasQueRestam(restam: Int): String =
    if (restam == 1) "Resta 1 grátis" else "Restam $restam grátis"

/** O que o leitor de tela diz que o botão de criar matéria faz; sem nenhuma grátis, ele abre a folha do Pro. */
internal fun rotuloDaMateriaNova(soNoPro: Boolean): String =
    if (soNoPro) "Ver o Pro, que libera mais matérias" else "Criar uma matéria"

/** O que a folha do Pro diz embaixo do botão, depois de uma compra ou de um "Restaurar compra". */
internal fun textoDoAviso(aviso: AvisoDaLoja): String = when (aviso) {
    AvisoDaLoja.PENDENTE -> "Pagamento pendente. O Pro libera assim que a Google Play confirmar."
    AvisoDaLoja.INDISPONIVEL -> "A loja da Google Play não está disponível agora. Tente de novo mais tarde."
    AvisoDaLoja.SEM_COMPRA ->
        "Nenhuma compra do Pro nesta conta da Google Play. Se comprou com outra, troque a conta na Play Store."
    AvisoDaLoja.SEM_RESPOSTA -> "Não deu para conferir com a Google Play agora. Tente de novo mais tarde."
}

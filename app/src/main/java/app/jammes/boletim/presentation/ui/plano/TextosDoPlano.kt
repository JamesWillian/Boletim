package app.jammes.boletim.presentation.ui.plano

/** Embaixo do botão de criar matéria, no grátis: "Restam 2 grátis"; com uma só, "Resta 1 grátis". */
internal fun textoDasQueRestam(restam: Int): String =
    if (restam == 1) "Resta 1 grátis" else "Restam $restam grátis"

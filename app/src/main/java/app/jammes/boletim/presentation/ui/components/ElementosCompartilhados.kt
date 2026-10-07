package app.jammes.boletim.presentation.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape

// Elementos compartilhados entre telas: o card da disciplina no Boletim "abre" e vira o resumo
// no detalhe, e o selo do ícone voa de um para o outro. O AppNavHost fornece os dois escopos;
// fora dele (num @Preview, por exemplo) os modificadores abaixo não fazem nada.

/** O [SharedTransitionScope] do AppNavHost: onde as duas pontas de um elemento se encontram. */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalEscopoCompartilhado = staticCompositionLocalOf<SharedTransitionScope?> { null }

/** A animação de entrada e saída da tela atual, que o NavHost dá a cada `composable`. */
val LocalEscopoDaTela = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

/** Duração da troca de tela, em ms. A transformação do card dura o mesmo, para terminarem juntas. */
const val DURACAO_TRANSICAO = 400

/**
 * Quanto do começo da troca é só da saída, em ms: a tela (ou o conteúdo do card) que sai some
 * nesse tempo, e só depois a nova aparece. Sem misturar as duas no meio, é o "fade through" do Material.
 */
const val DURACAO_SAIDA = 120

/** As chaves que ligam as duas pontas: a mesma chave nas duas telas é o mesmo elemento. */
object ChavesCompartilhadas {
    fun card(disciplinaId: Long) = "card-disciplina-$disciplinaId"
    fun selo(disciplinaId: Long) = "selo-disciplina-$disciplinaId"
}

@OptIn(ExperimentalSharedTransitionApi::class)
private val transformacao = BoundsTransform { _, _ ->
    tween(durationMillis = DURACAO_TRANSICAO, easing = FastOutSlowInEasing)
}

/**
 * Contêineres diferentes que representam a mesma coisa (o card e o resumo): o contorno se
 * transforma de um no outro enquanto o conteúdo de um some e o do outro aparece. Os dois fades
 * começam juntos (o fundo do card nunca some no meio), mas o que sai é mais curto, para o texto
 * antigo, esticado pela transformação, não ficar visível por cima do novo.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.limitesCompartilhados(chave: String, forma: Shape): Modifier {
    val escopo = LocalEscopoCompartilhado.current ?: return this
    val tela = LocalEscopoDaTela.current ?: return this
    return with(escopo) {
        this@limitesCompartilhados.sharedBounds(
            sharedContentState = rememberSharedContentState(chave),
            animatedVisibilityScope = tela,
            enter = fadeIn(tween(DURACAO_TRANSICAO * 3 / 4)),
            exit = fadeOut(tween(DURACAO_TRANSICAO / 2)),
            boundsTransform = transformacao,
            clipInOverlayDuringTransition = OverlayClip(forma),
        )
    }
}

/** O mesmo elemento nas duas telas (o selo do ícone): ele só muda de lugar e de tamanho. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.elementoCompartilhado(chave: String): Modifier {
    val escopo = LocalEscopoCompartilhado.current ?: return this
    val tela = LocalEscopoDaTela.current ?: return this
    return with(escopo) {
        this@elementoCompartilhado.sharedElement(
            sharedContentState = rememberSharedContentState(chave),
            animatedVisibilityScope = tela,
            boundsTransform = transformacao,
        )
    }
}

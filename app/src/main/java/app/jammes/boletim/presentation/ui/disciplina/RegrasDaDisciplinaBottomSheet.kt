package app.jammes.boletim.presentation.ui.disciplina

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.jammes.boletim.domain.model.TipoArredondamento
import app.jammes.boletim.domain.model.TipoMedia
import app.jammes.boletim.presentation.ui.anoletivo.BlocoNumerico
import app.jammes.boletim.presentation.ui.anoletivo.Cartao
import app.jammes.boletim.presentation.ui.anoletivo.Divisoria
import app.jammes.boletim.presentation.ui.anoletivo.FAIXA_DA_FREQUENCIA
import app.jammes.boletim.presentation.ui.anoletivo.FAIXA_DA_MEDIA
import app.jammes.boletim.presentation.ui.anoletivo.FormularioAnoLetivo
import app.jammes.boletim.presentation.ui.anoletivo.LinhaQueAbre
import app.jammes.boletim.presentation.ui.anoletivo.OpcoesDeEscolha
import app.jammes.boletim.presentation.ui.anoletivo.detalheDoArredondamento
import app.jammes.boletim.presentation.ui.anoletivo.detalheDoCalculo
import app.jammes.boletim.presentation.ui.anoletivo.exemploDoArredondamento
import app.jammes.boletim.presentation.ui.anoletivo.nomeDoArredondamento
import app.jammes.boletim.presentation.ui.anoletivo.resumoDoArredondamento
import app.jammes.boletim.presentation.ui.anoletivo.resumoDoCalculo
import app.jammes.boletim.presentation.ui.boletim.formatarMedia
import app.jammes.boletim.presentation.ui.components.SeloDisciplina
import app.jammes.boletim.presentation.ui.theme.Espacos
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * As regras de média só desta disciplina, que valem no lugar das do ano letivo: as mesmas quatro dos
 * ajustes do ano, com os mesmos passos. A média e a frequência mínimas mudam pelo − e +; o cálculo e o
 * arredondamento abrem ali mesmo, um de cada vez, e um toque na opção já escolhe.
 *
 * Abre com as que valem agora. Aplicar leva as regras para o formulário da tela, e quem grava é o
 * Salvar dela. Iguais às do ano, não há regra própria: a disciplina volta a seguir as do ano, e o
 * "Redefinir" faz isso de uma vez.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RegrasDaDisciplinaBottomSheet(
    regras: FormularioAnoLetivo.Regras, // as que valem agora: as próprias, ou as do ano
    doAno: FormularioAnoLetivo.Regras,
    disciplina: String, // o nome, para dizer para quem as regras valem
    dicaDaFrequencia: (frequenciaMinima: Double) -> String, // com o total de aulas, quantas faltas cabem
    onAplicar: (FormularioAnoLetivo.Regras?) -> Unit, // null: a disciplina segue as do ano
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val rolagem = rememberScrollState()

    // rememberSaveable: o que já foi mudado sobrevive a girar a tela
    var rascunho by rememberSaveable { mutableStateOf(regras) }
    var aberta by rememberSaveable { mutableStateOf<Escolha?>(null) }
    val iguaisAsDoAno = rascunho == doAno

    // A escolha abre embaixo da linha dela: enquanto cresce, a rolagem vai junto até o fim, senão o
    // que acabou de abrir ficaria escondido atrás dos botões
    LaunchedEffect(aberta) {
        if (aberta != null) {
            withTimeoutOrNull(DURACAO_PARA_ACOMPANHAR) {
                snapshotFlow { rolagem.maxValue }.collect { rolagem.scrollTo(it) }
            }
        }
    }

    // Esconde com a animação e só então avisa a tela, que tira o sheet da composição
    fun fechar() {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) onDismiss()
        }
    }

    fun alternar(escolha: Escolha) {
        aberta = if (aberta == escolha) null else escolha
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        tonalElevation = 2.dp
    ) {
        // O cabeçalho e os botões ficam fixos; só o meio rola. O Redefinir fica na linha do título,
        // que tem sempre a altura dele: ao aparecer ou sumir, nada pula
        Row(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .padding(start = 20.dp, end = Espacos.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Regras de média",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            AnimatedVisibility(visible = !iguaisAsDoAno, enter = fadeIn(), exit = fadeOut()) {
                TextButton(onClick = { rascunho = doAno }) { Text("Redefinir") }
            }
        }
        Text(
            text = situacaoDasRegras(iguaisAsDoAno, disciplina),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(horizontal = 20.dp)
                // O leitor de tela avisa quando elas passam a ser só desta disciplina, ou voltam
                .semantics { liveRegion = LiveRegionMode.Polite },
        )

        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rolagem)
                .padding(horizontal = 20.dp)
                .padding(top = Espacos.l, bottom = Espacos.l),
            verticalArrangement = Arrangement.spacedBy(Espacos.s),
        ) {
            // Os dois números que decidem as cores da disciplina, lado a lado e da mesma altura
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(Espacos.s),
            ) {
                BlocoNumerico(
                    rotulo = "Média mínima",
                    oQue = "a média mínima",
                    valor = rascunho.mediaMinima,
                    formatar = { formatarMedia(it) },
                    dica = "Nota para passar",
                    podeDiminuir = rascunho.mediaMinima > FAIXA_DA_MEDIA.start,
                    podeAumentar = rascunho.mediaMinima < FAIXA_DA_MEDIA.endInclusive,
                    onMudar = { sentido -> rascunho = rascunho.comMediaMinima(sentido) },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                BlocoNumerico(
                    rotulo = "Frequência mínima",
                    oQue = "a frequência mínima",
                    valor = rascunho.frequenciaMinima,
                    formatar = { "${formatarNumero(it)}%" },
                    dica = dicaDaFrequencia(rascunho.frequenciaMinima),
                    podeDiminuir = rascunho.frequenciaMinima > FAIXA_DA_FREQUENCIA.start,
                    podeAumentar = rascunho.frequenciaMinima < FAIXA_DA_FREQUENCIA.endInclusive,
                    onMudar = { sentido -> rascunho = rascunho.comFrequenciaMinima(sentido) },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }

            Cartao {
                val calculoAberto = aberta == Escolha.CALCULO
                LinhaQueAbre(
                    titulo = "Cálculo da média",
                    resumo = resumoDoCalculo(rascunho.tipoMedia),
                    rotuloDoClique = if (calculoAberto) "Fechar o cálculo da média" else "Escolher o cálculo da média",
                    onClick = { alternar(Escolha.CALCULO) },
                    fim = { SetaDaEscolha(aberta = calculoAberto) },
                ) {
                    SeloDisciplina(
                        cor = MaterialTheme.colorScheme.primary,
                        icone = Icons.Outlined.Functions,
                        tamanho = 36.dp,
                    )
                }
                AnimatedVisibility(visible = calculoAberto) {
                    OpcoesDeEscolha(
                        opcoes = TipoMedia.entries,
                        selecionada = rascunho.tipoMedia,
                        nome = { it.displayName },
                        detalhe = { detalheDoCalculo(it) },
                        // Um toque já escolhe e fecha, como o sheet de escolha dos ajustes
                        onEscolher = {
                            rascunho = rascunho.copy(tipoMedia = it)
                            aberta = null
                        },
                        modifier = Modifier.padding(start = Espacos.s, end = Espacos.s, bottom = Espacos.s),
                    )
                }

                Divisoria()

                val arredondamentoAberto = aberta == Escolha.ARREDONDAMENTO
                LinhaQueAbre(
                    titulo = "Arredondamento",
                    resumo = resumoDoArredondamento(rascunho.arredondamento),
                    rotuloDoClique = if (arredondamentoAberto) "Fechar o arredondamento" else "Escolher o arredondamento",
                    onClick = { alternar(Escolha.ARREDONDAMENTO) },
                    fim = { SetaDaEscolha(aberta = arredondamentoAberto) },
                ) {
                    SeloDisciplina(
                        cor = MaterialTheme.colorScheme.primary,
                        icone = Icons.Outlined.SwapVert,
                        tamanho = 36.dp,
                    )
                }
                AnimatedVisibility(visible = arredondamentoAberto) {
                    OpcoesDeEscolha(
                        opcoes = TipoArredondamento.entries,
                        selecionada = rascunho.arredondamento,
                        nome = { nomeDoArredondamento(it) },
                        detalhe = { detalheDoArredondamento(it) },
                        exemplo = { exemploDoArredondamento(it) },
                        onEscolher = {
                            rascunho = rascunho.copy(arredondamento = it)
                            aberta = null
                        },
                        modifier = Modifier.padding(start = Espacos.s, end = Espacos.s, bottom = Espacos.s),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { fechar() },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Cancelar") }

            Button(
                onClick = {
                    onAplicar(regrasEscolhidas(rascunho, doAno))
                    fechar()
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Aplicar") }
        }
    }
}

/** O que abre embaixo da linha dela, um de cada vez. */
private enum class Escolha { CALCULO, ARREDONDAMENTO }

/** A seta de uma escolha que abre ali mesmo: para baixo fechada, para cima aberta. */
@Composable
private fun SetaDaEscolha(aberta: Boolean) {
    Icon(
        imageVector = if (aberta) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// Um pouco mais que a animação de abrir uma escolha (expandVertically, com mola)
private const val DURACAO_PARA_ACOMPANHAR = 600L

/** Embaixo do título: se as regras são as do ano ou só desta disciplina, e o que isso quer dizer. */
internal fun situacaoDasRegras(iguaisAsDoAno: Boolean, disciplina: String): String =
    if (iguaisAsDoAno) {
        "Iguais às do ano letivo. O que mudar aqui vale só para $disciplina."
    } else {
        "Só para $disciplina. As outras disciplinas seguem as do ano letivo."
    }

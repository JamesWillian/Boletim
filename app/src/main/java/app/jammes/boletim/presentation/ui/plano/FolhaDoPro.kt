package app.jammes.boletim.presentation.ui.plano

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.jammes.boletim.R
import app.jammes.boletim.domain.model.ALUNOS_NO_GRATIS
import app.jammes.boletim.domain.model.MATERIAS_PROPRIAS_NO_GRATIS
import app.jammes.boletim.domain.model.Plano
import app.jammes.boletim.presentation.ui.theme.BoletimColors
import app.jammes.boletim.presentation.ui.theme.BoletimTheme
import app.jammes.boletim.presentation.ui.theme.Espacos
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** O preço do Pro como a loja informa. Numa promoção, vêm também o [precoCheio] e o [desconto], em %. */
data class OfertaDoPro(
    val preco: String,
    val precoCheio: String? = null,
    val desconto: Int? = null,
)

/**
 * A folha do Pro, que abre quando um limite do grátis barra algo: a arte, a frase, o grátis lado a
 * lado com o Pro e a compra, que fica sempre à vista embaixo.
 *
 * O botão desbloqueia o Pro pelo [PlanoViewModel], e com o Pro valendo a folha fecha sozinha.
 * [onFechar] avisa a tela que ela saiu, por qualquer caminho: o X, o arrastar, o voltar ou o Pro.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolhaDoPro(
    onFechar: () -> Unit,
    viewModel: PlanoViewModel = hiltViewModel(),
) {
    val plano by viewModel.plano.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val escopo = rememberCoroutineScope()
    val haptico = LocalHapticFeedback.current
    // O efeito do Pro dura a animação inteira: ele chama sempre o onFechar atual
    val fecharAtual by rememberUpdatedState(onFechar)

    // O X fecha com a mesma animação de arrastar a folha para baixo
    val fechar: () -> Unit = {
        escopo.launch { sheetState.hide() }.invokeOnCompletion { if (!sheetState.isVisible) fecharAtual() }
    }
    LaunchedEffect(plano) {
        if (plano == Plano.PRO) {
            sheetState.hide()
            fecharAtual()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onFechar,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        tonalElevation = 2.dp,
        dragHandle = null, // a alça fica na arte, em branco sobre o azul
    ) {
        ConteudoDaFolha(
            // Sem a loja ainda, não há preço: o cartão mostra só o plano
            oferta = null,
            onDesbloquear = {
                haptico.performHapticFeedback(HapticFeedbackType.Confirm)
                viewModel.desbloquear()
            },
            // Nem compra para restaurar
            onRestaurar = null,
            onFechar = fechar,
        )
    }
}

@Composable
private fun ConteudoDaFolha(
    oferta: OfertaDoPro?, // null enquanto a loja não informa o preço
    onDesbloquear: () -> Unit,
    onRestaurar: (() -> Unit)?, // null: sem o "Restaurar compra"
    onFechar: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        // Numa tela baixa ou com a fonte grande, só essa parte rola: a compra continua à vista
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ArteDoPro(onFechar = onFechar)
            Frase(Modifier.padding(start = Espacos.xl, end = Espacos.xl, top = Espacos.xl, bottom = Espacos.l))
            Comparativo(Modifier.padding(horizontal = Espacos.l))
            Novidades(Modifier.padding(top = Espacos.m))
        }
        Compra(
            oferta = oferta,
            onDesbloquear = onDesbloquear,
            onRestaurar = onRestaurar,
            modifier = Modifier.padding(start = Espacos.l, end = Espacos.l, top = Espacos.l),
        )
    }
}

// Arte ----------------------------------------------------------------------------------------

/** O topo: a folha do ícone do app com o selo dourado do Pro, sobre o azul das boas-vindas. */
@Composable
private fun ArteDoPro(onFechar: () -> Unit) {
    val gradiente = if (BoletimTheme.escuro) BoletimColors.GradienteEscuro else BoletimColors.GradienteClaro
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp)
            .clipToBounds()
            .background(Brush.linearGradient(gradiente)),
        contentAlignment = Alignment.Center,
    ) {
        // Os círculos claros das boas-vindas, como luz batendo no azul
        Box(
            Modifier.align(Alignment.TopEnd).offset(x = 80.dp, y = (-96).dp).size(240.dp)
                .clip(CircleShape).background(Color.White.copy(alpha = 0.07f))
        )
        Box(
            Modifier.align(Alignment.BottomStart).offset(x = (-72).dp, y = 84.dp).size(190.dp)
                .clip(CircleShape).background(Color.White.copy(alpha = 0.07f))
        )
        Box(
            Modifier.size(220.dp)
                .background(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.2f), Color.Transparent)))
        )
        Brilhos(Modifier.matchParentSize())

        // É ilustração: quem lê a tela ouve a frase logo abaixo
        Box(Modifier.offset(x = (-8).dp, y = 6.dp).clearAndSetSemantics {}) {
            // O desenho do ícone tem 108 e a folha, 44: assim ela fica com uns 96dp de altura
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.requiredSize(236.dp),
            )
            SeloPro(Modifier.align(Alignment.Center).offset(x = 32.dp, y = 42.dp))
        }

        Box(
            Modifier.align(Alignment.TopCenter).padding(top = Espacos.s).size(width = 32.dp, height = 4.dp)
                .clip(CircleShape).background(Color.White.copy(alpha = 0.4f))
        )
        IconButton(onClick = onFechar, modifier = Modifier.align(Alignment.TopEnd).padding(Espacos.xs)) {
            Box(
                Modifier.size(32.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Fechar", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/** O selo dourado do Pro, como o carimbo de um certificado no canto da folha. */
@Composable
private fun SeloPro(modifier: Modifier = Modifier) {
    // Faz parte do desenho: não cresce com a fonte do aparelho
    val tamanhoDoTexto = with(LocalDensity.current) { 13.dp.toSp() }
    Box(modifier.size(54.dp).rotate(-12f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val raio = size.minDimension / 2
            val ouro = Brush.linearGradient(OURO, start = Offset.Zero, end = Offset(size.width, size.height))
            val sombra = center + Offset(0f, 3.dp.toPx())
            drawCircle(
                Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.3f), Color.Transparent), center = sombra, radius = raio * 1.15f),
                radius = raio * 1.15f,
                center = sombra,
            )
            // A borda recortada: bolinhas em volta do disco
            val raioDaBolinha = raio * 0.17f
            val distancia = raio - raioDaBolinha
            repeat(BOLINHAS_DO_SELO) { i ->
                val angulo = 2 * PI * i / BOLINHAS_DO_SELO
                val bolinha = center + Offset((cos(angulo) * distancia).toFloat(), (sin(angulo) * distancia).toFloat())
                drawCircle(ouro, radius = raioDaBolinha, center = bolinha)
            }
            drawCircle(ouro, radius = distancia)
            // O anel de dentro, como o relevo de uma medalha
            drawCircle(TINTA_DO_OURO.copy(alpha = 0.45f), radius = raio * 0.7f, style = Stroke(width = 1.2.dp.toPx()))
        }
        Text(
            text = "PRO",
            fontSize = tamanhoDoTexto,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = TINTA_DO_OURO,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

/** Brilhos de quatro pontas em volta da folha. */
@Composable
private fun Brilhos(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        brilho(-92f, -40f, 10f, BoletimColors.Brand)
        brilho(84f, -56f, 6f, Color.White.copy(alpha = 0.85f))
        brilho(108f, 18f, 7f, BoletimColors.Brand)
        brilho(-112f, 36f, 5f, Color.White.copy(alpha = 0.6f))
    }
}

// Em dp, a partir do centro
private fun DrawScope.brilho(dx: Float, dy: Float, raio: Float, cor: Color) {
    val x = center.x + dx.dp.toPx()
    val y = center.y + dy.dp.toPx()
    val r = raio.dp.toPx()
    val forma = Path().apply {
        moveTo(x, y - r)
        quadraticTo(x, y, x + r, y)
        quadraticTo(x, y, x, y + r)
        quadraticTo(x, y, x - r, y)
        quadraticTo(x, y, x, y - r)
        close()
    }
    drawPath(forma, cor)
}

// Frase ---------------------------------------------------------------------------------------

@Composable
private fun Frase(modifier: Modifier = Modifier) {
    val dourado = dourado()
    Text(
        text = buildAnnotatedString {
            // Sempre em duas linhas, com "do Boletim" junto na segunda
            append("Todos os recursos\ndo Boletim, ")
            withStyle(SpanStyle(color = dourado)) { append("sem limites") }
        },
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

// Comparativo ---------------------------------------------------------------------------------

/**
 * Uma diferença entre os planos. No grátis, um limite ([noGratis], às vezes com uma [unidade], como em
 * "1 por ano") ou nada; no Pro, sem limite ou incluído.
 */
private class Diferenca(
    val icone: ImageVector,
    val texto: String,
    val noGratis: Int? = null,
    val unidade: String? = null,
)

// A ordem é fixa, venha a folha de onde vier
private val DIFERENCAS = listOf(
    Diferenca(Icons.Outlined.Groups, "Alunos", noGratis = ALUNOS_NO_GRATIS),
    // O grátis não repete o ano. A exceção de dezembro fica de fora, para a tabela não virar regulamento
    Diferenca(Icons.Outlined.CalendarMonth, "Anos letivos por aluno", noGratis = 1, unidade = "por ano"),
    Diferenca(Icons.Outlined.LibraryAdd, "Matérias criadas por você", noGratis = MATERIAS_PROPRIAS_NO_GRATIS),
    Diferenca(Icons.Outlined.Palette, "Personalizar qualquer matéria"),
)

@Composable
private fun Comparativo(modifier: Modifier = Modifier) {
    val forma = RoundedCornerShape(16.dp)
    Box(modifier.fillMaxWidth()) {
        // A coluna do Pro em destaque, do cabeçalho à última linha
        Row(Modifier.matchParentSize()) {
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.width(LARGURA_DA_COLUNA).fillMaxHeight()
                    .clip(forma).background(corDaFaixa()).border(1.dp, corDaBorda(), forma)
            )
        }
        Column {
            Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Grátis",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(LARGURA_DA_COLUNA),
                )
                Box(Modifier.width(LARGURA_DA_COLUNA), contentAlignment = Alignment.Center) { EtiquetaPro() }
            }
            DIFERENCAS.forEachIndexed { i, diferenca ->
                if (i > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 32.dp, end = LARGURA_DA_COLUNA),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    )
                }
                LinhaDoComparativo(diferenca)
            }
            Spacer(Modifier.height(Espacos.s))
        }
    }
}

@Composable
private fun LinhaDoComparativo(diferenca: Diferenca) {
    val noGratis = diferenca.noGratis?.let { listOfNotNull("$it", diferenca.unidade).joinToString(" ") } ?: "não tem"
    val noPro = if (diferenca.noGratis != null) "sem limite" else "tem"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clearAndSetSemantics { contentDescription = "${diferenca.texto}: no grátis, $noGratis; no Pro, $noPro" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = diferenca.icone,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(Espacos.m))
        Text(
            text = diferenca.texto,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f).padding(vertical = Espacos.s),
        )
        Box(Modifier.width(LARGURA_DA_COLUNA), contentAlignment = Alignment.Center) {
            if (diferenca.noGratis != null) {
                // A unidade, quando tem, vai embaixo do número, menor
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${diferenca.noGratis}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    diferenca.unidade?.let { unidade ->
                        Text(
                            text = unidade,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                Icon(
                    imageVector = Icons.Rounded.Remove,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Box(Modifier.width(LARGURA_DA_COLUNA), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (diferenca.noGratis != null) Icons.Rounded.AllInclusive else Icons.Rounded.Check,
                contentDescription = null,
                tint = dourado(),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/**
 * A etiqueta dourada do Pro: no cabeçalho do comparativo e no fim das linhas que abrem esta folha.
 * [compacta] é a que vai embaixo de um título, no lugar de uma linha de texto pequeno.
 */
@Composable
internal fun EtiquetaPro(modifier: Modifier = Modifier, compacta: Boolean = false) {
    Text(
        text = "PRO",
        style = if (compacta) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        color = TINTA_DO_OURO,
        // Sem espaço, quem encolhe é o vizinho: "PRO" nunca quebra
        maxLines = 1,
        softWrap = false,
        modifier = modifier
            .clip(CircleShape)
            .background(Brush.linearGradient(OURO))
            .padding(horizontal = if (compacta) Espacos.s else Espacos.m, vertical = if (compacta) 1.dp else 3.dp),
    )
}

@Composable
private fun Novidades(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = dourado(), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(Espacos.s))
        Text(
            text = "E mais novidades do Pro a caminho",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Compra --------------------------------------------------------------------------------------

@Composable
private fun Compra(
    oferta: OfertaDoPro?,
    onDesbloquear: () -> Unit,
    onRestaurar: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        CartaoDoPreco(oferta)
        Spacer(Modifier.height(Espacos.m))
        BotaoDourado(texto = "Desbloquear o Pro", onClick = onDesbloquear)
        if (onRestaurar != null) {
            TextButton(onClick = onRestaurar, modifier = Modifier.padding(vertical = Espacos.xs)) {
                Text("Restaurar compra", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Spacer(Modifier.height(Espacos.l))
        }
    }
}

/**
 * O plano e o preço. Na promoção, o cheio vem riscado em cima do novo, com o selo do desconto do lado.
 * O cartão tem a mesma altura nos dois casos, para não pular quando a promoção começa ou acaba.
 */
@Composable
private fun CartaoDoPreco(oferta: OfertaDoPro?) {
    val forma = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ALTURA_DO_CARTAO)
            .clip(forma)
            .background(corDoCartao())
            .border(1.5.dp, corDaBorda(), forma)
            .padding(horizontal = Espacos.l, vertical = Espacos.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Boletim Pro", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = "Pagamento único, sem assinatura",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (oferta != null) {
            Spacer(Modifier.width(Espacos.s))
            Column(horizontalAlignment = Alignment.End) {
                oferta.precoCheio?.let { cheio ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        oferta.desconto?.let { desconto ->
                            SeloDoDesconto(desconto)
                            Spacer(Modifier.width(Espacos.s))
                        }
                        Text(
                            text = cheio,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = TextDecoration.LineThrough,
                        )
                    }
                }
                Text(oferta.preco, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SeloDoDesconto(desconto: Int) {
    val cores = BoletimTheme.coresExtras
    Text(
        text = "$desconto% OFF",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = cores.onSucesso,
        modifier = Modifier
            .clip(CircleShape)
            .background(cores.sucesso)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun BotaoDourado(texto: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(CircleShape)
            .background(Brush.horizontalGradient(OURO))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(texto, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TINTA_DO_OURO)
    }
}

// Cores ---------------------------------------------------------------------------------------

// O dourado de texto e ícone: o da marca no escuro; no claro, um âmbar mais escuro, que tem contraste
// no fundo claro sem puxar para o verde-oliva do tertiary
@Composable
private fun dourado(): Color = if (BoletimTheme.escuro) BoletimColors.Brand else Color(0xFF9A6A00)

@Composable
private fun corDaFaixa(): Color = BoletimColors.Brand.copy(alpha = if (BoletimTheme.escuro) 0.1f else 0.18f)

@Composable
private fun corDaBorda(): Color = BoletimColors.Brand.copy(alpha = if (BoletimTheme.escuro) 0.35f else 0.6f)

// Branco no claro; no escuro, um degrau acima da folha
@Composable
private fun corDoCartao(): Color =
    if (BoletimTheme.escuro) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLowest

private val LARGURA_DA_COLUNA = 64.dp

// Cabe o preço riscado em cima do novo: com ou sem promoção, o cartão fica deste tamanho
private val ALTURA_DO_CARTAO = 72.dp
private const val BOLINHAS_DO_SELO = 16

// O dourado da marca com brilho, e a tinta escura que vai sobre ele
private val OURO = listOf(Color(0xFFF7D774), BoletimColors.Brand, Color(0xFFD9A11F))
private val TINTA_DO_OURO = Color(0xFF3F2E00)

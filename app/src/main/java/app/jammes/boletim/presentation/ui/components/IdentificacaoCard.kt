package app.jammes.boletim.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.jammes.boletim.domain.model.AlunoDomain
import app.jammes.boletim.domain.model.AnoLetivoDomain
import app.jammes.boletim.domain.model.Contexto
import app.jammes.boletim.domain.model.PeriodoDomain
import app.jammes.boletim.presentation.ui.theme.Espacos

/**
 * Topo do app: aluno, ano letivo, o sino das notificações, o botão dos ajustes do ano e o seletor de
 * período. Tocar no aluno chama [onAbrirPerfil], onde dá para trocar de aluno e de ano. Com
 * [temNotificacoesNovas], o sino ganha um ponto azul.
 * A opção "Ano letivo" chama [onSelectPeriodo] com null: o boletim passa a ser o do ano inteiro.
 */
@Composable
fun IdentificacaoCard(
    modifier: Modifier = Modifier,
    contexto: Contexto,
    aluno: AlunoDomain?,
    anosLetivos: List<AnoLetivoDomain> = emptyList(),
    temNotificacoesNovas: Boolean = false,
    onSelectPeriodo: (PeriodoDomain?) -> Unit = {},
    onAbrirNotificacoes: () -> Unit = {},
    onAbrirAjustesAnoLetivo: () -> Unit = {},
    onAbrirPerfil: () -> Unit = {},
) {
    val ano = anosLetivos.find { ano -> ano.id == contexto.anoLetivoId }
    val periodos = ano?.periodo.orEmpty()

    Surface(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = Espacos.m)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = Espacos.s, end = Espacos.xs, top = Espacos.xs, bottom = Espacos.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // O aluno e o ano são um botão só: o toque abre o perfil. O recuo fica dentro do
                // clique, para a onda do toque ter folga em volta do avatar e do nome
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable(onClickLabel = "Abrir o perfil do aluno", onClick = onAbrirPerfil)
                        .padding(Espacos.s),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Avatar(nome = aluno?.nome)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = Espacos.m)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = aluno?.nome.orEmpty(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            // Avisa que o nome abre alguma coisa, como nos seletores de conta
                            Icon(
                                Icons.Filled.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        if (ano != null) {
                            Text(
                                text = descricaoDoAno(ano),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                IconButton(onClick = onAbrirNotificacoes) {
                    BadgedBox(badge = { PontoDoSino(visivel = temNotificacoesNovas) }) {
                        Icon(
                            Icons.Outlined.Notifications,
                            contentDescription = if (temNotificacoesNovas) "Notificações: há novas" else "Notificações",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onAbrirAjustesAnoLetivo) {
                    Icon(
                        Icons.Outlined.CalendarMonth,
                        contentDescription = "Ajustes do ano letivo",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (ano != null && periodos.isNotEmpty()) {
                val opcoes = listOf(OpcaoDoSeletor(completo = "Ano letivo", curto = "Ano")) +
                    periodos.map { opcaoDoPeriodo(it.periodo, ano.tipoPeriodo) }
                SeletorEmPilula(
                    opcoes = opcoes,
                    // -1 quando o período salvo não existe mais: nenhuma opção fica marcada
                    selecionada = if (contexto.periodoId == null) {
                        0
                    } else {
                        periodos.indexOfFirst { it.id == contexto.periodoId }.let { if (it < 0) -1 else it + 1 }
                    },
                    onSelecionar = { i -> onSelectPeriodo(if (i == 0) null else periodos[i - 1]) },
                    modifier = Modifier.padding(horizontal = Espacos.l),
                )
            }
        }
    }
}

/**
 * O ponto azul do sino, quando há notificações novas. Entra e sai crescendo, em vez de piscar. Fora
 * do card, porque lá dentro o AnimatedVisibility da Row seria o escolhido, e ele não serve no badge.
 */
@Composable
private fun PontoDoSino(visivel: Boolean) {
    AnimatedVisibility(visible = visivel, enter = scaleIn(), exit = scaleOut()) {
        Badge(containerColor = MaterialTheme.colorScheme.primary)
    }
}

/** "9º Ano · 2024"; sem série, só o ano. */
internal fun descricaoDoAno(ano: AnoLetivoDomain): String = descricaoDoAno(ano.serie, ano.ano)

internal fun descricaoDoAno(serie: String?, ano: Int): String =
    listOfNotNull(serie?.takeIf { it.isNotBlank() }, ano.toString()).joinToString(" · ")

/** As iniciais do aluno num círculo azul; sem nome, o ícone de pessoa. */
@Composable
internal fun Avatar(nome: String?, modifier: Modifier = Modifier) {
    val iniciais = nome?.let(::iniciais).orEmpty()
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        if (iniciais.isNotEmpty()) {
            Text(
                text = iniciais,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

/** "James Willian" → "JW"; um nome só → a primeira letra dele. */
internal fun iniciais(nome: String): String {
    val partes = nome.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when (partes.size) {
        0 -> ""
        1 -> partes[0].take(1)
        else -> partes.first().take(1) + partes.last().take(1)
    }.uppercase()
}

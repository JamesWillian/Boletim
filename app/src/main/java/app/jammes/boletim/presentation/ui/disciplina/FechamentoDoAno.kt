package app.jammes.boletim.presentation.ui.disciplina

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.jammes.boletim.domain.model.Fechamento
import app.jammes.boletim.domain.model.PeriodoDomain
import app.jammes.boletim.domain.model.StatusDisciplina
import app.jammes.boletim.domain.model.TipoPeriodo
import app.jammes.boletim.presentation.ui.anoletivo.feminino
import app.jammes.boletim.presentation.ui.anoletivo.ordinalDoPeriodo
import app.jammes.boletim.presentation.ui.boletim.corDoStatus
import app.jammes.boletim.presentation.ui.boletim.formatarMedia
import app.jammes.boletim.presentation.ui.boletim.nomeDaSituacao
import app.jammes.boletim.presentation.ui.boletim.textoFaltas
import app.jammes.boletim.presentation.ui.components.Pilula
import app.jammes.boletim.presentation.ui.theme.BoletimTheme
import app.jammes.boletim.presentation.ui.theme.Espacos

/**
 * As confirmações do fechamento do ano. A tela guarda só qual está aberta; o conteúdo vem do detalhe,
 * então acompanha o que acabou de ser gravado (a nota da final, por exemplo).
 */
internal enum class DialogoDoFechamento {
    FINALIZAR,
    /** A mesma confirmação, oferecida logo depois de a final ganhar nota. */
    FINALIZAR_DEPOIS_DA_FINAL,
    /** O Finalizar sem nenhum período com nota: ainda não há média do ano para fechar. */
    SEM_NOTA,
    REABRIR,
}

/**
 * Os botões embaixo do resumo: fechar o ano da disciplina (ou reabrir, depois de finalizada) e editar
 * a disciplina. O primeiro troca de texto num fade, no lugar, ao finalizar ou reabrir.
 */
@Composable
internal fun AcoesDaDisciplina(
    finalizada: Boolean,
    onFinalizar: () -> Unit,
    onReabrir: () -> Unit,
    onEditar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Espacos.m),
    ) {
        OutlinedButton(
            onClick = if (finalizada) onReabrir else onFinalizar,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
        ) {
            AnimatedContent(
                targetState = finalizada,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "finalizarOuReabrir",
            ) { jaFinalizada ->
                if (jaFinalizada) ConteudoDoBotao("Reabrir", Icons.Outlined.LockOpen)
                else ConteudoDoBotao("Finalizar", Icons.Outlined.TaskAlt)
            }
        }
        OutlinedButton(
            onClick = onEditar,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
        ) {
            ConteudoDoBotao("Editar", Icons.Outlined.Edit)
        }
    }
}

@Composable
private fun ConteudoDoBotao(texto: String, icone: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icone, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text(texto)
    }
}

/** "Aprovado" ou "Reprovado", na cor do status e com o ícone dele: o carimbo da disciplina finalizada. */
@Composable
internal fun SeloDaSituacao(situacao: StatusDisciplina, modifier: Modifier = Modifier) {
    Pilula(
        texto = nomeDaSituacao(situacao),
        cor = corDoStatus(situacao),
        icone = if (situacao == StatusDisciplina.REPROVADO) Icons.Rounded.Close else Icons.Rounded.Check,
        modifier = modifier,
    )
}

/**
 * Confirma o fechamento do ano: a situação em que a disciplina fica e o porquê, a média do ano contra
 * a mínima e as faltas contra o limite (quando ele existe). O que reprova aparece em vermelho. Os
 * períodos ainda sem nota ficam fora da média, então vêm num alerta.
 *
 * [depoisDaFinal]: oferecida logo depois de a final ganhar nota, e não pelo botão.
 */
@Composable
internal fun ConfirmarFinalizacao(
    disciplina: String,
    fechamento: Fechamento,
    tipoPeriodo: TipoPeriodo,
    depoisDaFinal: Boolean,
    onFinalizar: () -> Unit,
    onCancelar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Finalizar $disciplina?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Espacos.l)) {
                if (depoisDaFinal) {
                    Text("A nota da final foi lançada. Se o ano da disciplina acabou, ela já pode ser fechada.")
                }
                Column(verticalArrangement = Arrangement.spacedBy(Espacos.xs)) {
                    SeloDaSituacao(fechamento.situacao, Modifier.padding(bottom = Espacos.xs))
                    LinhaDoMotivo(
                        texto = "Média do ano ${formatarMedia(fechamento.mediaDoAno)} · mínima ${formatarMedia(fechamento.mediaMinima)}",
                        reprova = fechamento.abaixoDaMinima,
                    )
                    fechamento.limiteFaltas?.let { limite ->
                        LinhaDoMotivo(
                            texto = "${textoFaltas(fechamento.faltas)} · $limite permitidas",
                            reprova = fechamento.passouDasFaltas,
                        )
                    }
                }
                avisoDosPeriodosSemNota(fechamento.periodosSemNota, tipoPeriodo)?.let { aviso ->
                    Row {
                        Icon(
                            Icons.Outlined.WarningAmber,
                            contentDescription = null, // o texto ao lado já é o alerta
                            tint = BoletimTheme.coresExtras.atencao,
                            modifier = Modifier.padding(end = Espacos.s).size(20.dp),
                        )
                        Text(aviso, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                Text("Finalizada, ela não aceita mais notas nem faltas. Dá para reabrir depois.")
            }
        },
        confirmButton = { TextButton(onClick = onFinalizar) { Text("Finalizar") } },
        dismissButton = { TextButton(onClick = onCancelar) { Text(if (depoisDaFinal) "Agora não" else "Cancelar") } },
    )
}

@Composable
private fun LinhaDoMotivo(texto: String, reprova: Boolean) {
    Text(
        text = texto,
        color = if (reprova) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** O Finalizar sem nenhuma nota: a situação sai da média do ano, e ainda não há uma. */
@Composable
internal fun AvisoSemNota(tipoPeriodo: TipoPeriodo, onFechar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text("Ainda não dá para finalizar") },
        text = { Text(textoSemNota(tipoPeriodo)) },
        confirmButton = { TextButton(onClick = onFechar) { Text("Entendi") } },
    )
}

@Composable
internal fun ConfirmarReabertura(
    disciplina: String,
    situacao: StatusDisciplina?,
    onReabrir: () -> Unit,
    onCancelar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Reabrir $disciplina?") },
        text = { Text(textoDaReabertura(situacao)) },
        confirmButton = { TextButton(onClick = onReabrir) { Text("Reabrir") } },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    )
}

// Textos ------------------------------------------------------------------------------------

/**
 * "O 4º Bimestre está sem nota e fica fora da média do ano.", "A 3ª e a 4ª Unidade estão sem nota e
 * ficam fora da média do ano." null quando todos têm nota.
 */
internal fun avisoDosPeriodosSemNota(periodos: List<PeriodoDomain>, tipo: TipoPeriodo): String? {
    if (periodos.isEmpty()) return null
    val artigo = if (tipo.feminino) "a" else "o"
    val ordinais = periodos.map { "$artigo ${ordinalDoPeriodo(it.periodo, tipo)}" }
    val lista = if (ordinais.size == 1) ordinais.single()
    else ordinais.dropLast(1).joinToString(", ") + " e " + ordinais.last()
    val verbo = if (ordinais.size == 1) "está sem nota e fica" else "estão sem nota e ficam"
    return "${lista.replaceFirstChar { it.uppercase() }} ${tipo.displayName} $verbo fora da média do ano."
}

/** "Nenhum bimestre tem nota ainda. …", "Nenhuma unidade tem nota ainda. …" */
internal fun textoSemNota(tipo: TipoPeriodo): String =
    "${if (tipo.feminino) "Nenhuma" else "Nenhum"} ${tipo.displayName.lowercase()} tem nota ainda. " +
        "A situação final sai da média do ano: lance as notas antes de finalizar."

/** O que muda ao reabrir: os lançamentos voltam, e a situação sai até a próxima vez. */
internal fun textoDaReabertura(situacao: StatusDisciplina?): String =
    "Ela volta a aceitar notas e faltas" +
        (situacao?.let { ", e deixa de aparecer como ${nomeDaSituacao(it)} até ser finalizada de novo." } ?: ".")

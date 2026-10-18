package app.jammes.boletim.presentation.ui.notificacoes

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.jammes.boletim.avisos.notificacoesLiberadas
import app.jammes.boletim.presentation.ui.configuracoes.ConfiguracoesViewModel
import app.jammes.boletim.presentation.ui.theme.Espacos

/**
 * Se o Android deixa os avisos do Boletim Escolar chegarem no celular, e como pedir quando não deixa. A
 * resposta é conferida de novo cada vez que a tela volta para a frente: a pessoa pode ter mudado nas
 * configurações do aparelho.
 *
 * O pedido do próprio Android (do Android 13 em diante) só aparece enquanto ele pode: se o app nunca
 * pediu, ou se a pessoa recusou uma vez só. Depois disso, e nos Androids mais antigos, só as
 * configurações do aparelho liberam.
 */
@Stable
class PermissaoDeNotificacoes internal constructor(
    private val estado: MutableState<Boolean>,
    private val jaPediu: () -> Boolean?, // null até as configurações serem lidas
    private val mostrarPedido: () -> Boolean, // false quando o pedido do Android não pode mais aparecer
    private val abrirConfiguracoes: () -> Unit,
) {
    val liberada: Boolean
        get() = estado.value

    /** O Permitir da faixa: o pedido do Android, ou a página do app nas configurações do aparelho. */
    fun pedir() {
        if (!mostrarPedido()) abrirConfiguracoes()
    }

    /**
     * A chave das notificações ligada: o pedido do Android, se ainda faltar a permissão. Quando ele não
     * pode mais aparecer, a pessoa não é levada para fora do app: a faixa de bloqueio diz o que fazer.
     */
    fun pedirAoLigar() {
        if (!liberada) mostrarPedido()
    }

    /**
     * O pedido do Android sem ninguém tocar em nada, por isso só se o app nunca pediu: quem já
     * respondeu uma vez não é perguntado de novo, e a faixa de bloqueio fica para quando mudar de ideia.
     */
    fun pedirPelaPrimeiraVez() {
        if (jaPediu() == false && !liberada) mostrarPedido()
    }
}

/**
 * [jaPediu] e [onPediu] guardam se o pedido do Android já apareceu, o que o próprio Android não conta.
 * [jaPediu] é null enquanto as configurações não foram lidas.
 */
@Composable
internal fun rememberPermissaoDeNotificacoes(
    jaPediu: Boolean?,
    onPediu: () -> Unit,
): PermissaoDeNotificacoes {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val estado = remember { mutableStateOf(notificacoesLiberadas(context)) }
    LifecycleResumeEffect(Unit) {
        estado.value = notificacoesLiberadas(context)
        onPauseOrDispose { }
    }
    val pedido = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        estado.value = notificacoesLiberadas(context)
    }
    val jaPediuAgora by rememberUpdatedState(jaPediu)
    val marcarPedido by rememberUpdatedState(onPediu)

    return remember {
        PermissaoDeNotificacoes(
            estado = estado,
            jaPediu = { jaPediuAgora },
            mostrarPedido = {
                // Antes de ler as configurações, como se nunca tivesse pedido: o pior é o Android não
                // mostrar o pedido e o toque no Permitir não fazer nada
                podePedirNoAndroid(context, activity, jaPediu = jaPediuAgora == true).also { pode ->
                    if (pode) {
                        marcarPedido()
                        pedido.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            },
            abrirConfiguracoes = { abrirConfiguracoesDoAparelho(context) },
        )
    }
}

private fun podePedirNoAndroid(context: Context, activity: Activity?, jaPediu: Boolean): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
    val concedida = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    // Com a permissão dada, o que está desligado fica nas configurações: as do app, ou o canal dos avisos
    if (concedida) return false
    return !jaPediu ||
        activity?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.POST_NOTIFICATIONS) } == true
}

private fun abrirConfiguracoesDoAparelho(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

/**
 * O pedido do Android que aparece sozinho, uma vez, quando a tela abre: se o app nunca pediu, as
 * notificações estão ligadas e o Android ainda bloqueia os avisos. Fica no boletim, a primeira tela
 * de quem acabou de se cadastrar, de quem restaurou um backup e de quem só atualizou o app.
 *
 * Espera as configurações serem lidas: antes disso, não dá para saber se o app já pediu, e quem
 * recusou seria perguntado de novo a cada vez que o app abre.
 */
@Composable
internal fun PedirPermissaoAoAbrir(
    viewModel: ConfiguracoesViewModel = hiltViewModel(LocalActivity.current as ViewModelStoreOwner),
) {
    val configuracoes by viewModel.configuracoes.collectAsStateWithLifecycle()
    val jaPediu by viewModel.permissaoDeNotificacoesPedida.collectAsStateWithLifecycle()
    val permissao = rememberPermissaoDeNotificacoes(jaPediu, viewModel::marcarPermissaoDeNotificacoesPedida)
    val ativas = configuracoes?.notificacoesAtivas
    LaunchedEffect(jaPediu, ativas) {
        if (jaPediu == false && ativas == true) permissao.pedirPelaPrimeiraVez()
    }
}

/** O Android não deixa os avisos chegarem: a faixa que diz isso e libera. */
@Composable
internal fun AvisoDeBloqueio(
    onPermitir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(start = Espacos.m, top = Espacos.xs, bottom = Espacos.xs, end = Espacos.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.NotificationsOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
        )
        Spacer(Modifier.width(Espacos.m))
        Text(
            text = TEXTO_DO_BLOQUEIO,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = Espacos.s),
        )
        TextButton(
            onClick = onPermitir,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onErrorContainer),
        ) { Text("Ativar") }
    }
}

internal const val TEXTO_DO_BLOQUEIO = "As notificações estão desativadas no aparelho. Ative para receber os avisos."

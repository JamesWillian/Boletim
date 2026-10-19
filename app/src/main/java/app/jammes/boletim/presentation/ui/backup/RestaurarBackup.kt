package app.jammes.boletim.presentation.ui.backup

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.jammes.boletim.presentation.ui.theme.Espacos

/**
 * O "Restaurar backup" das boas-vindas, para quem já usava o app: escolher o arquivo, digitar a
 * senha, se ele tiver uma, conferir o que ele traz e gravar. Depois de gravar não há o que fazer
 * aqui: o contexto do backup fica salvo, e o AppRoot troca as boas-vindas pelo boletim. Os avisos
 * (um arquivo que não é backup, por exemplo) aparecem no [snackbar] da tela de quem chama.
 */
@Composable
internal fun RestaurarBackup(
    snackbar: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: BackupViewModel = hiltViewModel(),
) {
    val andamento by viewModel.andamento.collectAsStateWithLifecycle()
    val aviso by viewModel.aviso.collectAsStateWithLifecycle()
    val escolherBackup = rememberEscolherBackup(viewModel)

    LaunchedEffect(aviso) {
        val mensagem = aviso ?: return@LaunchedEffect
        snackbar.showSnackbar(mensagem)
        viewModel.avisoVisto()
    }

    // Lendo ou gravando, a roda gira no lugar do ícone
    val trabalhando = andamento == Andamento.Lendo || andamento == Andamento.Importando
    TextButton(onClick = escolherBackup, enabled = andamento == Andamento.Parado, modifier = modifier) {
        if (trabalhando) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
        } else {
            Icon(Icons.Outlined.SettingsBackupRestore, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(Espacos.s))
        Text("Restaurar backup")
    }

    EtapasDaImportacao(andamento, substituindo = false, viewModel = viewModel)
}

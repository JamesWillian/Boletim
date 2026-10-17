package app.jammes.boletim.presentation.ui.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.jammes.boletim.domain.model.BackupInvalidoException
import app.jammes.boletim.domain.model.BackupLido
import app.jammes.boletim.domain.repository.BackupRepository
import app.jammes.boletim.domain.usecase.ImportarBackup
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.IOException

/** O que a tela está fazendo com os arquivos. Uma coisa de cada vez: um toque no meio não começa outra. */
sealed interface Andamento {
    data object Parado : Andamento
    data object Exportando : Andamento
    data object Lendo : Andamento // abrindo o arquivo escolhido para importar
    data class Confirmando(val backup: BackupLido) : Andamento // o arquivo serve: falta a pessoa confirmar
    data object Importando : Andamento
}

/**
 * O backup em arquivo, na tela de backup e no "Restaurar backup" das boas-vindas. Os arquivos chegam
 * do seletor do Android pela tela, que passa o endereço deles. A chave do backup automático fica no
 * ConfiguracoesViewModel, com as outras configurações.
 */
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupRepo: BackupRepository,
    private val importarBackup: ImportarBackup,
) : ViewModel() {

    private val _andamento = MutableStateFlow<Andamento>(Andamento.Parado)
    val andamento: StateFlow<Andamento> = _andamento.asStateFlow()

    /** Um aviso para mostrar uma vez embaixo da tela; volta a null depois de visto. */
    private val _aviso = MutableStateFlow<String?>(null)
    val aviso: StateFlow<String?> = _aviso.asStateFlow()

    private val _importou = Channel<Unit>(Channel.BUFFERED)

    /**
     * Os dados foram trocados e o contexto do backup já está salvo. Um evento, e não um estado: nas
     * boas-vindas ninguém chega a ouvir (o AppRoot troca a tela antes), e o ViewModel volta a Parado.
     */
    val importou: Flow<Unit> = _importou.receiveAsFlow()

    fun exportar(destino: String) {
        if (_andamento.value != Andamento.Parado) return
        _andamento.value = Andamento.Exportando
        viewModelScope.launch {
            _aviso.value = try {
                backupRepo.exportar(destino)
                "Dados exportados"
            } catch (e: IOException) {
                NAO_SALVOU
            } catch (e: SecurityException) {
                NAO_SALVOU
            }
            _andamento.value = Andamento.Parado
        }
    }

    /** Lê o arquivo escolhido e, se ele servir, pede a confirmação; se não, diz por quê. */
    fun lerArquivo(origem: String) {
        if (_andamento.value != Andamento.Parado) return
        _andamento.value = Andamento.Lendo
        viewModelScope.launch {
            _andamento.value = try {
                Andamento.Confirmando(backupRepo.ler(origem))
            } catch (e: BackupInvalidoException) {
                avisar(e.message ?: NAO_ABRIU)
            } catch (e: IOException) {
                avisar(NAO_ABRIU)
            } catch (e: SecurityException) {
                avisar(NAO_ABRIU)
            }
        }
    }

    fun cancelarImportacao() {
        if (_andamento.value is Andamento.Confirmando) _andamento.value = Andamento.Parado
    }

    /** Troca os dados do aparelho pelos do arquivo confirmado. */
    fun importar() {
        val backup = (_andamento.value as? Andamento.Confirmando)?.backup ?: return
        _andamento.value = Andamento.Importando
        viewModelScope.launch {
            _andamento.value = try {
                importarBackup(backup)
                _importou.send(Unit)
                Andamento.Parado
            } catch (e: BackupInvalidoException) {
                avisar(e.message ?: NAO_IMPORTOU)
            } catch (e: IOException) {
                avisar(NAO_IMPORTOU)
            }
        }
    }

    /** Nenhum app do aparelho abre o seletor de arquivos. */
    fun semSeletorDeArquivos() {
        _aviso.value = "Este aparelho não tem um app de arquivos para escolher onde fica o backup"
    }

    fun avisoVisto() {
        _aviso.value = null
    }

    private fun avisar(mensagem: String): Andamento {
        _aviso.value = mensagem
        return Andamento.Parado
    }
}

private const val NAO_SALVOU = "Não deu para salvar o arquivo"
private const val NAO_ABRIU = "Não deu para abrir o arquivo"
private const val NAO_IMPORTOU = "Não deu para importar os dados"

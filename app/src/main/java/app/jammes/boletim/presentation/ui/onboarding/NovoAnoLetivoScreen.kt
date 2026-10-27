package app.jammes.boletim.presentation.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.jammes.boletim.domain.model.PerfilAluno
import app.jammes.boletim.presentation.ui.components.descricaoDoAno
import java.time.LocalDate

// Os passos do onboarding que fazem um ano letivo: a série e o ano, e depois a divisão
private enum class PassoDoAnoNovo { ANO, DIVISAO }

/**
 * Um ano letivo novo para o aluno aberto, pelo perfil. São os passos do onboarding que fazem o ano,
 * com as mesmas peças: a série e o ano, já no [anoSugerido], e depois a divisão. As regras e as
 * matérias começam no padrão.
 *
 * Criar grava o ano e abre ele no app; [onCriado] leva então aos ajustes dele, para conferir. Voltar
 * do primeiro passo desiste ([onFechar]).
 */
@Composable
fun NovoAnoLetivoScreen(
    onFechar: () -> Unit,
    onCriado: () -> Unit,
    vm: NovoAnoLetivoViewModel = hiltViewModel(),
) {
    val perfil by vm.perfil.collectAsStateWithLifecycle()
    val salvando by vm.salvando.collectAsStateWithLifecycle()
    val avisos = remember { SnackbarHostState() }
    val haptico = LocalHapticFeedback.current
    val criado by rememberUpdatedState(onCriado)

    LaunchedEffect(vm) { vm.erros.collect { avisos.showSnackbar(it) } }
    LaunchedEffect(vm) {
        vm.criado.collect {
            haptico.performHapticFeedback(HapticFeedbackType.Confirm)
            criado()
        }
    }

    TelaDePassos(avisos) {
        // O Room responde em milissegundos: os passos já aparecem prontos, sem um carregando no meio
        perfil?.let { PassosDoAnoNovo(it, salvando, onFechar = onFechar, onCriar = vm::criar) }
    }
}

@Composable
private fun PassosDoAnoNovo(
    perfil: PerfilAluno,
    salvando: Boolean,
    onFechar: () -> Unit,
    onCriar: (FormularioNovoAnoLetivo) -> Unit,
) {
    // Os anos que o aluno já tem: o mesmo não pode repetir
    val existentes = remember(perfil.anosLetivos) { perfil.anosLetivos.map { it.ano }.toSet() }
    var passo by rememberSaveable { mutableStateOf(PassoDoAnoNovo.ANO) }
    var formulario by rememberSaveable {
        mutableStateOf(FormularioNovoAnoLetivo(ano = anoSugerido(existentes, LocalDate.now().year)))
    }
    // No primeiro passo, o voltar do sistema fica com o NavHost: desiste do ano novo
    BackHandler(enabled = passo != PassoDoAnoNovo.ANO) { passo = PassoDoAnoNovo.ANO }

    AnimatedContent(targetState = passo, transitionSpec = { trocaDePasso() }, label = "passoDoAnoNovo") { atual ->
        when (atual) {
            PassoDoAnoNovo.ANO -> PassoAno(
                nome = perfil.aluno.nome,
                formulario = formulario,
                existentes = existentes,
                onMudar = { formulario = it },
                onVoltar = onFechar,
                onContinuar = { passo = PassoDoAnoNovo.DIVISAO },
            )
            PassoDoAnoNovo.DIVISAO -> PassoDivisao(
                formulario = formulario,
                onMudar = { formulario = it },
                onVoltar = { passo = PassoDoAnoNovo.ANO },
                onContinuar = { onCriar(formulario) },
                total = PassoDoAnoNovo.entries.size,
                botao = "Criar ano letivo",
                carregando = salvando,
                aviso = "As regras de média e as matérias começam no padrão. Em seguida, os ajustes do ano abrem para você conferir.",
            )
        }
    }
}

/** O primeiro passo do onboarding sem o nome, que já existe: a prévia, a série e o ano. */
@Composable
private fun PassoAno(
    nome: String,
    formulario: FormularioNovoAnoLetivo,
    existentes: Set<Int>,
    onMudar: (FormularioNovoAnoLetivo) -> Unit,
    onVoltar: () -> Unit,
    onContinuar: () -> Unit,
) {
    EstruturaDoPasso(
        numero = 1,
        total = PassoDoAnoNovo.entries.size,
        titulo = "Para qual ano é o boletim?",
        subtitulo = "Escolha a série e o ano letivo. No topo do app, $nome vai aparecer assim:",
        onVoltar = onVoltar,
        botaoHabilitado = formulario.ano !in existentes,
        onBotao = onContinuar,
    ) {
        PreviaDaIdentificacao(nome = nome, descricao = descricaoDoAno(formulario.serie.texto(), formulario.ano))
        EscolhaDaSerie(serie = formulario.serie, onEscolher = { onMudar(formulario.copy(serie = it)) })
        EscolhaDoAno(ano = formulario.ano, onEscolher = { onMudar(formulario.comAno(it)) }, existentes = existentes)
    }
}

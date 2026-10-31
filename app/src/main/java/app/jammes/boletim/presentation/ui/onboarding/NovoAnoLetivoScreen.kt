package app.jammes.boletim.presentation.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.jammes.boletim.domain.model.CriacaoDoAno
import app.jammes.boletim.domain.model.PerfilAluno
import app.jammes.boletim.domain.model.Plano
import app.jammes.boletim.domain.model.criacaoDoAno
import app.jammes.boletim.presentation.ui.components.descricaoDoAno
import app.jammes.boletim.presentation.ui.plano.PlanoViewModel
import app.jammes.boletim.presentation.ui.theme.BoletimTheme
import app.jammes.boletim.presentation.ui.theme.Espacos
import java.time.LocalDate

// Os passos do onboarding que fazem um ano letivo: a série e o ano, e depois a divisão
private enum class PassoDoAnoNovo { ANO, DIVISAO }

/**
 * Um ano letivo novo para o aluno aberto, pelo perfil. São os passos do onboarding que fazem o ano,
 * com as mesmas peças: a série e o ano, já no [anoSugerido], e depois a divisão. As regras e as
 * matérias começam no padrão. Os anos que o plano não libera levam a etiqueta do Pro, e um ano que o
 * aluno já tem, que só o Pro repete, ganha um aviso.
 *
 * Criar grava o ano e abre ele no app; [onCriado] leva então aos ajustes dele, para conferir. Voltar
 * do primeiro passo desiste ([onFechar]).
 */
@Composable
fun NovoAnoLetivoScreen(
    onFechar: () -> Unit,
    onCriado: () -> Unit,
    vm: NovoAnoLetivoViewModel = hiltViewModel(),
    planoViewModel: PlanoViewModel = hiltViewModel(),
) {
    val perfil by vm.perfil.collectAsStateWithLifecycle()
    val plano by planoViewModel.plano.collectAsStateWithLifecycle()
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
        // O Room e o DataStore respondem em milissegundos: os passos já aparecem prontos, sem um
        // carregando no meio. O plano vem antes, porque o ano sugerido depende dele
        val lido = perfil
        val planoLido = plano
        if (lido != null && planoLido != null) {
            PassosDoAnoNovo(lido, planoLido, salvando, onFechar = onFechar, onCriar = vm::criar)
        }
    }
}

@Composable
private fun PassosDoAnoNovo(
    perfil: PerfilAluno,
    plano: Plano,
    salvando: Boolean,
    onFechar: () -> Unit,
    onCriar: (FormularioNovoAnoLetivo) -> Unit,
) {
    // Os anos que o aluno já tem: o mesmo não pode repetir
    val existentes = remember(perfil.anosLetivos) { perfil.anosLetivos.map { it.ano }.toSet() }
    val hoje = remember { LocalDate.now() }
    var passo by rememberSaveable { mutableStateOf(PassoDoAnoNovo.ANO) }
    var formulario by rememberSaveable {
        mutableStateOf(FormularioNovoAnoLetivo(ano = anoSugerido(existentes, hoje, plano)))
    }
    // No primeiro passo, o voltar do sistema fica com o NavHost: desiste do ano novo
    BackHandler(enabled = passo != PassoDoAnoNovo.ANO) { passo = PassoDoAnoNovo.ANO }

    AnimatedContent(targetState = passo, transitionSpec = { trocaDePasso() }, label = "passoDoAnoNovo") { atual ->
        when (atual) {
            PassoDoAnoNovo.ANO -> PassoAno(
                nome = perfil.aluno.nome,
                formulario = formulario,
                existentes = existentes,
                plano = plano,
                hoje = hoje,
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
    plano: Plano,
    hoje: LocalDate,
    onMudar: (FormularioNovoAnoLetivo) -> Unit,
    onVoltar: () -> Unit,
    onContinuar: () -> Unit,
) {
    val criacao = plano.criacaoDoAno(formulario.ano, existentes, hoje)
    EstruturaDoPasso(
        numero = 1,
        total = PassoDoAnoNovo.entries.size,
        titulo = "Para qual ano é o boletim?",
        subtitulo = "Escolha a série e o ano letivo. No topo do app, $nome vai aparecer assim:",
        onVoltar = onVoltar,
        botaoHabilitado = criacao.permitida,
        onBotao = onContinuar,
    ) {
        PreviaDaIdentificacao(nome = nome, descricao = descricaoDoAno(formulario.serie.texto(), formulario.ano))
        EscolhaDaSerie(serie = formulario.serie, onEscolher = { onMudar(formulario.copy(serie = it)) })
        EscolhaDoAno(ano = formulario.ano, plano = plano, onEscolher = { onMudar(formulario.comAno(it)) }, existentes = existentes)
        AnimatedVisibility(visible = criacao == CriacaoDoAno.REPETIDA) {
            AvisoDeAnoRepetido(avisoDeAnoRepetido(nome, formulario.ano))
        }
    }
}

/** O alerta embaixo do ano, como o do Finalizar: o ícone na cor de atenção e o texto ao lado. */
@Composable
private fun AvisoDeAnoRepetido(texto: String) {
    Row(Modifier.padding(top = Espacos.m)) {
        Icon(
            Icons.Outlined.WarningAmber,
            contentDescription = null, // o texto ao lado já é o alerta
            tint = BoletimTheme.coresExtras.atencao,
            modifier = Modifier.padding(end = Espacos.s).size(20.dp),
        )
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

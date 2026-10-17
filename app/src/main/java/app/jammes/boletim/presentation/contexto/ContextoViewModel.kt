package app.jammes.boletim.presentation.contexto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.jammes.boletim.domain.repository.AlunoRepository
import app.jammes.boletim.domain.repository.AnoLetivoRepository
import app.jammes.boletim.domain.repository.ContextoRepository
import app.jammes.boletim.domain.repository.DisciplinaRepository
import app.jammes.boletim.domain.repository.NotificacaoRepository
import app.jammes.boletim.presentation.ui.components.siglasDasDisciplinas
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import jakarta.inject.Inject
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlin.collections.emptyList

@HiltViewModel
class ContextoViewModel @Inject constructor(
    private val contextRepo: ContextoRepository,
    anoLetivoRepo: AnoLetivoRepository,
    alunoRepo: AlunoRepository,
    disciplinaRepo: DisciplinaRepository,
    notificacaoRepo: NotificacaoRepository,
): ViewModel() {

    val state: StateFlow<ContextoUiState> = contextRepo.contexto
        .map { contexto ->
            if (contexto == null) ContextoUiState.Vazio else ContextoUiState.Definido(contexto)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ContextoUiState.Carregando)

    @OptIn(ExperimentalCoroutinesApi::class)
    val anosLetivos = contextRepo.contexto
        .filterNotNull()
        .map { it.alunoId }
        .distinctUntilChanged()
        .flatMapLatest { anoLetivoRepo.observeByAluno(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val alunos = alunoRepo.observeAluno()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val disciplinas = contextRepo.contexto
        .filterNotNull()
        .map { it.anoLetivoId }
        .distinctUntilChanged()
        .flatMapLatest { ano -> disciplinaRepo.observeByAnoLetivo(ano) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    /**
     * A sigla de cada disciplina do ano aberto, pelo id: o que as abas mostram no estilo "Ícones +
     * sigla". Vem da abreviação da própria disciplina, e não da matéria.
     */
    val siglas = disciplinas.map(::siglasDasDisciplinas)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyMap()
        )

    /**
     * Se o aluno aberto tem notificação que ainda não apareceu na tela delas: acende o ponto azul do
     * sino, no topo. As dos outros alunos não contam; trocar de aluno troca o ponto junto.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val temNotificacoesNovas = contextRepo.contexto
        .filterNotNull()
        .map { it.alunoId }
        .distinctUntilChanged()
        .flatMapLatest { aluno -> notificacaoRepo.naoLidas(aluno) }
        .map { it > 0 }
        .distinctUntilChanged()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            false
        )

    fun trocarAluno(alunoId: Long, anoLetivoId: Long, periodoId: Long) {
        viewModelScope.launch { contextRepo.selecionarAluno(alunoId, anoLetivoId, periodoId) }
    }
    fun trocarAno(anoLetivoId: Long, periodoId: Long) {
        viewModelScope.launch { contextRepo.selecionarAnoLetivo(anoLetivoId, periodoId) }
    }
    /** null: o ano letivo inteiro. */
    fun trocarPeriodo(periodoId: Long?) {
        viewModelScope.launch { contextRepo.selecionarPeriodo(periodoId) }
    }
}
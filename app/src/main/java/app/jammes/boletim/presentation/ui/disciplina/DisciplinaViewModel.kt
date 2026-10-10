package app.jammes.boletim.presentation.ui.disciplina

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.jammes.boletim.domain.model.AvaliacaoDomain
import app.jammes.boletim.domain.model.DisciplinaDetalhe
import app.jammes.boletim.domain.model.FaltaDomain
import app.jammes.boletim.domain.model.intervalo
import app.jammes.boletim.domain.model.periodoDe
import app.jammes.boletim.domain.repository.AvaliacaoRepository
import app.jammes.boletim.domain.repository.ContextoRepository
import app.jammes.boletim.domain.repository.FaltaRepository
import app.jammes.boletim.domain.usecase.ObterDisciplinaDetalhe
import app.jammes.boletim.presentation.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.time.Clock

sealed interface DisciplinaUiState {
    data object Carregando : DisciplinaUiState
    data object NaoEncontrada : DisciplinaUiState
    data class Sucesso(val detalhe: DisciplinaDetalhe) : DisciplinaUiState
}

@HiltViewModel
class DisciplinaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    contextoRepo: ContextoRepository,
    obterDisciplinaDetalhe: ObterDisciplinaDetalhe,
    private val avaliacaoRepo: AvaliacaoRepository,
    private val faltaRepo: FaltaRepository,
): ViewModel() {

    // Argumento da rota que abriu a tela. Cada disciplina aberta ganha seu próprio ViewModel,
    // então o id não muda durante a vida dele.
    private val disciplinaId = savedStateHandle.toRoute<Routes.Boletim.Disciplina>().disciplinaId

    val state: StateFlow<DisciplinaUiState> =
        obterDisciplinaDetalhe(disciplinaId, contextoRepo.contexto.filterNotNull())
            .map { detalhe ->
                if (detalhe == null)
                    DisciplinaUiState.NaoEncontrada
                else
                    DisciplinaUiState.Sucesso(detalhe)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = DisciplinaUiState.Carregando
            )

    /**
     * Ponto de partida do formulário: valendo 10, com peso 1 e a data de hoje. Fica no período
     * que está na tela; no ano letivo inteiro, no período de hoje (o formulário deixa trocar).
     */
    fun novaAvaliacao(detalhe: DisciplinaDetalhe): AvaliacaoDomain {
        val hoje = LocalDate.now()
        return AvaliacaoDomain(
            disciplinaId = disciplinaId,
            periodoId = detalhe.periodoId ?: detalhe.periodos.periodoDe(hoje)?.id,
            nome = "",
            nota = null,
            notaMaxima = 10.0,
            peso = 1.0,
            data = hoje,
            criadoEm = Clock.System.now(),
        )
    }

    fun salvar(avaliacao: AvaliacaoDomain) {
        viewModelScope.launch {
            avaliacaoRepo.upsert(avaliacao)
        }
    }

    fun excluir(avaliacao: AvaliacaoDomain) {
        viewModelScope.launch {
            avaliacaoRepo.delete(avaliacao)
        }
    }

    /**
     * Ponto de partida do formulário de falta: uma falta, hoje. Se hoje cai fora dos períodos da
     * tela (outro período, ou o ano já acabou), começa no dia mais perto dentro deles. O período
     * sai da data. null quando a tela não tem período onde a falta possa contar.
     */
    fun novaFalta(detalhe: DisciplinaDetalhe): FaltaDomain? {
        val periodos = detalhe.periodosNoFiltro
        val dia = LocalDate.now().coerceIn(periodos.intervalo() ?: return null)
        val periodo = periodos.periodoDe(dia) ?: return null
        return FaltaDomain(
            disciplinaId = disciplinaId,
            periodoId = periodo.id,
            data = dia,
            qtdAulas = 1,
        )
    }

    fun salvar(falta: FaltaDomain) {
        viewModelScope.launch {
            faltaRepo.upsert(falta)
        }
    }

    fun excluir(falta: FaltaDomain) {
        viewModelScope.launch {
            faltaRepo.delete(falta)
        }
    }
}

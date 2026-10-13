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
import app.jammes.boletim.domain.repository.DisciplinaRepository
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
    private val disciplinaRepo: DisciplinaRepository,
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
     * Ponto de partida do formulário: valendo 10, com peso 1 e a data de hoje. Se hoje cai fora
     * dos períodos da tela (outro período, ou o ano já acabou), começa no dia mais perto dentro
     * deles, como a falta. Fica no período que está na tela; no ano letivo inteiro, no da data.
     */
    fun novaAvaliacao(detalhe: DisciplinaDetalhe): AvaliacaoDomain {
        val hoje = LocalDate.now()
        val dia = detalhe.periodosNoFiltro.intervalo()?.let { hoje.coerceIn(it) } ?: hoje
        return AvaliacaoDomain(
            disciplinaId = disciplinaId,
            periodoId = detalhe.periodoId ?: detalhe.periodos.periodoDe(dia)?.id,
            nome = "",
            nota = null,
            notaMaxima = 10.0,
            peso = 1.0,
            data = dia,
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

    /**
     * Fecha o ano da disciplina (ativa = 0): ela continua no boletim, aprovada ou reprovada, mas não
     * aceita mais notas nem faltas. Sem nota em nenhum período não há o que fechar, e nada é gravado.
     */
    fun finalizar(detalhe: DisciplinaDetalhe) {
        if (detalhe.fechamento == null) return
        viewModelScope.launch {
            disciplinaRepo.setAtiva(disciplinaId, ativa = false)
        }
    }

    /** Desfaz o [finalizar]: a disciplina volta a aceitar notas e faltas, e fica sem situação. */
    fun reabrir() {
        viewModelScope.launch {
            disciplinaRepo.setAtiva(disciplinaId, ativa = true)
        }
    }
}

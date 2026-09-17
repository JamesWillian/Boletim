package app.jammes.boletim.presentation.contexto

import app.jammes.boletim.domain.model.Contexto

sealed interface ContextoUiState {
    data object Carregando : ContextoUiState
    data object Vazio : ContextoUiState
    data class Definido(val contexto: Contexto) : ContextoUiState
}
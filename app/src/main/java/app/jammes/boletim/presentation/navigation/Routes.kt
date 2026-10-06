package app.jammes.boletim.presentation.navigation

import kotlinx.serialization.Serializable

/**
 * Uma rota por tela, aninhadas na mesma hierarquia do grafo ([AppNavHost]):
 *
 * ```
 * Routes
 * ├── Boletim               grafo: telas COM as abas de disciplinas
 * │   ├── Geral             BoletimScreen
 * │   └── Disciplina(id)    DisciplinaDetailScreen
 * ├── Materia               MateriaScreen     ┐
 * ├── AnoLetivo             AnoLetivoScreen   ├ telas SEM as abas
 * └── Aluno                 AlunoScreen       ┘
 * ```
 *
 * São @Serializable para a navegação type-safe: `navigate(Routes.Boletim.Disciplina(id))`
 * já leva o argumento, sem montar string na mão.
 */
object Routes {

    @Serializable
    data object Boletim {
        @Serializable data object Geral
        @Serializable data class Disciplina(val disciplinaId: Long)
    }

    @Serializable data object Materia
    @Serializable data object AnoLetivo
    @Serializable data object Aluno
}

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
 * ├── Materia               MateriaScreen     ┐ telas SEM as abas
 * ├── AnoLetivo             AnoLetivoScreen   ┘
 * └── Aluno                 AlunoScreen         dialog: o perfil, por cima da tela atual
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

package app.jammes.boletim.presentation.navigation

import kotlinx.serialization.Serializable

/**
 * Uma rota por tela, aninhadas na mesma hierarquia do grafo ([AppNavHost]):
 *
 * ```
 * Routes
 * ├── Boletim                  grafo: telas COM as abas de disciplinas
 * │   ├── Geral                BoletimScreen
 * │   └── Disciplina(id)       DisciplinaDetailScreen
 * ├── Materia                  MateriaScreen           ┐ telas SEM as abas
 * ├── AnoLetivo                AnoLetivoScreen         │
 * ├── EditarDisciplina(id)     EditarDisciplinaScreen  │ aberta pelo Editar do detalhe
 * ├── NovoAluno                OnboardingScreen        │ o cadastro de outro aluno, sem as boas-vindas
 * ├── Configuracoes            ConfiguracoesScreen     │ o tema e o estilo das abas, aberta pelo perfil
 * ├── Backup                   BackupScreen            │ backup e restauração, aberta pelo perfil
 * ├── Notificacoes             NotificacoesScreen      │ a lista, aberta pelo sino do topo
 * ├── ConfiguracoesDasNotificacoes  ConfiguracoesDasNotificacoesScreen
 * │                                                    ┘ a chave e os horários, aberta pela lista
 * └── Aluno                    AlunoScreen               dialog: o perfil, por cima da tela atual
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
    @Serializable data class EditarDisciplina(val disciplinaId: Long)
    @Serializable data object NovoAluno
    @Serializable data object Configuracoes
    @Serializable data object Backup
    @Serializable data object Notificacoes
    @Serializable data object ConfiguracoesDasNotificacoes
    @Serializable data object Aluno
}

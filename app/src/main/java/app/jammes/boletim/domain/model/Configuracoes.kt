package app.jammes.boletim.domain.model

import java.time.LocalTime

/**
 * As preferências da pessoa que usa o app. Valem para o app todo, com qualquer aluno aberto.
 * Os padrões são o jeito do app antes de as configurações existirem.
 */
data class Configuracoes(
    val tema: Tema = Tema.SISTEMA,
    val estiloDasAbas: EstiloDasAbas = EstiloDasAbas.ICONES_E_DESCRICAO,
    val backupAutomatico: Boolean = true, // o Auto Backup do Android, na conta Google do aparelho
    val notificacoesAtivas: Boolean = true, // desligadas, nenhum aviso chega; os horários ficam guardados
    val horariosDasNotificacoes: List<LocalTime> = HORARIOS_PADRAO, // em ordem, de 1 a MAXIMO_DE_HORARIOS
)

/** Claro ou escuro sempre, ou o mesmo do aparelho. */
enum class Tema { CLARO, ESCURO, SISTEMA }

/** O que as abas da lateral mostram embaixo do ícone: nada, a sigla da disciplina ou o nome inteiro. */
enum class EstiloDasAbas { APENAS_ICONES, ICONES_E_SIGLA, ICONES_E_DESCRICAO }

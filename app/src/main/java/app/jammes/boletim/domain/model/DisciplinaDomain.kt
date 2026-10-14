package app.jammes.boletim.domain.model

import java.time.LocalDate

data class DisciplinaDomain(
    val id : Long = 0L,
    val nome : String,
    // A sigla das abas, copiada da matéria e editável na disciplina; null: sai do nome
    val abreviacao : String? = null,
    val cor : Int = 0,
    val icone : IconeMateria = IconeMateria.PADRAO,
    val totalAulas : Int? = null,
    val professor : String? = null,
    // Os dias em que a disciplina tem aula, os dois ou nenhum; null: o ano letivo inteiro
    val periodoInicio : LocalDate? = null,
    val periodoFim : LocalDate? = null,
    val ativa : Boolean = true,
    val ordem : Int = 0,
    val materiaId : Long,
    val anoLetivoId : Long
)

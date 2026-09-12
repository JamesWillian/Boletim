package app.jammes.boletim.domain.model

data class DisciplinaDomain(
    val id : Long = 0L,
    val nome : String,
    val cor : Int = 0,
    val totalAulas : Int? = null,
    val professor : String? = null,
    val periodoInicio : Long? = null,
    val periodoFim : Long? = null,
    val ativa : Boolean = true,
    val ordem : Int = 0,
    val materiaId : Long,
    val anoLetivoId : Long
)

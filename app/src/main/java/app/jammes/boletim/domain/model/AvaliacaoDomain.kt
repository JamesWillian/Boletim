package app.jammes.boletim.domain.model

import java.time.LocalDate
import kotlin.time.Instant

data class AvaliacaoDomain(
    val id: Long = 0L,
    val disciplinaId: Long,
    val periodoId: Long?,
    val nome: String,
    val nota: Double?, // null: avaliação que ainda não aconteceu
    val notaMaxima: Double,
    val peso: Double,
    val tipo: TipoAvaliacao = TipoAvaliacao.NORMAL,
    val data: LocalDate?,
    val criadoEm: Instant
)

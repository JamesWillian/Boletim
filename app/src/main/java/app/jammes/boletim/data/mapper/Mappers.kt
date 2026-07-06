package app.jammes.boletim.data.mapper

import app.jammes.boletim.data.local.entity.AlunoEntity
import app.jammes.boletim.data.local.entity.AnoLetivoEntity
import app.jammes.boletim.data.local.entity.PeriodoEntity
import app.jammes.boletim.domain.model.AlunoDomain
import app.jammes.boletim.domain.model.AnoLetivoDomain
import app.jammes.boletim.domain.model.PeriodoDomain
import java.util.UUID

fun AlunoEntity.toDomain(): AlunoDomain = AlunoDomain(
    id = id,
    nome = nome
)

fun AlunoDomain.toEntity(): AlunoEntity = AlunoEntity(
    id = id.ifEmpty { UUID.randomUUID().toString() },
    nome = nome.trim()
)

fun AnoLetivoEntity.toDomain(periodo: List<PeriodoDomain>): AnoLetivoDomain = AnoLetivoDomain(
    id = id,
    descricao = descricao,
    periodo = periodo
)

fun AnoLetivoDomain.toEntity(): AnoLetivoEntity = AnoLetivoEntity(
    id = id,
    descricao = descricao
)

fun PeriodoEntity.toDomain(): PeriodoDomain = PeriodoDomain(
    id = id,
    periodo = periodo,
    anoLetivoId = anoLetivoId
)
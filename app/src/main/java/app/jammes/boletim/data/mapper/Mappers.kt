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
    nome = nome,
    anoLetivoId = anoLetivoId,
    periodoId = periodoId
)

fun AlunoDomain.toEntity(): AlunoEntity = AlunoEntity(
    id = id.ifEmpty { UUID.randomUUID().toString() },
    nome = nome.trim(),
    anoLetivoId = anoLetivoId,
    periodoId = periodoId
)

fun AnoLetivoEntity.toDomain(periodo: List<PeriodoDomain>): AnoLetivoDomain = AnoLetivoDomain(
    id = id,
    descricao = descricao,
    periodo = periodo
)

fun AnoLetivoDomain.toEntity(): AnoLetivoEntity = AnoLetivoEntity(
    id = id.ifEmpty { UUID.randomUUID().toString() },
    descricao = descricao.trim()
)

fun PeriodoEntity.toDomain(): PeriodoDomain = PeriodoDomain(
    id = id,
    periodo = periodo,
    anoLetivoId = anoLetivoId
)
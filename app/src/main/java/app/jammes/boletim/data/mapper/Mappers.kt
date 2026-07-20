package app.jammes.boletim.data.mapper

import app.jammes.boletim.data.local.dao.BoletimPeriodo
import app.jammes.boletim.data.local.entity.AlunoEntity
import app.jammes.boletim.data.local.entity.AnoLetivoEntity
import app.jammes.boletim.data.local.entity.PeriodoEntity
import app.jammes.boletim.domain.model.AlunoDomain
import app.jammes.boletim.domain.model.AnoLetivoDomain
import app.jammes.boletim.domain.model.BoletimDomain
import app.jammes.boletim.domain.model.DisciplinaDomain
import app.jammes.boletim.domain.model.PeriodoDomain
import app.jammes.boletim.domain.model.PeriodoType
import java.util.UUID

fun AlunoEntity.toDomain(anoLetivo: String?, periodo: String?): AlunoDomain = AlunoDomain(
    id = id,
    nome = nome,
    anoLetivoId = anoLetivoId,
    anoLetivo = anoLetivo,
    periodoId = periodoId,
    periodo = periodo,
    periodoType = PeriodoType.fromString(periodoType)
)

fun AlunoDomain.toEntity(): AlunoEntity = AlunoEntity(
    id = id.ifEmpty { UUID.randomUUID().toString() },
    nome = nome.trim(),
    anoLetivoId = anoLetivoId,
    periodoId = periodoId,
    periodoType = periodoType.displayName.lowercase()
)

fun AnoLetivoEntity.toDomain(
    periodo: List<PeriodoDomain>,
    periodoType: String? = null
): AnoLetivoDomain = AnoLetivoDomain(
    id = id,
    descricao = descricao,
    periodo = periodo,
    periodoType = PeriodoType.fromString(periodoType)
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

fun PeriodoDomain.toEntity(): PeriodoEntity = PeriodoEntity(
    id = id.ifEmpty { UUID.randomUUID().toString() },
    periodo = periodo,
    anoLetivoId = anoLetivoId
)

fun BoletimPeriodo.toDomain(): BoletimDomain = BoletimDomain(
    id = id,
    disciplina = DisciplinaDomain(
        id = disciplinaId,
        nome = disciplina
    ),
    periodoId = periodoId,
    nota = nota

)
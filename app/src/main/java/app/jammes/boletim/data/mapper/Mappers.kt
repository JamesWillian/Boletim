package app.jammes.boletim.data.mapper

import app.jammes.boletim.data.local.entity.AlunoEntity
import app.jammes.boletim.domain.model.AlunoDomain
import java.util.UUID

fun AlunoEntity.toDomain(): AlunoDomain = AlunoDomain(
    id = id,
    nome = nome
)

fun AlunoDomain.toEntity(): AlunoEntity = AlunoEntity(
    id = id.ifEmpty { UUID.randomUUID().toString() },
    nome = nome.trim()
)
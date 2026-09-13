package app.jammes.boletim.data.mapper

import app.jammes.boletim.data.local.entity.AlunoEntity
import app.jammes.boletim.data.local.entity.AnoLetivoEntity
import app.jammes.boletim.data.local.entity.AvaliacaoEntity
import app.jammes.boletim.data.local.entity.DisciplinaComDados
import app.jammes.boletim.data.local.entity.DisciplinaEntity
import app.jammes.boletim.data.local.entity.FaltaEntity
import app.jammes.boletim.data.local.entity.MateriaEntity
import app.jammes.boletim.data.local.entity.PeriodoEntity
import app.jammes.boletim.data.local.entity.RegraAvaliacaoEntity
import app.jammes.boletim.domain.model.AlunoDomain
import app.jammes.boletim.domain.model.AnoLetivoDomain
import app.jammes.boletim.domain.model.AvaliacaoDomain
import app.jammes.boletim.domain.model.BoletimItem
import app.jammes.boletim.domain.model.TipoAvaliacao
import app.jammes.boletim.domain.model.DisciplinaDomain
import app.jammes.boletim.domain.model.FaltaDomain
import app.jammes.boletim.domain.model.MateriaDomain
import app.jammes.boletim.domain.model.PeriodoDomain
import app.jammes.boletim.domain.model.RegraAvaliacaoDomain
import app.jammes.boletim.domain.model.TipoArredondamento
import app.jammes.boletim.domain.model.TipoMedia
import app.jammes.boletim.domain.model.TipoPeriodo

fun AlunoEntity.toDomain(): AlunoDomain = AlunoDomain(
    id = id,
    nome = nome,
    avatar = avatar,
    ativo = ativo,
    criadoEm = criadoEm
)

fun AlunoDomain.toEntity(): AlunoEntity = AlunoEntity(
    id = id,
    nome = nome.trim(),
    avatar = avatar?.trim(),
    ativo = ativo,
    criadoEm = criadoEm
)

fun AnoLetivoEntity.toDomain(periodos: List<PeriodoDomain> = emptyList()): AnoLetivoDomain = AnoLetivoDomain(
    id = id,
    ano = ano,
    serie = serie,
    tipoPeriodo = TipoPeriodo.fromString(tipoPeriodo),
    qtdPeriodos = qtdPeriodos,
    ativo = ativo,
    alunoId = alunoId,
    periodo = periodos
)

fun AnoLetivoDomain.toEntity(): AnoLetivoEntity = AnoLetivoEntity(
    id = id,
    ano = ano,
    serie = serie,
    tipoPeriodo = TipoPeriodo.toString(tipoPeriodo),
    qtdPeriodos = qtdPeriodos,
    ativo = ativo,
    alunoId = alunoId
)

fun PeriodoEntity.toDomain(): PeriodoDomain = PeriodoDomain(
    id = id,
    periodo = periodo,
    anoLetivoId = anoLetivoId,
    dataInicio = dataInicio,
    dataFim = dataFim
)

fun PeriodoDomain.toEntity(): PeriodoEntity = PeriodoEntity(
    id = id,
    periodo = periodo,
    anoLetivoId = anoLetivoId,
    dataInicio = dataInicio,
    dataFim = dataFim
)

fun MateriaEntity.toDomain(): MateriaDomain = MateriaDomain(
    id = id,
    nome = nome,
    abreviacao = abreviacao,
    cor = cor
)

fun MateriaDomain.toEntity(): MateriaEntity = MateriaEntity(
    id = id,
    nome = nome.trim(),
    abreviacao = abreviacao?.trim(),
    cor = cor
)

fun DisciplinaEntity.toDomain(): DisciplinaDomain = DisciplinaDomain(
    id = id,
    nome = nome,
    cor = cor,
    totalAulas = totalAulas,
    professor = professor,
    periodoInicio = periodoInicio,
    periodoFim = periodoFim,
    ativa = ativa == 1,
    ordem = ordem,
    materiaId = materiaId,
    anoLetivoId = anoLetivoId
)

fun DisciplinaDomain.toEntity(): DisciplinaEntity = DisciplinaEntity(
    id = id,
    nome = nome.trim(),
    cor = cor,
    totalAulas = totalAulas,
    professor = professor?.trim(),
    periodoInicio = periodoInicio,
    periodoFim = periodoFim,
    ativa = if (ativa) 1 else 0,
    ordem = ordem,
    materiaId = materiaId,
    anoLetivoId = anoLetivoId
)

fun AvaliacaoEntity.toDomain(): AvaliacaoDomain = AvaliacaoDomain(
    id = id,
    disciplinaId = disciplinaId,
    periodoId = periodoId,
    nome = nome,
    nota = nota,
    notaMaxima = notaMaxima,
    peso = peso,
    tipo = TipoAvaliacao.fromString(tipo),
    data = data,
    criadoEm = criadoEm
)

fun AvaliacaoDomain.toEntity(): AvaliacaoEntity = AvaliacaoEntity(
    id = id,
    disciplinaId = disciplinaId,
    periodoId = periodoId,
    nome = nome.trim(),
    nota = nota,
    notaMaxima = notaMaxima,
    peso = peso,
    tipo = TipoAvaliacao.toString(tipo),
    data = data,
    criadoEm = criadoEm
)

fun FaltaEntity.toDomain(): FaltaDomain = FaltaDomain(
    id = id,
    disciplinaId = disciplinaId,
    periodoId = periodoId,
    data = data,
    qtdAulas = qtdAulas
)

fun FaltaDomain.toEntity(): FaltaEntity = FaltaEntity(
    id = id,
    disciplinaId = disciplinaId,
    periodoId = periodoId,
    data = data,
    qtdAulas = qtdAulas
)

fun RegraAvaliacaoEntity.toDomain(): RegraAvaliacaoDomain = RegraAvaliacaoDomain(
    id = id,
    anoLetivoId = anoLetivoId,
    disciplinaId = disciplinaId,
    mediaMinima = mediaMinima,
    mediaRecuperacao = mediaRecuperacao,
    frequenciaMinima = frequenciaMinima,
    tipoMedia = TipoMedia.fromString(tipoMedia),
    arredondamento = TipoArredondamento.fromString(arredondamento),
    descartarMenorNota = descartarMenorNota == 1
)

fun RegraAvaliacaoDomain.toEntity(): RegraAvaliacaoEntity = RegraAvaliacaoEntity(
    id = id,
    anoLetivoId = anoLetivoId,
    disciplinaId = disciplinaId,
    mediaMinima = mediaMinima,
    mediaRecuperacao = mediaRecuperacao,
    frequenciaMinima = frequenciaMinima,
    tipoMedia = TipoMedia.toString(tipoMedia),
    arredondamento = TipoArredondamento.toString(arredondamento),
    descartarMenorNota = if (descartarMenorNota) 1 else 0
)

fun DisciplinaComDados.toDomain(periodoId: Long): BoletimItem = BoletimItem(
    disciplinaId = disciplina.id,
    nome = disciplina.nome,
    cor = disciplina.cor,
    avaliacoes = avaliacoes.filter { it.periodoId == periodoId }.map { it.toDomain() },
    faltas = faltas.map { it.toDomain() },
    totalAulas = disciplina.totalAulas
)
package app.jammes.boletim.data.backup

import app.jammes.boletim.data.local.entity.AlunoEntity
import app.jammes.boletim.data.local.entity.AnoLetivoEntity
import app.jammes.boletim.data.local.entity.AvaliacaoEntity
import app.jammes.boletim.data.local.entity.DisciplinaEntity
import app.jammes.boletim.data.local.entity.FaltaEntity
import app.jammes.boletim.data.local.entity.MateriaEntity
import app.jammes.boletim.data.local.entity.PeriodoEntity
import app.jammes.boletim.data.local.entity.RegraAvaliacaoEntity
import app.jammes.boletim.domain.model.BackupInvalidoException
import app.jammes.boletim.domain.model.ResumoDoBackup
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import java.time.DateTimeException
import java.time.LocalDate
import kotlin.time.Instant

/*
 * O arquivo de backup é um JSON com o nome do app, a versão do formato, quando foi exportado e todas
 * as tabelas do banco, uma lista para cada, com os mesmos ids.
 *
 * Ele não depende do schema do Room: uma entidade pode mudar sem estragar os arquivos já salvos,
 * desde que o formato mude junto. Os nomes dos campos são o formato, então renomear um quebra os
 * arquivos antigos. Um campo novo entra com valor padrão (os arquivos antigos não têm ele) e sobe a
 * VERSAO_DO_ARQUIVO; assim, um app mais velho recusa o arquivo novo em vez de perder o campo.
 */

/** Todas as tabelas do banco, linha por linha. */
data class CopiaDoBanco(
    val materias: List<MateriaEntity>,
    val alunos: List<AlunoEntity>,
    val anosLetivos: List<AnoLetivoEntity>,
    val periodos: List<PeriodoEntity>,
    val disciplinas: List<DisciplinaEntity>,
    val regras: List<RegraAvaliacaoEntity>,
    val avaliacoes: List<AvaliacaoEntity>,
    val faltas: List<FaltaEntity>,
)

/** Um arquivo de backup lido e conferido: quando foi exportado e os dados dele. */
data class ArquivoDeBackup(
    val exportadoEm: Instant,
    val copia: CopiaDoBanco,
)

/** O arquivo de backup da [copia], já como texto. */
fun escreverBackup(copia: CopiaDoBanco, exportadoEm: Instant): String =
    JSON.encodeToString(BackupJson.serializer(), copia.paraJson(exportadoEm))

/**
 * Lê o [texto] de um arquivo de backup e confere se ele dá para importar: se é do app, se a versão
 * é uma que este app entende e se cada linha aponta para linhas que existem no arquivo. Se não der,
 * sai [BackupInvalidoException] com o motivo.
 */
fun lerBackup(texto: String): ArquivoDeBackup {
    val raiz = try {
        JSON.parseToJsonElement(texto) as? JsonObject
    } catch (e: SerializationException) {
        null // nem é JSON
    }
    if (raiz == null || (raiz["app"] as? JsonPrimitive)?.contentOrNull != MARCA_DO_APP) {
        throw BackupInvalidoException(NAO_E_BACKUP)
    }
    val versao = (raiz["versao"] as? JsonPrimitive)?.intOrNull ?: throw BackupInvalidoException(BACKUP_ALTERADO)
    if (versao > VERSAO_DO_ARQUIVO) throw BackupInvalidoException(VERSAO_MAIS_NOVA)

    val arquivo = try {
        val json = JSON.decodeFromJsonElement(BackupJson.serializer(), raiz)
        ArquivoDeBackup(exportadoEm = Instant.parse(json.exportadoEm), copia = json.paraCopia())
    } catch (e: IllegalArgumentException) {
        // Campo faltando ou com tipo errado (o SerializationException é um IllegalArgumentException)
        throw BackupInvalidoException(BACKUP_ALTERADO)
    } catch (e: DateTimeException) {
        throw BackupInvalidoException(BACKUP_ALTERADO) // data que não se lê
    }
    arquivo.copia.conferir()
    return arquivo
}

/** O que o arquivo traz, para a pessoa conferir antes de importar. */
fun ArquivoDeBackup.resumo() = ResumoDoBackup(
    exportadoEm = exportadoEm,
    alunos = copia.alunos.sortedBy { it.id }.map { it.nome },
    anosLetivos = copia.anosLetivos.size,
    avaliacoes = copia.avaliacoes.size,
)

internal const val MARCA_DO_APP = "app.jammes.boletim"
internal const val VERSAO_DO_ARQUIVO = 1

internal const val NAO_E_BACKUP = "Esse arquivo não é um backup do Boletim Escolar"
internal const val VERSAO_MAIS_NOVA = "Esse backup é de uma versão mais nova do Boletim Escolar. Atualize o app para importar"
internal const val BACKUP_ALTERADO = "O arquivo está incompleto ou foi alterado"
internal const val SEM_ANO_LETIVO = "O arquivo não tem nenhum aluno com ano letivo"

// O arquivo é pequeno, então vai formatado: dá para abrir e ler
private val JSON = Json { prettyPrint = true }

/**
 * Os ids não se repetem, e as chaves estrangeiras apontam para linhas do próprio arquivo. O banco
 * também recusaria, mas assim o problema aparece antes de a pessoa confirmar. E sem nenhum ano
 * letivo não há o que abrir depois de importar.
 */
private fun CopiaDoBanco.conferir() {
    val idsMaterias = materias.idsUnicos { it.id }
    val idsAlunos = alunos.idsUnicos { it.id }
    val idsAnos = anosLetivos.idsUnicos { it.id }
    val idsPeriodos = periodos.idsUnicos { it.id }
    val idsDisciplinas = disciplinas.idsUnicos { it.id }
    regras.idsUnicos { it.id }
    avaliacoes.idsUnicos { it.id }
    faltas.idsUnicos { it.id }

    val ligados = anosLetivos.all { it.alunoId in idsAlunos } &&
        periodos.all { it.anoLetivoId in idsAnos } &&
        disciplinas.all { it.materiaId in idsMaterias && it.anoLetivoId in idsAnos } &&
        regras.all { it.anoLetivoId in idsAnos && (it.disciplinaId == null || it.disciplinaId in idsDisciplinas) } &&
        avaliacoes.all { it.disciplinaId in idsDisciplinas && (it.periodoId == null || it.periodoId in idsPeriodos) } &&
        faltas.all { it.disciplinaId in idsDisciplinas && it.periodoId in idsPeriodos }
    if (!ligados) throw BackupInvalidoException(BACKUP_ALTERADO)

    // Todo ano aponta para um aluno do arquivo: com algum ano, há um aluno com ano letivo
    if (anosLetivos.isEmpty()) throw BackupInvalidoException(SEM_ANO_LETIVO)
}

private fun <T> List<T>.idsUnicos(id: (T) -> Long): Set<Long> {
    val ids = map(id).toSet()
    if (ids.size != size) throw BackupInvalidoException(BACKUP_ALTERADO)
    return ids
}

// O formato -----------------------------------------------------------------------------------
// Uma classe para cada tabela, com todas as colunas. Datas em ISO 8601 ("2026-10-15"), para o
// arquivo não depender de como o banco grava elas.

@Serializable
internal data class BackupJson(
    val app: String,
    val versao: Int,
    val exportadoEm: String,
    val materias: List<MateriaJson>,
    val alunos: List<AlunoJson>,
    val anosLetivos: List<AnoLetivoJson>,
    val periodos: List<PeriodoJson>,
    val disciplinas: List<DisciplinaJson>,
    val regras: List<RegraJson>,
    val avaliacoes: List<AvaliacaoJson>,
    val faltas: List<FaltaJson>,
)

@Serializable
internal data class MateriaJson(
    val id: Long,
    val nome: String,
    val abreviacao: String?,
    val cor: Int,
    val icone: String,
)

@Serializable
internal data class AlunoJson(
    val id: Long,
    val nome: String,
    val avatar: String?,
    val ativo: Int,
    val criadoEm: String,
)

@Serializable
internal data class AnoLetivoJson(
    val id: Long,
    val alunoId: Long,
    val ano: Int,
    val serie: String?,
    val tipoPeriodo: String,
    val qtdPeriodos: Int,
    val ativo: Int,
)

@Serializable
internal data class PeriodoJson(
    val id: Long,
    val anoLetivoId: Long,
    val periodo: Int,
    val dataInicio: String,
    val dataFim: String,
)

@Serializable
internal data class DisciplinaJson(
    val id: Long,
    val materiaId: Long,
    val anoLetivoId: Long,
    val nome: String,
    val abreviacao: String?,
    val cor: Int,
    val icone: String,
    val totalAulas: Int?,
    val professor: String?,
    val periodoInicio: String?,
    val periodoFim: String?,
    val ativa: Int,
    val ordem: Int,
)

@Serializable
internal data class RegraJson(
    val id: Long,
    val anoLetivoId: Long,
    val disciplinaId: Long?,
    val mediaMinima: Double,
    val mediaRecuperacao: Double?,
    val frequenciaMinima: Double,
    val tipoMedia: String,
    val arredondamento: String,
    val descartarMenorNota: Int,
)

@Serializable
internal data class AvaliacaoJson(
    val id: Long,
    val disciplinaId: Long,
    val periodoId: Long?,
    val nome: String,
    val nota: Double?,
    val notaMaxima: Double,
    val peso: Double,
    val tipo: String,
    val data: String?,
    val criadoEm: String,
)

@Serializable
internal data class FaltaJson(
    val id: Long,
    val disciplinaId: Long,
    val periodoId: Long,
    val data: String,
    val qtdAulas: Int,
)

// Do banco para o arquivo ---------------------------------------------------------------------

private fun CopiaDoBanco.paraJson(exportadoEm: Instant) = BackupJson(
    app = MARCA_DO_APP,
    versao = VERSAO_DO_ARQUIVO,
    exportadoEm = exportadoEm.toString(),
    materias = materias.map {
        MateriaJson(id = it.id, nome = it.nome, abreviacao = it.abreviacao, cor = it.cor, icone = it.icone)
    },
    alunos = alunos.map {
        AlunoJson(id = it.id, nome = it.nome, avatar = it.avatar, ativo = it.ativo, criadoEm = it.criadoEm.toString())
    },
    anosLetivos = anosLetivos.map {
        AnoLetivoJson(
            id = it.id,
            alunoId = it.alunoId,
            ano = it.ano,
            serie = it.serie,
            tipoPeriodo = it.tipoPeriodo,
            qtdPeriodos = it.qtdPeriodos,
            ativo = it.ativo,
        )
    },
    periodos = periodos.map {
        PeriodoJson(
            id = it.id,
            anoLetivoId = it.anoLetivoId,
            periodo = it.periodo,
            dataInicio = it.dataInicio.toString(),
            dataFim = it.dataFim.toString(),
        )
    },
    disciplinas = disciplinas.map {
        DisciplinaJson(
            id = it.id,
            materiaId = it.materiaId,
            anoLetivoId = it.anoLetivoId,
            nome = it.nome,
            abreviacao = it.abreviacao,
            cor = it.cor,
            icone = it.icone,
            totalAulas = it.totalAulas,
            professor = it.professor,
            periodoInicio = it.periodoInicio?.toString(),
            periodoFim = it.periodoFim?.toString(),
            ativa = it.ativa,
            ordem = it.ordem,
        )
    },
    regras = regras.map {
        RegraJson(
            id = it.id,
            anoLetivoId = it.anoLetivoId,
            disciplinaId = it.disciplinaId,
            mediaMinima = it.mediaMinima,
            mediaRecuperacao = it.mediaRecuperacao,
            frequenciaMinima = it.frequenciaMinima,
            tipoMedia = it.tipoMedia,
            arredondamento = it.arredondamento,
            descartarMenorNota = it.descartarMenorNota,
        )
    },
    avaliacoes = avaliacoes.map {
        AvaliacaoJson(
            id = it.id,
            disciplinaId = it.disciplinaId,
            periodoId = it.periodoId,
            nome = it.nome,
            nota = it.nota,
            notaMaxima = it.notaMaxima,
            peso = it.peso,
            tipo = it.tipo,
            data = it.data?.toString(),
            criadoEm = it.criadoEm.toString(),
        )
    },
    faltas = faltas.map {
        FaltaJson(
            id = it.id,
            disciplinaId = it.disciplinaId,
            periodoId = it.periodoId,
            data = it.data.toString(),
            qtdAulas = it.qtdAulas,
        )
    },
)

// Do arquivo para o banco ---------------------------------------------------------------------

private fun BackupJson.paraCopia() = CopiaDoBanco(
    materias = materias.map {
        MateriaEntity(id = it.id, nome = it.nome, abreviacao = it.abreviacao, cor = it.cor, icone = it.icone)
    },
    alunos = alunos.map {
        AlunoEntity(id = it.id, nome = it.nome, avatar = it.avatar, ativo = it.ativo, criadoEm = Instant.parse(it.criadoEm))
    },
    anosLetivos = anosLetivos.map {
        AnoLetivoEntity(
            id = it.id,
            alunoId = it.alunoId,
            ano = it.ano,
            serie = it.serie,
            tipoPeriodo = it.tipoPeriodo,
            qtdPeriodos = it.qtdPeriodos,
            ativo = it.ativo,
        )
    },
    periodos = periodos.map {
        PeriodoEntity(
            id = it.id,
            anoLetivoId = it.anoLetivoId,
            periodo = it.periodo,
            dataInicio = LocalDate.parse(it.dataInicio),
            dataFim = LocalDate.parse(it.dataFim),
        )
    },
    disciplinas = disciplinas.map {
        DisciplinaEntity(
            id = it.id,
            materiaId = it.materiaId,
            anoLetivoId = it.anoLetivoId,
            nome = it.nome,
            abreviacao = it.abreviacao,
            cor = it.cor,
            icone = it.icone,
            totalAulas = it.totalAulas,
            professor = it.professor,
            periodoInicio = it.periodoInicio?.let(LocalDate::parse),
            periodoFim = it.periodoFim?.let(LocalDate::parse),
            ativa = it.ativa,
            ordem = it.ordem,
        )
    },
    regras = regras.map {
        RegraAvaliacaoEntity(
            id = it.id,
            anoLetivoId = it.anoLetivoId,
            disciplinaId = it.disciplinaId,
            mediaMinima = it.mediaMinima,
            mediaRecuperacao = it.mediaRecuperacao,
            frequenciaMinima = it.frequenciaMinima,
            tipoMedia = it.tipoMedia,
            arredondamento = it.arredondamento,
            descartarMenorNota = it.descartarMenorNota,
        )
    },
    avaliacoes = avaliacoes.map {
        AvaliacaoEntity(
            id = it.id,
            disciplinaId = it.disciplinaId,
            periodoId = it.periodoId,
            nome = it.nome,
            nota = it.nota,
            notaMaxima = it.notaMaxima,
            peso = it.peso,
            tipo = it.tipo,
            data = it.data?.let(LocalDate::parse),
            criadoEm = Instant.parse(it.criadoEm),
        )
    },
    faltas = faltas.map {
        FaltaEntity(
            id = it.id,
            disciplinaId = it.disciplinaId,
            periodoId = it.periodoId,
            data = LocalDate.parse(it.data),
            qtdAulas = it.qtdAulas,
        )
    },
)

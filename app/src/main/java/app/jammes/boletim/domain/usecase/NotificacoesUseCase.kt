package app.jammes.boletim.domain.usecase

import app.jammes.boletim.domain.model.AlunoDomain
import app.jammes.boletim.domain.model.AnoLetivoDomain
import app.jammes.boletim.domain.model.AvaliacaoDomain
import app.jammes.boletim.domain.model.AvisoDoAluno
import app.jammes.boletim.domain.model.DisciplinaDados
import app.jammes.boletim.domain.model.HORARIOS_PADRAO
import app.jammes.boletim.domain.model.NotificacaoDomain
import app.jammes.boletim.domain.model.RegraAvaliacaoDomain
import app.jammes.boletim.domain.model.StatusDisciplina
import app.jammes.boletim.domain.model.TipoAvaliacao
import app.jammes.boletim.domain.model.TipoNotificacao
import app.jammes.boletim.domain.model.intervalo
import app.jammes.boletim.domain.model.nomeDe
import app.jammes.boletim.domain.model.nomeFeminino
import app.jammes.boletim.domain.repository.AgendaDosAvisos
import app.jammes.boletim.domain.repository.AlunoRepository
import app.jammes.boletim.domain.repository.AnoLetivoRepository
import app.jammes.boletim.domain.repository.AvisosNoCelular
import app.jammes.boletim.domain.repository.ConfiguracoesRepository
import app.jammes.boletim.domain.repository.DisciplinaRepository
import app.jammes.boletim.domain.repository.NotificacaoRepository
import app.jammes.boletim.domain.repository.RegraAvaliacaoRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.time.Instant

// ---------------------------------------------------------------------------
// Regras puras: o que vira notificação
// ---------------------------------------------------------------------------

/** O ano letivo de um aluno com o que as regras olham: as disciplinas, com as avaliações e as faltas, e as regras de média. */
data class AnoParaNotificar(
    val alunoId: Long,
    val anoLetivo: AnoLetivoDomain,
    val disciplinas: List<DisciplinaDados>,
    val regras: List<RegraAvaliacaoDomain>,
)

/** A nota é cobrada a partir de 2 dias depois da avaliação: dá tempo de o professor corrigir. */
private const val DIAS_ATE_COBRAR_A_NOTA = 2L

/** Avaliações mais velhas que isso não são cobradas: quem começa a usar o app no meio do ano não recebe uma enxurrada. */
private const val DIAS_PARA_COBRAR_A_NOTA = 30L

/** O fim do período é avisado nos últimos 5 dias. */
private const val DIAS_PARA_O_FIM_DO_PERIODO = 5L

/** "Hoje tem prova" só sai de manhã: à tarde, a prova já passou. */
private val MEIO_DIA: LocalTime = LocalTime.NOON

/**
 * As notificações que o momento [agora] pede para o aluno, no ano letivo de [dados]. Só as das
 * disciplinas em curso: as finalizadas não recebem mais nada.
 *
 * Cada uma tem a chave do que ela avisa, e gerar de novo dá as mesmas chaves: o banco não grava a
 * repetida, e o aviso sai uma vez só. Vêm do menos para o mais urgente, porque a lista mostra primeiro
 * a gravada por último entre as do mesmo instante: as avaliações ficam no topo.
 */
fun gerarNotificacoes(dados: AnoParaNotificar, agora: LocalDateTime, criadaEm: Instant): List<NotificacaoDomain> {
    val emCurso = dados.disciplinas.filter { it.ativa }
    if (emCurso.isEmpty()) return emptyList()
    val gerador = Gerador(dados, agora, criadaEm)
    return gerador.fimDosPeriodos() +
        gerador.faltas(emCurso) +
        gerador.medias(emCurso) +
        gerador.notasPendentes(emCurso) +
        gerador.avaliacoesChegando(emCurso)
}

/** As regras, uma por tipo de notificação, com o que todas usam à mão. */
private class Gerador(
    private val dados: AnoParaNotificar,
    private val agora: LocalDateTime,
    private val criadaEm: Instant,
) {
    private val hoje: LocalDate = agora.toLocalDate()
    private val ano = dados.anoLetivo

    /** O período que está chegando ao fim: a última semana dele. */
    fun fimDosPeriodos(): List<NotificacaoDomain> = ano.periodo.mapNotNull { periodo ->
        val dias = ChronoUnit.DAYS.between(hoje, periodo.dataFim)
        if (dias !in 0..DIAS_PARA_O_FIM_DO_PERIODO) return@mapNotNull null
        val artigo = if (ano.tipoPeriodo.nomeFeminino) "A" else "O"
        val quando = when (dias) {
            0L -> "termina hoje"
            1L -> "termina amanhã"
            else -> "termina em $dias dias"
        }
        notificacao(
            tipo = TipoNotificacao.PERIODO,
            chave = "periodo:${periodo.id}",
            titulo = "$artigo ${ano.tipoPeriodo.nomeDe(periodo.periodo)} $quando",
            texto = "Confira se todas as notas já foram lançadas.",
            periodoId = periodo.id,
        )
    }

    /**
     * A disciplina perto do limite de faltas do ano, e depois no limite (ou além dele): um aviso de
     * cada. Só durante o ano letivo e com o total de aulas informado, que é o que dá o limite.
     */
    fun faltas(disciplinas: List<DisciplinaDados>): List<NotificacaoDomain> {
        val doAno = ano.periodo.intervalo() ?: return emptyList()
        if (hoje !in doAno) return emptyList()
        return disciplinas.mapNotNull { disciplina ->
            val regra = dados.regras.paraDisciplina(disciplina.disciplinaId)
            val limite = CalcularFrequencia.limiteFaltas(disciplina.totalAulas, regra) ?: return@mapNotNull null
            val faltas = disciplina.faltas.sumOf { it.qtdAulas }
            val restantes = limite - faltas
            val (chave, titulo) = when {
                faltas == 0 -> return@mapNotNull null
                restantes < 0 -> "limite" to "${disciplina.nome} passou do limite de faltas"
                restantes == 0 -> "limite" to "${disciplina.nome} chegou ao limite de faltas"
                restantes <= margemDeFaltas(limite) -> "perto" to "${disciplina.nome} perto do limite de faltas"
                else -> return@mapNotNull null
            }
            notificacao(
                tipo = TipoNotificacao.FALTAS,
                chave = "faltas:${disciplina.disciplinaId}:$chave",
                titulo = titulo,
                texto = "Faltas no ano: $faltas de $limite permitidas.",
                disciplinaId = disciplina.disciplinaId,
            )
        }
    }

    /** A média do período de hoje abaixo da mínima: um aviso por disciplina em cada período. */
    fun medias(disciplinas: List<DisciplinaDados>): List<NotificacaoDomain> {
        val periodo = ano.periodo.firstOrNull { hoje in it.dataInicio..it.dataFim } ?: return emptyList()
        return disciplinas.mapNotNull { disciplina ->
            val regra = dados.regras.paraDisciplina(disciplina.disciplinaId)
            val doPeriodo = disciplina.avaliacoes.filter { it.periodoId == periodo.id }
            val media = CalcularMediaDoPeriodo(doPeriodo, regra).media ?: return@mapNotNull null
            if (statusDaMedia(media, regra) != StatusDisciplina.ABAIXO) return@mapNotNull null
            notificacao(
                tipo = TipoNotificacao.MEDIA,
                chave = "media:${disciplina.disciplinaId}:${periodo.id}",
                titulo = "Média de ${disciplina.nome} abaixo da mínima",
                texto = "${ano.tipoPeriodo.nomeDe(periodo.periodo)}: ${mediaEmTexto(media)}. " +
                    "A mínima é ${mediaEmTexto(regra.mediaMinima)}.",
                periodoId = periodo.id,
                disciplinaId = disciplina.disciplinaId,
            )
        }
    }

    /** A avaliação que já passou e continua sem nota: um lembrete só, de 2 a 30 dias depois dela. */
    fun notasPendentes(disciplinas: List<DisciplinaDados>): List<NotificacaoDomain> =
        disciplinas.flatMap { disciplina ->
            disciplina.avaliacoes.mapNotNull { avaliacao ->
                val data = avaliacao.data ?: return@mapNotNull null
                if (avaliacao.nota != null) return@mapNotNull null
                if (ChronoUnit.DAYS.between(data, hoje) !in DIAS_ATE_COBRAR_A_NOTA..DIAS_PARA_COBRAR_A_NOTA) {
                    return@mapNotNull null
                }
                notificacao(
                    tipo = TipoNotificacao.NOTA,
                    chave = "nota:${avaliacao.id}",
                    titulo = "${nomeDaAvaliacao(avaliacao)} de ${disciplina.nome} sem nota",
                    texto = "Foi em ${data.format(DIA_E_MES)}. Se a nota já saiu, lance no app.",
                    periodoId = periodoDaAvaliacao(avaliacao),
                    disciplinaId = disciplina.disciplinaId,
                    avaliacaoId = avaliacao.id,
                )
            }
        }

    /**
     * A avaliação de amanhã, e a de hoje enquanto é de manhã. A chave leva a data: remarcada para
     * outro dia, ela é avisada de novo.
     */
    fun avaliacoesChegando(disciplinas: List<DisciplinaDados>): List<NotificacaoDomain> =
        disciplinas.flatMap { disciplina ->
            disciplina.avaliacoes.mapNotNull { avaliacao ->
                val data = avaliacao.data ?: return@mapNotNull null
                if (avaliacao.nota != null) return@mapNotNull null
                val quando = when {
                    data == hoje.plusDays(1) -> "amanhã"
                    data == hoje && agora.toLocalTime() < MEIO_DIA -> "hoje"
                    else -> return@mapNotNull null
                }
                notificacao(
                    tipo = TipoNotificacao.AVALIACAO,
                    chave = "avaliacao:${avaliacao.id}:$data",
                    titulo = "${nomeDaAvaliacao(avaliacao)} de ${disciplina.nome} $quando",
                    texto = data.format(DIA_DA_SEMANA_E_DATA).replaceFirstChar { it.titlecase(PT_BR) } + ".",
                    periodoId = periodoDaAvaliacao(avaliacao),
                    disciplinaId = disciplina.disciplinaId,
                    avaliacaoId = avaliacao.id,
                )
            }
        }

    private fun notificacao(
        tipo: TipoNotificacao,
        chave: String,
        titulo: String,
        texto: String,
        periodoId: Long? = null,
        disciplinaId: Long? = null,
        avaliacaoId: Long? = null,
    ) = NotificacaoDomain(
        alunoId = dados.alunoId,
        tipo = tipo,
        chave = chave,
        titulo = titulo,
        texto = texto,
        criadaEm = criadaEm,
        anoLetivoId = ano.id,
        periodoId = periodoId,
        disciplinaId = disciplinaId,
        avaliacaoId = avaliacaoId,
    )
}

/** Perto do limite: nas últimas 2 faltas, ou no último quinto do limite quando ele é grande. */
internal fun margemDeFaltas(limite: Int): Int = maxOf(2, limite / 5)

/** O nome que a pessoa deu; sem nome, o tipo: "Avaliação", "Recuperação", "Final". */
private fun nomeDaAvaliacao(avaliacao: AvaliacaoDomain): String =
    avaliacao.nome.trim().ifEmpty {
        when (avaliacao.tipo) {
            TipoAvaliacao.NORMAL -> "Avaliação"
            TipoAvaliacao.RECUPERACAO -> "Recuperação"
            TipoAvaliacao.FINAL -> "Final"
        }
    }

/** O período em que a tela mostra a avaliação. A final é do ano: aparece só no ano letivo inteiro. */
private fun periodoDaAvaliacao(avaliacao: AvaliacaoDomain): Long? =
    if (avaliacao.tipo == TipoAvaliacao.FINAL) null else avaliacao.periodoId

private val PT_BR = Locale.forLanguageTag("pt-BR")
private val DIA_DA_SEMANA_E_DATA = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", PT_BR)
private val DIA_E_MES = DateTimeFormatter.ofPattern("d 'de' MMMM", PT_BR)

// ---------------------------------------------------------------------------
// Regras puras: quando avisar e como chamar cada aluno
// ---------------------------------------------------------------------------

/**
 * O próximo dos [horarios] depois de [agora]: hoje, se ainda não passou, senão amanhã. Sem horário
 * nenhum, vale o padrão.
 */
fun proximoHorario(agora: ZonedDateTime, horarios: List<LocalTime>): ZonedDateTime {
    val hoje = agora.toLocalDate()
    return horarios.ifEmpty { HORARIOS_PADRAO }
        .map { horario ->
            val deHoje = ZonedDateTime.of(hoje, horario, agora.zone)
            if (deHoje.isAfter(agora)) deHoje else ZonedDateTime.of(hoje.plusDays(1), horario, agora.zone)
        }
        .min()
}

/**
 * Como o app chama o aluno [alunoId] na lista e no aviso do celular: pelo primeiro nome, ou pelo nome
 * inteiro quando outro aluno do aparelho tem o mesmo primeiro nome. null com um aluno só, porque aí
 * não há de quem mais ser.
 */
internal fun nomeNaLista(alunos: List<AlunoDomain>, alunoId: Long): String? {
    if (alunos.size < 2) return null
    val aluno = alunos.find { it.id == alunoId } ?: return null
    val primeiro = primeiroNome(aluno.nome)
    val repetido = alunos.any { it.id != alunoId && primeiroNome(it.nome).equals(primeiro, ignoreCase = true) }
    return if (repetido) aluno.nome.trim() else primeiro
}

private fun primeiroNome(nome: String): String = nome.trim().substringBefore(' ')

/** O aviso do celular de cada aluno que tem notificação não lida, na ordem do cadastro. */
internal fun montarAvisos(alunos: List<AlunoDomain>, naoLidas: List<NotificacaoDomain>): List<AvisoDoAluno> {
    val porAluno = naoLidas.groupBy { it.alunoId }
    return alunos.sortedBy { it.id }.mapNotNull { aluno ->
        val doAluno = porAluno[aluno.id] ?: return@mapNotNull null
        AvisoDoAluno(alunoId = aluno.id, nome = nomeNaLista(alunos, aluno.id), notificacoes = doAluno)
    }
}

/**
 * Dos [pendentes], os avisos que o horário põe na barra de novo: os dos alunos com alguma notificação
 * ainda não avisada e não lida. Uma que a pessoa já viu no app, antes do horário, não faz o aviso
 * do aluno voltar.
 */
internal fun avisosComNovidade(pendentes: List<AvisoDoAluno>, naoAvisadas: List<NotificacaoDomain>): List<AvisoDoAluno> {
    val comNovidade = naoAvisadas.filterNot { it.lida }.map { it.alunoId }.toSet()
    return pendentes.filter { it.alunoId in comNovidade }
}

// ---------------------------------------------------------------------------
// Casos de uso
// ---------------------------------------------------------------------------

/**
 * O que roda em cada horário das notificações. Gera as de cada aluno, no ano letivo deste ano, e
 * mostra no celular as que ainda não foram avisadas. Devolve quantas notificações novas entraram.
 * Com as notificações desligadas, não faz nada.
 */
class AvisarNotificacoes @Inject constructor(
    private val configuracoesRepository: ConfiguracoesRepository,
    private val alunoRepository: AlunoRepository,
    private val anoLetivoRepository: AnoLetivoRepository,
    private val disciplinaRepository: DisciplinaRepository,
    private val regraRepository: RegraAvaliacaoRepository,
    private val notificacaoRepository: NotificacaoRepository,
    private val avisos: AvisosNoCelular,
) {
    suspend operator fun invoke(agora: ZonedDateTime): Int {
        if (!configuracoesRepository.configuracoes.first().notificacoesAtivas) return 0
        val criadaEm = Instant.fromEpochMilliseconds(agora.toInstant().toEpochMilli())
        val alunos = alunoRepository.observeAluno().first()

        var novas = 0
        for (aluno in alunos) {
            // O ano letivo do calendário de hoje; os anos que já passaram não avisam mais nada
            val ano = anoLetivoRepository.observeByAluno(aluno.id).first()
                .firstOrNull { it.ano == agora.year } ?: continue
            val dados = AnoParaNotificar(
                alunoId = aluno.id,
                anoLetivo = ano,
                disciplinas = disciplinaRepository.observeBoletim(ano.id).first(),
                regras = regraRepository.observeByAnoLetivo(ano.id).first(),
            )
            novas += notificacaoRepository.inserir(gerarNotificacoes(dados, agora.toLocalDateTime(), criadaEm))
        }

        avisarNoCelular(alunos)
        return novas
    }

    /**
     * As que ainda não foram avisadas vão para a barra do celular. As que a pessoa já viu no app, antes
     * do horário, não tocam, mas também contam como avisadas. Sem a permissão do Android, ficam para o
     * primeiro horário depois que ela vier.
     */
    private suspend fun avisarNoCelular(alunos: List<AlunoDomain>) {
        if (!avisos.podeMostrar()) return
        val naoAvisadas = notificacaoRepository.naoAvisadas()
        if (naoAvisadas.isEmpty()) return
        val pendentes = montarAvisos(alunos, notificacaoRepository.todasNaoLidas())
        val novos = avisosComNovidade(pendentes, naoAvisadas)
        if (novos.isNotEmpty()) avisos.mostrar(novos = novos, pendentes = pendentes)
        notificacaoRepository.marcarComoAvisadas(naoAvisadas.map { it.id })
    }
}

/** O aluno viu as notificações dele no app: o aviso dele sai da barra do celular, e o resumo fica com os outros. */
class RetirarAvisoDoAluno @Inject constructor(
    private val alunoRepository: AlunoRepository,
    private val notificacaoRepository: NotificacaoRepository,
    private val avisos: AvisosNoCelular,
) {
    suspend operator fun invoke(alunoId: Long) {
        val alunos = alunoRepository.observeAluno().first()
        val dosOutros = notificacaoRepository.todasNaoLidas().filter { it.alunoId != alunoId }
        avisos.retirar(alunoId, montarAvisos(alunos, dosOutros))
    }
}

/** Marca o despertador para o próximo horário das notificações, ou desmarca quando elas estão desligadas. */
class ReagendarAvisos @Inject constructor(
    private val configuracoesRepository: ConfiguracoesRepository,
    private val agenda: AgendaDosAvisos,
) {
    suspend operator fun invoke(agora: ZonedDateTime = ZonedDateTime.now()) {
        val configuracoes = configuracoesRepository.configuracoes.first()
        if (configuracoes.notificacoesAtivas) {
            agenda.marcar(proximoHorario(agora, configuracoes.horariosDasNotificacoes))
        } else {
            agenda.desmarcar()
        }
    }
}

/**
 * Enquanto o app roda, cada mudança na chave ou nos horários marca o despertador de novo; a primeira
 * leitura também, o que refaz a marcação a cada vez que o app abre. Não termina.
 */
class AcompanharHorariosDosAvisos @Inject constructor(
    private val configuracoesRepository: ConfiguracoesRepository,
    private val reagendar: ReagendarAvisos,
) {
    suspend operator fun invoke() {
        configuracoesRepository.configuracoes
            .map { it.notificacoesAtivas to it.horariosDasNotificacoes }
            .distinctUntilChanged()
            .collect { reagendar() }
    }
}

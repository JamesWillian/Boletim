package app.jammes.boletim.domain.usecase

import app.jammes.boletim.domain.model.AlunoDomain
import app.jammes.boletim.domain.model.AnoLetivoDomain
import app.jammes.boletim.domain.model.AvaliacaoDomain
import app.jammes.boletim.domain.model.AvisoDoAluno
import app.jammes.boletim.domain.model.DisciplinaDados
import app.jammes.boletim.domain.model.FaltaDomain
import app.jammes.boletim.domain.model.IconeMateria
import app.jammes.boletim.domain.model.NotificacaoDomain
import app.jammes.boletim.domain.model.PeriodoDomain
import app.jammes.boletim.domain.model.RegraAvaliacaoDomain
import app.jammes.boletim.domain.model.TipoAvaliacao
import app.jammes.boletim.domain.model.TipoNotificacao
import app.jammes.boletim.domain.model.TipoPeriodo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.time.Instant

/**
 * O que vira notificação, quando o próximo horário toca e o aviso de cada aluno no celular. O ano
 * letivo de exemplo é 2026 em bimestres, e o "agora" padrão é um sábado, 17/10, às 19h, no 4º Bimestre.
 */
class NotificacoesUseCaseTest {

    // Fim do período -------------------------------------------------------------------------

    @Test fun `o periodo que termina em ate 5 dias avisa uma vez, com a chave dele`() {
        val geradas = gerar(dados(disciplina()), agora = "2026-09-29T19:00") // o 3º termina na sexta, 02/10

        val aviso = geradas.single()
        assertEquals(TipoNotificacao.PERIODO, aviso.tipo)
        assertEquals("periodo:13", aviso.chave)
        assertEquals("O 3º Bimestre termina em 3 dias", aviso.titulo)
        assertEquals("Confira se todas as notas já foram lançadas.", aviso.texto)
        assertEquals(13L, aviso.periodoId)
        assertEquals(null, aviso.disciplinaId)
    }

    @Test fun `na vespera e no ultimo dia do periodo, amanha e hoje`() {
        assertEquals("O 3º Bimestre termina amanhã", gerar(dados(disciplina()), agora = "2026-10-01T19:00").single().titulo)
        assertEquals("O 3º Bimestre termina hoje", gerar(dados(disciplina()), agora = "2026-10-02T07:00").single().titulo)
    }

    @Test fun `6 dias antes do fim, e depois dele, o periodo nao avisa`() {
        assertEquals(emptyList<NotificacaoDomain>(), gerar(dados(disciplina()), agora = "2026-09-26T19:00"))
        assertEquals(emptyList<NotificacaoDomain>(), gerar(dados(disciplina()), agora = "2026-10-03T19:00"))
    }

    @Test fun `unidade e feminina`() {
        val geradas = gerar(dados(disciplina(), ano = ano(TipoPeriodo.UNIDADE)), agora = "2026-09-29T19:00")

        assertEquals("A 3ª Unidade termina em 3 dias", geradas.single().titulo)
    }

    // Faltas ---------------------------------------------------------------------------------

    @Test fun `perto do limite de faltas nas ultimas 2`() {
        // 75% de 40 aulas: até 10 faltas. Com 8, faltam 2
        val geradas = gerar(dados(disciplina(totalAulas = 40, faltas = listOf(falta(3), falta(3), falta(2)))))

        val aviso = geradas.single()
        assertEquals(TipoNotificacao.FALTAS, aviso.tipo)
        assertEquals("faltas:3:perto", aviso.chave)
        assertEquals("Matemática perto do limite de faltas", aviso.titulo)
        assertEquals("Faltas no ano: 8 de 10 permitidas.", aviso.texto)
        assertEquals(3L, aviso.disciplinaId)
        assertEquals(null, aviso.periodoId) // o limite é do ano
    }

    @Test fun `longe do limite, nada`() {
        assertEquals(emptyList<NotificacaoDomain>(), gerar(dados(disciplina(totalAulas = 40, faltas = listOf(falta(7))))))
    }

    @Test fun `no limite e alem dele, o mesmo aviso`() {
        val no = gerar(dados(disciplina(totalAulas = 40, faltas = listOf(falta(10))))).single()
        val alem = gerar(dados(disciplina(totalAulas = 40, faltas = listOf(falta(12))))).single()

        assertEquals("Matemática chegou ao limite de faltas", no.titulo)
        assertEquals("Matemática passou do limite de faltas", alem.titulo)
        // Quem já foi avisado no limite não é avisado de novo ao passar dele
        assertEquals("faltas:3:limite", no.chave)
        assertEquals(no.chave, alem.chave)
    }

    @Test fun `com um limite grande, perto e o ultimo quinto`() {
        // 75% de 120 aulas: até 30 faltas, e perto é a partir de 24
        assertEquals(emptyList<NotificacaoDomain>(), gerar(dados(disciplina(totalAulas = 120, faltas = listOf(falta(23))))))
        assertEquals("faltas:3:perto", gerar(dados(disciplina(totalAulas = 120, faltas = listOf(falta(24))))).single().chave)
    }

    @Test fun `a margem de faltas e de pelo menos 2`() {
        assertEquals(2, margemDeFaltas(3))
        assertEquals(2, margemDeFaltas(10))
        assertEquals(2, margemDeFaltas(14))
        assertEquals(3, margemDeFaltas(15))
        assertEquals(6, margemDeFaltas(30))
    }

    @Test fun `sem nenhuma falta nao avisa, mesmo com o limite zero`() {
        // 75% de 3 aulas: nenhuma falta permitida
        assertEquals(emptyList<NotificacaoDomain>(), gerar(dados(disciplina(totalAulas = 3))))
    }

    @Test fun `sem o total de aulas nao ha limite, e as faltas nao avisam`() {
        assertEquals(emptyList<NotificacaoDomain>(), gerar(dados(disciplina(totalAulas = null, faltas = listOf(falta(30))))))
    }

    @Test fun `antes e depois do ano letivo, as faltas nao avisam`() {
        val dados = dados(disciplina(totalAulas = 40, faltas = listOf(falta(9))))

        assertEquals(emptyList<NotificacaoDomain>(), gerar(dados, agora = "2026-01-20T19:00"))
        assertEquals(emptyList<NotificacaoDomain>(), gerar(dados, agora = "2027-01-10T19:00"))
    }

    @Test fun `a frequencia minima da disciplina vale sobre a do ano`() {
        // A disciplina pede 90%: de 40 aulas, só 4 faltas. Com 3, falta 1
        val regras = listOf(regra(), regra(disciplinaId = 3, frequenciaMinima = 90.0))

        val aviso = gerar(dados(disciplina(totalAulas = 40, faltas = listOf(falta(3))), regras = regras)).single()

        assertEquals("Faltas no ano: 3 de 4 permitidas.", aviso.texto)
    }

    // Média ----------------------------------------------------------------------------------

    @Test fun `media do periodo de hoje abaixo da minima avisa`() {
        val avaliacoes = listOf(
            avaliacao(id = 1, nota = 5.0, data = "2026-10-06"),
            avaliacao(id = 2, nota = 6.0, data = "2026-10-08"),
        )

        val aviso = gerar(dados(disciplina(avaliacoes = avaliacoes))).single()

        assertEquals(TipoNotificacao.MEDIA, aviso.tipo)
        assertEquals("media:3:14", aviso.chave)
        assertEquals("Média de Matemática abaixo da mínima", aviso.titulo)
        assertEquals("4º Bimestre: 5,5. A mínima é 7,0.", aviso.texto)
        assertEquals(14L, aviso.periodoId)
        assertEquals(3L, aviso.disciplinaId)
        assertEquals(null, aviso.avaliacaoId)
    }

    @Test fun `media de outro periodo nao avisa`() {
        val doTerceiro = listOf(avaliacao(id = 1, nota = 3.0, data = "2026-09-01", periodoId = 13))

        assertEquals(emptyList<NotificacaoDomain>(), gerar(dados(disciplina(avaliacoes = doTerceiro))))
    }

    @Test fun `media na minima ou acima dela nao avisa`() {
        val avaliacoes = listOf(avaliacao(id = 1, nota = 7.0, data = "2026-10-06"))

        assertEquals(emptyList<NotificacaoDomain>(), gerar(dados(disciplina(avaliacoes = avaliacoes))))
    }

    @Test fun `a media minima da disciplina vale sobre a do ano`() {
        val avaliacoes = listOf(avaliacao(id = 1, nota = 5.5, data = "2026-10-06"))
        val regras = listOf(regra(), regra(disciplinaId = 3, mediaMinima = 5.0))

        assertEquals(emptyList<NotificacaoDomain>(), gerar(dados(disciplina(avaliacoes = avaliacoes), regras = regras)))
    }

    @Test fun `nas ferias entre dois periodos nao ha media para avisar`() {
        val avaliacoes = listOf(avaliacao(id = 1, nota = 3.0, data = "2026-06-01", periodoId = 12))

        val geradas = gerar(dados(disciplina(avaliacoes = avaliacoes)), agora = "2026-07-15T19:00")

        assertEquals(emptyList<NotificacaoDomain>(), geradas)
    }

    // Nota pendente --------------------------------------------------------------------------

    @Test fun `avaliacao de 2 dias atras sem nota cobra a nota`() {
        val avaliacoes = listOf(avaliacao(id = 40, nota = null, data = "2026-10-15"))

        val aviso = gerar(dados(disciplina(avaliacoes = avaliacoes))).single()

        assertEquals(TipoNotificacao.NOTA, aviso.tipo)
        assertEquals("nota:40", aviso.chave)
        assertEquals("Prova de Matemática sem nota", aviso.titulo)
        assertEquals("Foi em 15 de outubro. Se a nota já saiu, lance no app.", aviso.texto)
        assertEquals(14L, aviso.periodoId)
        assertEquals(3L, aviso.disciplinaId)
        assertEquals(40L, aviso.avaliacaoId)
    }

    @Test fun `a nota e cobrada de 2 a 30 dias depois da avaliacao`() {
        fun cobra(data: String) = gerar(dados(disciplina(avaliacoes = listOf(avaliacao(id = 40, nota = null, data = data)))))
            .any { it.tipo == TipoNotificacao.NOTA }

        assertEquals(false, cobra("2026-10-16")) // ontem: o professor ainda está corrigindo
        assertEquals(true, cobra("2026-09-17")) // 30 dias
        assertEquals(false, cobra("2026-09-16")) // 31 dias: velha demais
    }

    @Test fun `avaliacao com nota ou sem data nao cobra nada`() {
        val avaliacoes = listOf(
            avaliacao(id = 40, nota = 8.0, data = "2026-10-15"),
            avaliacao(id = 41, nota = null, data = null),
        )

        assertEquals(emptyList<NotificacaoDomain>(), gerar(dados(disciplina(avaliacoes = avaliacoes))))
    }

    @Test fun `sem nome, a avaliacao e chamada pelo tipo`() {
        val avaliacoes = listOf(avaliacao(id = 40, nota = null, data = "2026-10-15", nome = "  ", tipo = TipoAvaliacao.RECUPERACAO))

        assertEquals("Recuperação de Matemática sem nota", gerar(dados(disciplina(avaliacoes = avaliacoes))).single().titulo)
    }

    @Test fun `a final e do ano, e abre sem periodo`() {
        val avaliacoes = listOf(avaliacao(id = 40, nota = null, data = "2026-10-15", nome = "", tipo = TipoAvaliacao.FINAL))

        val aviso = gerar(dados(disciplina(avaliacoes = avaliacoes))).single()

        assertEquals("Final de Matemática sem nota", aviso.titulo)
        assertEquals(null, aviso.periodoId)
    }

    // Avaliação chegando ---------------------------------------------------------------------

    @Test fun `avaliacao de amanha avisa na vespera`() {
        val avaliacoes = listOf(avaliacao(id = 41, nota = null, data = "2026-10-18"))

        val aviso = gerar(dados(disciplina(avaliacoes = avaliacoes))).single()

        assertEquals(TipoNotificacao.AVALIACAO, aviso.tipo)
        assertEquals("avaliacao:41:2026-10-18", aviso.chave)
        assertEquals("Prova de Matemática amanhã", aviso.titulo)
        assertEquals("Domingo, 18 de outubro.", aviso.texto)
        assertEquals(14L, aviso.periodoId)
        assertEquals(41L, aviso.avaliacaoId)
    }

    @Test fun `a avaliacao de hoje so avisa de manha`() {
        val dados = dados(disciplina(avaliacoes = listOf(avaliacao(id = 41, nota = null, data = "2026-10-17"))))

        val cedo = gerar(dados, agora = "2026-10-17T07:00").single()
        assertEquals("Prova de Matemática hoje", cedo.titulo)
        assertEquals("Sábado, 17 de outubro.", cedo.texto)
        assertEquals(1, gerar(dados, agora = "2026-10-17T11:59").size)
        assertEquals(emptyList<NotificacaoDomain>(), gerar(dados, agora = "2026-10-17T12:00"))
    }

    @Test fun `o amanha da vespera e o hoje da manha seguinte sao o mesmo aviso`() {
        val dados = dados(disciplina(avaliacoes = listOf(avaliacao(id = 41, nota = null, data = "2026-10-18"))))

        val naVespera = gerar(dados, agora = "2026-10-17T19:00").single()
        val noDia = gerar(dados, agora = "2026-10-18T07:00").single()

        // A mesma chave: o banco não grava o segundo, e a prova é avisada uma vez só
        assertEquals(naVespera.chave, noDia.chave)
    }

    @Test fun `remarcada para outro dia, a avaliacao e avisada de novo`() {
        val antes = gerar(dados(disciplina(avaliacoes = listOf(avaliacao(id = 41, nota = null, data = "2026-10-18")))))
        val depois = gerar(
            dados(disciplina(avaliacoes = listOf(avaliacao(id = 41, nota = null, data = "2026-10-21")))),
            agora = "2026-10-20T19:00",
        )

        assertNotEquals(antes.single().chave, depois.single().chave)
    }

    @Test fun `avaliacao daqui a 2 dias, ou ja com nota, nao avisa`() {
        val avaliacoes = listOf(
            avaliacao(id = 41, nota = null, data = "2026-10-19"),
            avaliacao(id = 42, nota = 9.0, data = "2026-10-18"),
        )

        assertEquals(emptyList<NotificacaoDomain>(), gerar(dados(disciplina(avaliacoes = avaliacoes))))
    }

    // Todas ----------------------------------------------------------------------------------

    @Test fun `disciplina finalizada nao recebe nada`() {
        val finalizada = disciplina(
            avaliacoes = listOf(avaliacao(id = 41, nota = null, data = "2026-10-18")),
            totalAulas = 40,
            faltas = listOf(falta(10)),
            ativa = false,
        )

        assertEquals(emptyList<NotificacaoDomain>(), gerar(dados(finalizada, disciplina(id = 4, nome = "Português"))))
    }

    @Test fun `sem disciplina em curso, nem o fim do periodo avisa`() {
        val geradas = gerar(dados(disciplina(ativa = false)), agora = "2026-09-29T19:00")

        assertEquals(emptyList<NotificacaoDomain>(), geradas)
    }

    @Test fun `vem do menos para o mais urgente, com o aluno, o ano e o instante`() {
        // 15/12, uma terça: o 4º Bimestre termina na sexta
        val disciplina = disciplina(
            avaliacoes = listOf(
                avaliacao(id = 1, nota = 5.0, data = "2026-10-06"), // a média do 4º: 5,0
                avaliacao(id = 40, nota = null, data = "2026-12-10"), // sem nota
                avaliacao(id = 41, nota = null, data = "2026-12-16"), // amanhã
            ),
            totalAulas = 40,
            faltas = listOf(falta(9)),
        )

        val geradas = gerar(dados(disciplina), agora = "2026-12-15T19:00")

        assertEquals(
            listOf(TipoNotificacao.PERIODO, TipoNotificacao.FALTAS, TipoNotificacao.MEDIA, TipoNotificacao.NOTA, TipoNotificacao.AVALIACAO),
            geradas.map { it.tipo },
        )
        assertTrue(geradas.all { it.alunoId == 7L && it.anoLetivoId == 1L && it.criadaEm == CRIADA_EM })
        assertTrue(geradas.none { it.lida || it.avisada })
    }

    // O próximo horário ----------------------------------------------------------------------

    @Test fun `o proximo horario e hoje, se ainda nao passou`() {
        assertEquals(
            ZonedDateTime.of(2026, 10, 17, 19, 0, 0, 0, SAO_PAULO),
            proximoHorario(momento("2026-10-17T18:00"), listOf(LocalTime.of(19, 0))),
        )
    }

    @Test fun `no proprio horario, o proximo e o de amanha`() {
        assertEquals(
            ZonedDateTime.of(2026, 10, 18, 19, 0, 0, 0, SAO_PAULO),
            proximoHorario(momento("2026-10-17T19:00"), listOf(LocalTime.of(19, 0))),
        )
    }

    @Test fun `com varios horarios, o mais cedo depois de agora`() {
        val horarios = listOf(LocalTime.of(19, 0), LocalTime.of(7, 0), LocalTime.of(12, 30))

        assertEquals(ZonedDateTime.of(2026, 10, 17, 7, 0, 0, 0, SAO_PAULO), proximoHorario(momento("2026-10-17T06:00"), horarios))
        assertEquals(ZonedDateTime.of(2026, 10, 17, 19, 0, 0, 0, SAO_PAULO), proximoHorario(momento("2026-10-17T12:45"), horarios))
        assertEquals(ZonedDateTime.of(2026, 10, 18, 7, 0, 0, 0, SAO_PAULO), proximoHorario(momento("2026-10-17T20:00"), horarios))
    }

    @Test fun `sem horario nenhum, vale o padrao`() {
        assertEquals(ZonedDateTime.of(2026, 10, 17, 19, 0, 0, 0, SAO_PAULO), proximoHorario(momento("2026-10-17T10:00"), emptyList()))
    }

    // De quem são ----------------------------------------------------------------------------

    @Test fun `com um aluno so, o nome nao aparece`() {
        assertEquals(null, nomeNaLista(listOf(aluno(1, "James Willian")), alunoId = 1))
    }

    @Test fun `com mais de um aluno, o primeiro nome`() {
        val alunos = listOf(aluno(1, "James Willian"), aluno(2, "Maria Clara"))

        assertEquals("James", nomeNaLista(alunos, alunoId = 1))
        assertEquals("Maria", nomeNaLista(alunos, alunoId = 2))
    }

    @Test fun `com o mesmo primeiro nome de outro aluno, o nome inteiro`() {
        val alunos = listOf(aluno(1, "Maria Clara"), aluno(2, "maria Eduarda "), aluno(3, "Pedro"))

        assertEquals("Maria Clara", nomeNaLista(alunos, alunoId = 1))
        assertEquals("maria Eduarda", nomeNaLista(alunos, alunoId = 2))
        assertEquals("Pedro", nomeNaLista(alunos, alunoId = 3))
    }

    @Test fun `o aluno que nao esta na lista fica sem nome`() {
        assertEquals(null, nomeNaLista(listOf(aluno(1, "James"), aluno(2, "Maria")), alunoId = 9))
    }

    // Os avisos do celular -------------------------------------------------------------------

    @Test fun `um aviso por aluno com nao lidas, na ordem do cadastro`() {
        val alunos = listOf(aluno(2, "Maria Clara"), aluno(1, "James Willian"), aluno(3, "Pedro"))
        val naoLidas = listOf(notificacao(1, alunoId = 2), notificacao(2, alunoId = 1), notificacao(3, alunoId = 2))

        val avisos = montarAvisos(alunos, naoLidas)

        assertEquals(
            listOf(
                AvisoDoAluno(alunoId = 1, nome = "James", notificacoes = listOf(notificacao(2, alunoId = 1))),
                AvisoDoAluno(alunoId = 2, nome = "Maria", notificacoes = listOf(notificacao(1, alunoId = 2), notificacao(3, alunoId = 2))),
            ),
            avisos,
        )
    }

    @Test fun `com um aluno so, o aviso vai sem nome`() {
        val avisos = montarAvisos(listOf(aluno(1, "James Willian")), listOf(notificacao(1, alunoId = 1)))

        assertEquals(null, avisos.single().nome)
    }

    @Test fun `o horario poe na barra so os alunos com novidade nao lida`() {
        val pendentes = montarAvisos(
            listOf(aluno(1, "James"), aluno(2, "Maria"), aluno(3, "Pedro")),
            listOf(notificacao(1, alunoId = 1), notificacao(2, alunoId = 2), notificacao(3, alunoId = 3)),
        )
        val naoAvisadas = listOf(
            notificacao(1, alunoId = 1), // nova
            notificacao(4, alunoId = 2, lida = true), // nova, mas já vista no app
        ) // e o Pedro só tem uma antiga, que já foi avisada

        val novos = avisosComNovidade(pendentes, naoAvisadas)

        assertEquals(listOf(1L), novos.map { it.alunoId })
    }

    @Test fun `sem nada novo, o horario nao poe nada na barra`() {
        val pendentes = montarAvisos(listOf(aluno(1, "James")), listOf(notificacao(1, alunoId = 1)))

        assertEquals(emptyList<AvisoDoAluno>(), avisosComNovidade(pendentes, naoAvisadas = emptyList()))
    }

    // Exemplos -------------------------------------------------------------------------------

    private fun gerar(dados: AnoParaNotificar, agora: String = "2026-10-17T19:00") =
        gerarNotificacoes(dados, LocalDateTime.parse(agora), CRIADA_EM)

    private fun dados(
        vararg disciplinas: DisciplinaDados,
        regras: List<RegraAvaliacaoDomain> = listOf(regra()),
        ano: AnoLetivoDomain = ano(),
    ) = AnoParaNotificar(alunoId = 7, anoLetivo = ano, disciplinas = disciplinas.toList(), regras = regras)

    private fun ano(tipo: TipoPeriodo = TipoPeriodo.BIMESTRE) = AnoLetivoDomain(
        id = 1,
        ano = 2026,
        serie = null,
        periodo = listOf(
            periodo(id = 11, numero = 1, inicio = "2026-02-02", fim = "2026-04-30"),
            periodo(id = 12, numero = 2, inicio = "2026-05-04", fim = "2026-07-10"),
            periodo(id = 13, numero = 3, inicio = "2026-07-27", fim = "2026-10-02"),
            periodo(id = 14, numero = 4, inicio = "2026-10-05", fim = "2026-12-18"),
        ),
        tipoPeriodo = tipo,
        qtdPeriodos = 4,
        alunoId = 7,
    )

    private fun periodo(id: Long, numero: Int, inicio: String, fim: String) =
        PeriodoDomain(id = id, anoLetivoId = 1, periodo = numero, dataInicio = LocalDate.parse(inicio), dataFim = LocalDate.parse(fim))

    private fun disciplina(
        id: Long = 3,
        nome: String = "Matemática",
        avaliacoes: List<AvaliacaoDomain> = emptyList(),
        totalAulas: Int? = null,
        faltas: List<FaltaDomain> = emptyList(),
        ativa: Boolean = true,
    ) = DisciplinaDados(
        disciplinaId = id,
        nome = nome,
        abreviacao = null,
        cor = 0,
        icone = IconeMateria.CALCULADORA,
        avaliacoes = avaliacoes,
        faltas = faltas,
        totalAulas = totalAulas,
        ativa = ativa,
    )

    private fun avaliacao(
        id: Long,
        nota: Double?,
        data: String?,
        periodoId: Long = 14,
        nome: String = "Prova",
        tipo: TipoAvaliacao = TipoAvaliacao.NORMAL,
    ) = AvaliacaoDomain(
        id = id,
        disciplinaId = 3,
        periodoId = periodoId,
        nome = nome,
        nota = nota,
        notaMaxima = 10.0,
        peso = 1.0,
        tipo = tipo,
        data = data?.let(LocalDate::parse),
        criadoEm = CRIADA_EM,
    )

    private fun falta(aulas: Int) = FaltaDomain(disciplinaId = 3, periodoId = 14, data = LocalDate.of(2026, 10, 6), qtdAulas = aulas)

    private fun regra(disciplinaId: Long? = null, mediaMinima: Double = 7.0, frequenciaMinima: Double = 75.0) =
        RegraAvaliacaoDomain(
            anoLetivoId = 1,
            disciplinaId = disciplinaId,
            mediaMinima = mediaMinima,
            mediaRecuperacao = 5.0,
            frequenciaMinima = frequenciaMinima,
        )

    private fun aluno(id: Long, nome: String) = AlunoDomain(id = id, nome = nome, criadoEm = CRIADA_EM)

    private fun notificacao(id: Long, alunoId: Long, lida: Boolean = false) = NotificacaoDomain(
        id = id,
        alunoId = alunoId,
        tipo = TipoNotificacao.AVISO,
        chave = "teste:$id",
        titulo = "Notificação $id",
        texto = "",
        criadaEm = CRIADA_EM,
        lida = lida,
    )

    private fun momento(quando: String): ZonedDateTime = LocalDateTime.parse(quando).atZone(SAO_PAULO)

    private companion object {
        val CRIADA_EM: Instant = Instant.parse("2026-10-17T22:00:00Z")
        val SAO_PAULO: ZoneId = ZoneId.of("America/Sao_Paulo")
    }
}

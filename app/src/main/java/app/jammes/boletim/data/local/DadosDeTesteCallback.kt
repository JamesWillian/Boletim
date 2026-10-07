package app.jammes.boletim.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import app.jammes.boletim.data.local.entity.AvaliacaoEntity
import app.jammes.boletim.data.local.entity.FaltaEntity
import app.jammes.boletim.data.local.entity.PeriodoEntity
import app.jammes.boletim.data.mapper.Converters
import app.jammes.boletim.domain.model.Contexto
import app.jammes.boletim.domain.model.TipoAvaliacao
import java.time.LocalDate
import kotlin.time.Clock

/**
 * Dados de teste: popula o banco quando ele é criado, como o [MateriasPadraoCallback] faz com as
 * matérias. Depois de instalar o app (ou limpar os dados), ele já abre com aluno, ano letivo,
 * disciplinas, avaliações e faltas, sem precisar cadastrar nada.
 *
 * Só entra em build de debug (veja o DatabaseModule). O Contexto fica no DataStore, fora do banco,
 * então quem salva o [CONTEXTO] é o BoletimApp.
 */
class DadosDeTesteCallback : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        // Como no MateriasPadraoCallback, o banco ainda está sendo aberto: vai SQL direto.
        // Os ids são fixos para o CONTEXTO e as chaves estrangeiras apontarem para as linhas certas.
        val converters = Converters()
        val criadoEm = converters.instantToLong(CRIADO_EM)

        db.execSQL(
            "INSERT INTO aluno (id, nome, avatar, ativo, created_at) VALUES (?, ?, ?, ?, ?)",
            arrayOf<Any?>(CONTEXTO.alunoId, "James Willian", null, 1, criadoEm)
        )
        db.execSQL(
            """
            INSERT INTO ano_letivo (id, aluno_id, ano, serie, tipo_periodo, qtd_periodos, ativo)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf<Any?>(CONTEXTO.anoLetivoId, CONTEXTO.alunoId, 2024, "9º Ano", "UNIDADE", PERIODOS.size, 1)
        )
        PERIODOS.forEach {
            db.execSQL(
                "INSERT INTO periodo (id, ano_letivo_id, periodo, data_inicio, data_fim) VALUES (?, ?, ?, ?, ?)",
                arrayOf<Any?>(
                    it.id, it.anoLetivoId, it.periodo,
                    converters.localDateToInt(it.dataInicio), converters.localDateToInt(it.dataFim)
                )
            )
        }
        DISCIPLINAS.forEach {
            // Roda depois do MateriasPadraoCallback (é a ordem do addCallback), então a matéria já
            // existe. A disciplina copia nome, cor e ícone dela, como faria o cadastro.
            db.execSQL(
                """
                INSERT INTO disciplina (id, materia_id, ano_letivo_id, nome, cor, icone, total_aulas, professor, ativa, ordem)
                SELECT ?, id, ?, nome, cor, icone, ?, ?, 1, ? FROM materia WHERE abreviacao = ?
                """.trimIndent(),
                arrayOf<Any?>(it.id, CONTEXTO.anoLetivoId, it.totalAulas, it.professor, it.id, it.materia)
            )
        }
        AVALIACOES.forEach {
            db.execSQL(
                """
                INSERT INTO avaliacao (id, disciplina_id, periodo_id, nome, nota, nota_maxima, peso, tipo, data, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any?>(
                    it.id, it.disciplinaId, it.periodoId, it.nome, it.nota, it.notaMaxima, it.peso, it.tipo,
                    converters.localDateToInt(it.data), converters.instantToLong(it.criadoEm)
                )
            )
        }
        FALTAS.forEach {
            db.execSQL(
                "INSERT INTO falta (id, disciplina_id, periodo_id, data, qtd_aulas) VALUES (?, ?, ?, ?, ?)",
                arrayOf<Any?>(
                    it.id, it.disciplinaId, it.periodoId, converters.localDateToInt(it.data), it.qtdAulas
                )
            )
        }
    }

    companion object {
        /** Aluno, ano letivo e período em que o app abre com estes dados. */
        val CONTEXTO = Contexto(alunoId = 1, anoLetivoId = 1, periodoId = 1)

        // Declarado antes das listas, que o usam ao serem montadas
        private val CRIADO_EM = Clock.System.now()

        private const val PORTUGUES = 1L
        private const val MATEMATICA = 2L
        private const val HISTORIA = 3L
        private const val GEOGRAFIA = 4L
        private const val CIENCIAS = 5L

        private val PERIODOS = listOf(
            periodo(1, LocalDate.of(2024, 2, 1), LocalDate.of(2024, 4, 30)),
            periodo(2, LocalDate.of(2024, 5, 1), LocalDate.of(2024, 7, 15)),
            periodo(3, LocalDate.of(2024, 8, 1), LocalDate.of(2024, 9, 30)),
            periodo(4, LocalDate.of(2024, 10, 1), LocalDate.of(2024, 12, 15)),
        )

        private val DISCIPLINAS = listOf(
            DisciplinaDeTeste(PORTUGUES, materia = "POR", professor = "Prof. Maria", totalAulas = 80),
            DisciplinaDeTeste(MATEMATICA, materia = "MAT", professor = "Prof. José", totalAulas = 80),
            DisciplinaDeTeste(HISTORIA, materia = "HIS", professor = "Prof. Ana", totalAulas = 40),
            DisciplinaDeTeste(GEOGRAFIA, materia = "GEO", professor = "Prof. Carlos", totalAulas = 40),
            DisciplinaDeTeste(CIENCIAS, materia = "CIE", professor = "Prof. Paulo", totalAulas = 40),
        )

        private val AVALIACOES = listOf(
            avaliacao(1, PORTUGUES, "Prova 1", 8.5, LocalDate.of(2024, 3, 15)),
            avaliacao(2, PORTUGUES, "Trabalho 1", 9.0, LocalDate.of(2024, 3, 20)),
            avaliacao(3, PORTUGUES, "Simulado", 7.5, LocalDate.of(2024, 4, 10)),
            avaliacao(4, PORTUGUES, "Atividade", 10.0, LocalDate.of(2024, 4, 25)),
            avaliacao(5, MATEMATICA, "Prova 1", 6.0, LocalDate.of(2024, 3, 16)),
            avaliacao(6, MATEMATICA, "Teste 1", 7.0, LocalDate.of(2024, 3, 25)),
            avaliacao(7, MATEMATICA, "Exercícios", 8.0, LocalDate.of(2024, 4, 5)),
            avaliacao(8, MATEMATICA, "Prova Mensal", 5.5, LocalDate.of(2024, 4, 28)),
            avaliacao(9, CIENCIAS, "Laboratório", 10.0, LocalDate.of(2024, 3, 10)),
            avaliacao(10, CIENCIAS, "Relatório", 9.5, LocalDate.of(2024, 3, 28)),
            avaliacao(11, CIENCIAS, "Pesquisa", 8.5, LocalDate.of(2024, 4, 12)),
            avaliacao(12, CIENCIAS, "Prova Final P1", 7.0, LocalDate.of(2024, 4, 29)),
            // Sem nota: marcadas, mas ainda não aconteceram (ficam fora da média)
            avaliacao(13, PORTUGUES, "Prova 2", null, LocalDate.of(2024, 4, 30)),
            avaliacao(14, MATEMATICA, "Prova 2", null, LocalDate.of(2024, 5, 20), periodoId = 2),
            // Da 2ª à 4ª Unidade, para o filtro "Todos" ter o que agrupar e a média anual ter o que calcular.
            // Português: bom o ano todo
            avaliacao(15, PORTUGUES, "Prova 3", 7.5, LocalDate.of(2024, 6, 10), periodoId = 2),
            avaliacao(16, PORTUGUES, "Seminário", 9.0, LocalDate.of(2024, 6, 20), periodoId = 2),
            avaliacao(17, PORTUGUES, "Redação", 8.0, LocalDate.of(2024, 7, 5), periodoId = 2),
            avaliacao(18, PORTUGUES, "Prova 4", 8.5, LocalDate.of(2024, 8, 20), periodoId = 3),
            avaliacao(19, PORTUGUES, "Leitura", 9.5, LocalDate.of(2024, 9, 5), periodoId = 3),
            avaliacao(20, PORTUGUES, "Redação 2", 7.0, LocalDate.of(2024, 9, 25), periodoId = 3),
            avaliacao(21, PORTUGUES, "Prova 5", 8.0, LocalDate.of(2024, 10, 15), periodoId = 4),
            avaliacao(22, PORTUGUES, "Trabalho final", null, LocalDate.of(2024, 12, 5), periodoId = 4),
            // Matemática: começa abaixo da mínima e melhora
            avaliacao(23, MATEMATICA, "Teste 2", 6.5, LocalDate.of(2024, 6, 5), periodoId = 2),
            avaliacao(24, MATEMATICA, "Lista de exercícios", 7.0, LocalDate.of(2024, 6, 25), periodoId = 2),
            avaliacao(25, MATEMATICA, "Prova 3", 7.5, LocalDate.of(2024, 8, 22), periodoId = 3),
            avaliacao(26, MATEMATICA, "Teste 3", 8.0, LocalDate.of(2024, 9, 10), periodoId = 3),
            avaliacao(27, MATEMATICA, "Trabalho", 7.0, LocalDate.of(2024, 9, 26), periodoId = 3),
            avaliacao(28, MATEMATICA, "Prova 4", 8.0, LocalDate.of(2024, 10, 20), periodoId = 4),
            avaliacao(29, MATEMATICA, "Olimpíada", 9.0, LocalDate.of(2024, 11, 12), periodoId = 4),
            avaliacao(30, MATEMATICA, "Prova final", null, LocalDate.of(2024, 12, 10), periodoId = 4),
            // História: nada na 1ª Unidade (fica fora da média anual, não conta como zero) e cai na 3ª
            avaliacao(31, HISTORIA, "Prova 1", 8.0, LocalDate.of(2024, 5, 20), periodoId = 2),
            avaliacao(32, HISTORIA, "Trabalho em grupo", 7.0, LocalDate.of(2024, 6, 12), periodoId = 2),
            avaliacao(33, HISTORIA, "Seminário", 9.0, LocalDate.of(2024, 7, 2), periodoId = 2),
            avaliacao(34, HISTORIA, "Prova 2", 6.0, LocalDate.of(2024, 8, 15), periodoId = 3),
            avaliacao(35, HISTORIA, "Resumo", 5.5, LocalDate.of(2024, 9, 3), periodoId = 3),
            avaliacao(36, HISTORIA, "Debate", 7.0, LocalDate.of(2024, 9, 20), periodoId = 3),
            avaliacao(37, HISTORIA, "Prova 3", 7.5, LocalDate.of(2024, 10, 18), periodoId = 4),
            avaliacao(38, HISTORIA, "Linha do tempo", 8.0, LocalDate.of(2024, 11, 8), periodoId = 4),
            // Geografia: 6,9 na 2ª e 7,1 na 3ª, média anual exatamente na mínima (7,0)
            avaliacao(39, GEOGRAFIA, "Prova 1", 6.8, LocalDate.of(2024, 5, 22), periodoId = 2),
            avaliacao(40, GEOGRAFIA, "Mapa", 7.0, LocalDate.of(2024, 6, 18), periodoId = 2),
            avaliacao(41, GEOGRAFIA, "Prova 2", 7.2, LocalDate.of(2024, 8, 28), periodoId = 3),
            avaliacao(42, GEOGRAFIA, "Maquete", 7.0, LocalDate.of(2024, 9, 18), periodoId = 3),
            avaliacao(43, GEOGRAFIA, "Prova 3", null, LocalDate.of(2024, 11, 20), periodoId = 4),
            // Ciências: ótima, despenca na 3ª e recupera. A recuperação aparece na lista, mas fica fora da conta
            avaliacao(44, CIENCIAS, "Prova 2", 9.0, LocalDate.of(2024, 5, 28), periodoId = 2),
            avaliacao(45, CIENCIAS, "Experimento", 8.5, LocalDate.of(2024, 6, 14), periodoId = 2),
            avaliacao(46, CIENCIAS, "Feira de ciências", 10.0, LocalDate.of(2024, 7, 10), periodoId = 2),
            avaliacao(47, CIENCIAS, "Prova 3", 5.0, LocalDate.of(2024, 8, 21), periodoId = 3),
            avaliacao(48, CIENCIAS, "Relatório 2", 6.0, LocalDate.of(2024, 9, 6), periodoId = 3),
            avaliacao(49, CIENCIAS, "Questionário", 5.5, LocalDate.of(2024, 9, 19), periodoId = 3),
            avaliacao(50, CIENCIAS, "Prova 4", 7.0, LocalDate.of(2024, 10, 17), periodoId = 4),
            avaliacao(51, CIENCIAS, "Laboratório 2", 8.0, LocalDate.of(2024, 11, 6), periodoId = 4),
            avaliacao(52, CIENCIAS, "Projeto", 7.5, LocalDate.of(2024, 11, 28), periodoId = 4),
            avaliacao(53, CIENCIAS, "Recuperação", 7.0, LocalDate.of(2024, 9, 27), periodoId = 3, tipo = TipoAvaliacao.RECUPERACAO),
        )

        private val FALTAS = listOf(
            falta(1, PORTUGUES, LocalDate.of(2024, 3, 5), qtdAulas = 2),
            falta(2, PORTUGUES, LocalDate.of(2024, 4, 10)),
            falta(3, HISTORIA, LocalDate.of(2024, 3, 10)),
            falta(4, CIENCIAS, LocalDate.of(2024, 3, 15), qtdAulas = 2),
        )

        private fun periodo(numero: Int, inicio: LocalDate, fim: LocalDate) = PeriodoEntity(
            id = numero.toLong(),
            anoLetivoId = CONTEXTO.anoLetivoId,
            periodo = numero,
            dataInicio = inicio,
            dataFim = fim,
        )

        private fun avaliacao(
            id: Long,
            disciplinaId: Long,
            nome: String,
            nota: Double?,
            data: LocalDate,
            periodoId: Long = 1,
            tipo: TipoAvaliacao = TipoAvaliacao.NORMAL,
        ) = AvaliacaoEntity(
            id = id,
            disciplinaId = disciplinaId,
            periodoId = periodoId,
            nome = nome,
            nota = nota,
            notaMaxima = 10.0,
            peso = 1.0,
            tipo = TipoAvaliacao.toString(tipo),
            data = data,
            criadoEm = CRIADO_EM,
        )

        private fun falta(id: Long, disciplinaId: Long, data: LocalDate, qtdAulas: Int = 1, periodoId: Long = 1) =
            FaltaEntity(
                id = id,
                disciplinaId = disciplinaId,
                periodoId = periodoId,
                data = data,
                qtdAulas = qtdAulas,
            )
    }
}

/** A matéria vem pela abreviação: o id dela é o banco que define, no MateriasPadraoCallback. */
private class DisciplinaDeTeste(
    val id: Long,
    val materia: String,
    val professor: String,
    val totalAulas: Int,
)

package app.jammes.boletim.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import app.jammes.boletim.data.local.entity.AlunoEntity
import app.jammes.boletim.data.local.entity.AnoLetivoEntity
import app.jammes.boletim.data.local.entity.AvaliacaoEntity
import app.jammes.boletim.data.local.entity.DisciplinaEntity
import app.jammes.boletim.data.local.entity.FaltaEntity
import app.jammes.boletim.data.local.entity.MateriaEntity
import app.jammes.boletim.data.local.entity.PeriodoEntity
import app.jammes.boletim.data.local.entity.RegraAvaliacaoEntity

/**
 * O banco inteiro de uma vez, para o backup em arquivo: ler todas as tabelas, apagar tudo e gravar
 * de volta com os mesmos ids. Quem chama abre a transação, para nada ficar pela metade.
 */
@Dao
interface BackupDao {

    @Query("SELECT * FROM materia ORDER BY id")
    suspend fun materias(): List<MateriaEntity>

    @Query("SELECT * FROM aluno ORDER BY id")
    suspend fun alunos(): List<AlunoEntity>

    @Query("SELECT * FROM ano_letivo ORDER BY id")
    suspend fun anosLetivos(): List<AnoLetivoEntity>

    @Query("SELECT * FROM periodo ORDER BY id")
    suspend fun periodos(): List<PeriodoEntity>

    @Query("SELECT * FROM disciplina ORDER BY id")
    suspend fun disciplinas(): List<DisciplinaEntity>

    @Query("SELECT * FROM regra_avaliacao ORDER BY id")
    suspend fun regras(): List<RegraAvaliacaoEntity>

    @Query("SELECT * FROM avaliacao ORDER BY id")
    suspend fun avaliacoes(): List<AvaliacaoEntity>

    @Query("SELECT * FROM falta ORDER BY id")
    suspend fun faltas(): List<FaltaEntity>

    // Apagar vai dos filhos para os pais: a matéria de uma disciplina não sai antes dela (RESTRICT)

    @Query("DELETE FROM falta")
    suspend fun apagarFaltas()

    @Query("DELETE FROM avaliacao")
    suspend fun apagarAvaliacoes()

    @Query("DELETE FROM regra_avaliacao")
    suspend fun apagarRegras()

    @Query("DELETE FROM disciplina")
    suspend fun apagarDisciplinas()

    @Query("DELETE FROM periodo")
    suspend fun apagarPeriodos()

    @Query("DELETE FROM ano_letivo")
    suspend fun apagarAnosLetivos()

    @Query("DELETE FROM aluno")
    suspend fun apagarAlunos()

    @Query("DELETE FROM materia")
    suspend fun apagarMaterias()

    // Gravar vai dos pais para os filhos. Sem o IGNORE dos outros DAOs: uma linha que não entra
    // (id repetido, chave apontando para nada) derruba a transação inteira, em vez de sumir calada

    @Insert
    suspend fun inserirMaterias(materias: List<MateriaEntity>)

    @Insert
    suspend fun inserirAlunos(alunos: List<AlunoEntity>)

    @Insert
    suspend fun inserirAnosLetivos(anos: List<AnoLetivoEntity>)

    @Insert
    suspend fun inserirPeriodos(periodos: List<PeriodoEntity>)

    @Insert
    suspend fun inserirDisciplinas(disciplinas: List<DisciplinaEntity>)

    @Insert
    suspend fun inserirRegras(regras: List<RegraAvaliacaoEntity>)

    @Insert
    suspend fun inserirAvaliacoes(avaliacoes: List<AvaliacaoEntity>)

    @Insert
    suspend fun inserirFaltas(faltas: List<FaltaEntity>)
}

package app.jammes.boletim.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import app.jammes.boletim.data.local.dao.AlunoDao
import app.jammes.boletim.data.local.dao.AnoLetivoDao
import app.jammes.boletim.data.local.dao.AvaliacaoDao
import app.jammes.boletim.data.local.dao.DisciplinaDao
import app.jammes.boletim.data.local.dao.FaltaDao
import app.jammes.boletim.data.local.dao.MateriaDao
import app.jammes.boletim.data.local.dao.PeriodoDao
import app.jammes.boletim.data.local.dao.RegraAvaliacaoDao
import app.jammes.boletim.data.local.entity.AlunoEntity
import app.jammes.boletim.data.local.entity.AnoLetivoEntity
import app.jammes.boletim.data.local.entity.AvaliacaoEntity
import app.jammes.boletim.data.local.entity.DisciplinaEntity
import app.jammes.boletim.data.local.entity.FaltaEntity
import app.jammes.boletim.data.local.entity.MateriaEntity
import app.jammes.boletim.data.local.entity.PeriodoEntity
import app.jammes.boletim.data.local.entity.RegraAvaliacaoEntity
import app.jammes.boletim.data.mapper.Converters

@Database(
    entities = [
        AlunoEntity::class,
        AnoLetivoEntity::class,
        AvaliacaoEntity::class,
        DisciplinaEntity::class,
        FaltaEntity::class,
        MateriaEntity::class,
        PeriodoEntity::class,
        RegraAvaliacaoEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun alunoDao() : AlunoDao
    abstract fun anoLetivoDao() : AnoLetivoDao
    abstract fun avaliacaoDao() : AvaliacaoDao
    abstract fun disciplinaDao() : DisciplinaDao
    abstract fun faltaDao() : FaltaDao
    abstract fun materiaDao() : MateriaDao
    abstract fun periodoDao() : PeriodoDao
    abstract fun regraAvaliacaoDao() : RegraAvaliacaoDao

    companion object {

        const val NAME = "boletim.db"

    }
}
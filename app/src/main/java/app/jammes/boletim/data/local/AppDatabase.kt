package app.jammes.boletim.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import app.jammes.boletim.data.local.dao.AlunoDao
import app.jammes.boletim.data.local.dao.AnoLetivoDao
import app.jammes.boletim.data.local.dao.BoletimDao
import app.jammes.boletim.data.local.dao.DisciplinaDao
import app.jammes.boletim.data.local.dao.PeriodoDao
import app.jammes.boletim.data.local.entity.AlunoEntity
import app.jammes.boletim.data.local.entity.AnoLetivoEntity
import app.jammes.boletim.data.local.entity.BoletimEntity
import app.jammes.boletim.data.local.entity.DisciplinaEntity
import app.jammes.boletim.data.local.entity.PeriodoEntity

@Database(
    entities = [
        AlunoEntity::class,
        AnoLetivoEntity::class,
        BoletimEntity::class,
        DisciplinaEntity::class,
        PeriodoEntity::class,
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun alunoDao() : AlunoDao
    abstract fun anoLetivoDao() : AnoLetivoDao
    abstract fun boletimDao() : BoletimDao
    abstract fun disciplinaDao() : DisciplinaDao
    abstract fun periodoDao() : PeriodoDao

    companion object {

        const val NAME = "boletim.db"

    }
}
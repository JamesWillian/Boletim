package app.jammes.boletim.di

import android.content.Context
import androidx.room.Room
import app.jammes.boletim.data.local.AppDatabase
import app.jammes.boletim.data.local.dao.AlunoDao
import app.jammes.boletim.data.local.dao.AnoLetivoDao
import app.jammes.boletim.data.local.dao.AvaliacaoDao
import app.jammes.boletim.data.local.dao.DisciplinaDao
import app.jammes.boletim.data.local.dao.FaltaDao
import app.jammes.boletim.data.local.dao.MateriaDao
import app.jammes.boletim.data.local.dao.PeriodoDao
import app.jammes.boletim.data.local.dao.RegraAvaliacaoDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.NAME
        ).build()

    @Provides
    fun provideAlunoDao(db: AppDatabase): AlunoDao = db.alunoDao()

    @Provides
    fun provideAnoLetivoDao(db: AppDatabase): AnoLetivoDao = db.anoLetivoDao()

    @Provides
    fun provideAvaliacaoDao(db: AppDatabase): AvaliacaoDao = db.avaliacaoDao()

    @Provides
    fun provideDisciplinaDao(db: AppDatabase): DisciplinaDao = db.disciplinaDao()

    @Provides
    fun provideFaltaDao(db: AppDatabase): FaltaDao = db.faltaDao()

    @Provides
    fun provideMateriaDao(db: AppDatabase): MateriaDao = db.materiaDao()

    @Provides
    fun providePeriodoDao(db: AppDatabase): PeriodoDao = db.periodoDao()

    @Provides
    fun provideRegraAvaliacaoDao(db: AppDatabase): RegraAvaliacaoDao = db.regraAvaliacaoDao()
}
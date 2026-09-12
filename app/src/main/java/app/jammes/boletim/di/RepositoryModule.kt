package app.jammes.boletim.di

import app.jammes.boletim.data.local.repository.AlunoRepositoryImpl
import app.jammes.boletim.data.local.repository.AnoLetivoRepositoryImpl
import app.jammes.boletim.data.local.repository.AvaliacaoRepositoryImpl
import app.jammes.boletim.data.local.repository.DisciplinaRepositoryImpl
import app.jammes.boletim.data.local.repository.FaltaRepositoryImpl
import app.jammes.boletim.data.local.repository.MateriaRepositoryImpl
import app.jammes.boletim.data.local.repository.RegraAvaliacaoRepositoryImpl
import app.jammes.boletim.domain.repository.AlunoRepository
import app.jammes.boletim.domain.repository.AnoLetivoRepository
import app.jammes.boletim.domain.repository.AvaliacaoRepository
import app.jammes.boletim.domain.repository.DisciplinaRepository
import app.jammes.boletim.domain.repository.FaltaRepository
import app.jammes.boletim.domain.repository.MateriaRepository
import app.jammes.boletim.domain.repository.RegraAvaliacaoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAlunoRepository(impl: AlunoRepositoryImpl): AlunoRepository

    @Binds
    @Singleton
    abstract fun bindAnoLetivoRepository(impl: AnoLetivoRepositoryImpl): AnoLetivoRepository

    @Binds
    @Singleton
    abstract fun bindMateriaRepository(impl: MateriaRepositoryImpl): MateriaRepository

    @Binds
    @Singleton
    abstract fun bindDisciplinaRepository(impl: DisciplinaRepositoryImpl): DisciplinaRepository

    @Binds
    @Singleton
    abstract fun bindAvaliacaoRepository(impl: AvaliacaoRepositoryImpl): AvaliacaoRepository

    @Binds
    @Singleton
    abstract fun bindFaltaRepository(impl: FaltaRepositoryImpl): FaltaRepository

    @Binds
    @Singleton
    abstract fun bindRegraAvaliacaoRepository(impl: RegraAvaliacaoRepositoryImpl): RegraAvaliacaoRepository
}
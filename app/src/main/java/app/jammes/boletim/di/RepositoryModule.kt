package app.jammes.boletim.di

import app.jammes.boletim.data.local.repository.AlunoRepositoryImpl
import app.jammes.boletim.domain.repository.AlunoRepository
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
}
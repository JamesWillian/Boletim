package app.jammes.boletim.di

import app.jammes.boletim.domain.repository.RegistroDeUso
import app.jammes.boletim.uso.RegistroDeUsoImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** O registro do uso, atrás da interface do domínio. */
@Module
@InstallIn(SingletonComponent::class)
abstract class UsoModule {

    @Binds
    @Singleton
    abstract fun bindRegistroDeUso(impl: RegistroDeUsoImpl): RegistroDeUso
}

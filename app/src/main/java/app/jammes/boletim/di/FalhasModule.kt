package app.jammes.boletim.di

import app.jammes.boletim.domain.repository.RegistroDeFalhas
import app.jammes.boletim.falhas.RegistroDeFalhasImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** O registro das falhas, atrás da interface do domínio. */
@Module
@InstallIn(SingletonComponent::class)
abstract class FalhasModule {

    @Binds
    @Singleton
    abstract fun bindRegistroDeFalhas(impl: RegistroDeFalhasImpl): RegistroDeFalhas
}

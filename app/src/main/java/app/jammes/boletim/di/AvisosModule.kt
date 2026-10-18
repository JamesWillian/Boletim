package app.jammes.boletim.di

import app.jammes.boletim.avisos.AgendaDosAvisosImpl
import app.jammes.boletim.avisos.AvisosNoCelularImpl
import app.jammes.boletim.domain.repository.AgendaDosAvisos
import app.jammes.boletim.domain.repository.AvisosNoCelular
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** As notificações do Android e o despertador dos horários, atrás das interfaces do domínio. */
@Module
@InstallIn(SingletonComponent::class)
abstract class AvisosModule {

    @Binds
    @Singleton
    abstract fun bindAvisosNoCelular(impl: AvisosNoCelularImpl): AvisosNoCelular

    @Binds
    @Singleton
    abstract fun bindAgendaDosAvisos(impl: AgendaDosAvisosImpl): AgendaDosAvisos
}

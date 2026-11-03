package app.jammes.boletim.di

import app.jammes.boletim.data.billing.LojaGooglePlay
import app.jammes.boletim.domain.repository.LojaDoPro
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** A loja do Pro: a Google Play. */
@Module
@InstallIn(SingletonComponent::class)
abstract class LojaModule {

    @Binds
    @Singleton
    abstract fun bindLojaDoPro(impl: LojaGooglePlay): LojaDoPro
}

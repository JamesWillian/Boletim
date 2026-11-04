package app.jammes.boletim.di

import app.jammes.boletim.data.billing.LojaGooglePlay
import app.jammes.boletim.data.review.AvaliacaoNaGooglePlay
import app.jammes.boletim.domain.repository.AvaliacaoNaLoja
import app.jammes.boletim.domain.repository.LojaDoPro
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** A loja do app, que vende o Pro e recebe as avaliações: a Google Play. */
@Module
@InstallIn(SingletonComponent::class)
abstract class LojaModule {

    @Binds
    @Singleton
    abstract fun bindLojaDoPro(impl: LojaGooglePlay): LojaDoPro

    @Binds
    @Singleton
    abstract fun bindAvaliacaoNaLoja(impl: AvaliacaoNaGooglePlay): AvaliacaoNaLoja
}

package app.jammes.boletim.di

import app.jammes.boletim.BuildConfig
import app.jammes.boletim.data.repository.ChavesDoPlano
import app.jammes.boletim.data.repository.PlanoRepositoryImpl
import app.jammes.boletim.domain.repository.PlanoRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** O plano do app, com as chaves que vêm do build. */
@Module
@InstallIn(SingletonComponent::class)
abstract class PlanoModule {

    @Binds
    @Singleton
    abstract fun bindPlanoRepository(impl: PlanoRepositoryImpl): PlanoRepository

    companion object {

        @Provides
        fun provideChavesDoPlano(): ChavesDoPlano = ChavesDoPlano(
            limitesLigados = BuildConfig.LIMITES_DO_FREE,
            proDeTestePermitido = BuildConfig.DEBUG,
        )
    }
}

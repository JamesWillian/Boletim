package app.jammes.boletim.data.repository

import androidx.room.withTransaction
import app.jammes.boletim.data.local.AppDatabase
import app.jammes.boletim.domain.repository.Transacao
import jakarta.inject.Inject

class TransacaoImpl @Inject constructor(
    private val db: AppDatabase
): Transacao {

    // As funções suspend dos DAOs chamadas dentro do bloco entram na mesma transação do Room
    override suspend fun <T> invoke(bloco: suspend () -> T): T = db.withTransaction(bloco)
}

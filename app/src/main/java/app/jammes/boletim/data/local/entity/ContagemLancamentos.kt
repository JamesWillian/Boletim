package app.jammes.boletim.data.local.entity

import androidx.room.ColumnInfo

/** Uma linha das consultas que contam o que foi lançado em cada período ou disciplina do ano. */
data class ContagemLancamentos(
    @ColumnInfo(name = "id") val id: Long, // do período ou da disciplina, conforme a consulta
    @ColumnInfo(name = "avaliacoes") val avaliacoes: Int,
    @ColumnInfo(name = "faltas") val faltas: Int // soma das aulas, como o Boletim conta
)

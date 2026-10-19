package app.jammes.boletim.data.local.entity

import androidx.room.ColumnInfo

/** A linha da consulta que conta o que é de um aluno: o que sai junto quando ele é excluído. */
data class ContagemDoAluno(
    @ColumnInfo(name = "anos_letivos") val anosLetivos: Int,
    @ColumnInfo(name = "disciplinas") val disciplinas: Int,
    @ColumnInfo(name = "avaliacoes") val avaliacoes: Int,
    @ColumnInfo(name = "faltas") val faltas: Int // soma das aulas, como o Boletim conta
)

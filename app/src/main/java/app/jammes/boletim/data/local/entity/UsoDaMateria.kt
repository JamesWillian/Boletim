package app.jammes.boletim.data.local.entity

import androidx.room.ColumnInfo

/** Uma linha da consulta que conta em quantos anos letivos cada matéria está. */
data class UsoDaMateria(
    @ColumnInfo(name = "materia_id") val materiaId: Long,
    @ColumnInfo(name = "anos") val anos: Int
)

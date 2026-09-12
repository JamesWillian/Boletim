package app.jammes.boletim.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "materia")
data class MateriaEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0L,
    @ColumnInfo(name = "nome") val nome: String,
    @ColumnInfo(name = "abreviacao") val abreviacao: String?,
    @ColumnInfo(name = "cor") val cor: Int = 0
)

package app.jammes.boletim.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import app.jammes.boletim.data.local.entity.MateriaEntity

/**
 * Insere as matérias padrão quando o banco é criado, ou seja, na primeira vez que o app é
 * aberto depois de instalado (ou de ter os dados limpos). Nesse momento a tabela de matérias
 * está sempre vazia. O onCreate não roda de novo, então apagar matérias depois não as traz de volta.
 */
class MateriasPadraoCallback : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        // O banco ainda está sendo aberto, então os DAOs não podem ser usados aqui: vai SQL direto.
        MATERIAS.forEach { materia ->
            db.execSQL(
                "INSERT INTO materia (nome, abreviacao, cor) VALUES (?, ?, ?)",
                arrayOf<Any?>(materia.nome, materia.abreviacao, materia.cor)
            )
        }
    }

    companion object {
        // cor = índice em CoresDisciplina.paleta
        val MATERIAS = listOf(
            MateriaEntity(nome = "Português", abreviacao = "POR", cor = 0),
            MateriaEntity(nome = "Matemática", abreviacao = "MAT", cor = 1),
            MateriaEntity(nome = "História", abreviacao = "HIS", cor = 16),
            MateriaEntity(nome = "Geografia", abreviacao = "GEO", cor = 10),
            MateriaEntity(nome = "Ciências", abreviacao = "CIE", cor = 2),
            MateriaEntity(nome = "Inglês", abreviacao = "ING", cor = 8),
            MateriaEntity(nome = "Arte", abreviacao = "ART", cor = 6),
            MateriaEntity(nome = "Educação Física", abreviacao = "EDF", cor = 3),
            MateriaEntity(nome = "Física", abreviacao = "FIS", cor = 5),
            MateriaEntity(nome = "Química", abreviacao = "QUI", cor = 4),
            MateriaEntity(nome = "Biologia", abreviacao = "BIO", cor = 11),
            MateriaEntity(nome = "Filosofia", abreviacao = "FIL", cor = 17),
            MateriaEntity(nome = "Sociologia", abreviacao = "SOC", cor = 14)
        )
    }
}

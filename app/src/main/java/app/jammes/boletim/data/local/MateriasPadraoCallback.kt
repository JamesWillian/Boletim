package app.jammes.boletim.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import app.jammes.boletim.data.local.entity.MateriaEntity
import app.jammes.boletim.domain.model.IconeMateria

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
                "INSERT INTO materia (nome, abreviacao, cor, icone) VALUES (?, ?, ?, ?)",
                arrayOf<Any?>(materia.nome, materia.abreviacao, materia.cor, materia.icone)
            )
        }
    }

    companion object {
        // cor = índice em CoresDisciplina.paleta
        val MATERIAS = listOf(
            materia("Português", "POR", cor = 0, IconeMateria.LIVRO),
            materia("Matemática", "MAT", cor = 1, IconeMateria.CALCULADORA),
            materia("História", "HIS", cor = 16, IconeMateria.PERGAMINHO),
            materia("Geografia", "GEO", cor = 10, IconeMateria.GLOBO),
            materia("Ciências", "CIE", cor = 2, IconeMateria.MICROSCOPIO),
            materia("Inglês", "ING", cor = 8, IconeMateria.IDIOMA),
            materia("Arte", "ART", cor = 6, IconeMateria.PALETA),
            materia("Educação Física", "EDF", cor = 3, IconeMateria.BOLA),
            materia("Física", "FIS", cor = 5, IconeMateria.RAIO),
            materia("Química", "QUI", cor = 4, IconeMateria.FRASCO),
            materia("Biologia", "BIO", cor = 11, IconeMateria.FOLHA),
            materia("Filosofia", "FIL", cor = 17, IconeMateria.MENTE),
            materia("Sociologia", "SOC", cor = 14, IconeMateria.PESSOAS)
        )

        private fun materia(nome: String, abreviacao: String, cor: Int, icone: IconeMateria) =
            MateriaEntity(nome = nome, abreviacao = abreviacao, cor = cor, icone = IconeMateria.toString(icone))
    }
}

package app.jammes.boletim.domain.usecase

import app.jammes.boletim.domain.model.IconeMateria
import app.jammes.boletim.domain.model.LimiteDoGratisException
import app.jammes.boletim.domain.model.MateriaDoCadastro
import app.jammes.boletim.domain.model.MateriaDomain
import app.jammes.boletim.domain.model.podeGravarMateriasNovas
import app.jammes.boletim.domain.repository.MateriaRepository
import app.jammes.boletim.domain.repository.PlanoRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.text.Normalizer

// ---------------------------------------------------------------------------
// Regras puras
// ---------------------------------------------------------------------------

// O ícone de uma matéria nova, pelo começo de uma palavra do nome (sem acentos: "Música" e
// "Musical" dão no mesmo). Sem nenhuma, fica o padrão
private val ICONES_POR_PALAVRA = listOf(
    "music" to IconeMateria.NOTA_MUSICAL,
    "robot" to IconeMateria.ROBO,
    "program" to IconeMateria.CODIGO,
    "informat" to IconeMateria.COMPUTADOR,
    "tecnolog" to IconeMateria.COMPUTADOR,
    "redac" to IconeMateria.REDACAO,
    "literat" to IconeMateria.REDACAO,
    "financ" to IconeMateria.DINHEIRO,
    "econom" to IconeMateria.DINHEIRO,
    "teatro" to IconeMateria.TEATRO,
    "desenho" to IconeMateria.REGUA,
    "espanhol" to IconeMateria.IDIOMA,
    "frances" to IconeMateria.IDIOMA,
)

/** O ícone que o nome sugere: "Educação Musical" ganha a nota musical, "Robótica" o robô. */
internal fun iconeSugerido(nome: String): IconeMateria {
    val palavras = nome.semAcentos().split(' ', '-')
    return ICONES_POR_PALAVRA.firstOrNull { (comeco, _) -> palavras.any { it.startsWith(comeco) } }?.second
        ?: IconeMateria.PADRAO
}

/** Em minúsculas e sem acentos, para comparar: "Matemática" e "matematica" dão no mesmo. */
internal fun String.semAcentos(): String =
    Normalizer.normalize(lowercase(), Normalizer.Form.NFD).replace(MARCAS_DE_ACENTO, "")

private val MARCAS_DE_ACENTO = Regex("\\p{M}")

// ---------------------------------------------------------------------------
// Casos de uso
// ---------------------------------------------------------------------------

/** A matéria é do app (não muda nem sai, em nenhum plano) ou está em uso num ano letivo (não sai): nada foi gravado. */
class MateriaProtegidaException(mensagem: String) : Exception(mensagem)

class ObservarMateriasDoCadastro @Inject constructor(
    private val materiaRepository: MateriaRepository,
) {
    /** Cada matéria do cadastro, na ordem dele, e em quantos anos letivos ela está. */
    operator fun invoke(): Flow<List<MateriaDoCadastro>> =
        combine(materiaRepository.observeMaterias(), materiaRepository.observeAnosEmUso()) { materias, anos ->
            materias.map { MateriaDoCadastro(it, anosEmUso = anos[it.id] ?: 0) }
        }
}

class SalvarMateria @Inject constructor(
    private val materiaRepository: MateriaRepository,
    private val planoRepository: PlanoRepository,
) {
    /**
     * Grava uma matéria do cadastro: id 0 é nova. As disciplinas têm cópia própria do nome, da sigla, da
     * cor e do ícone, então editar a matéria só vale para as que nascerem daqui em diante.
     *
     * Se o plano não deixa criar mais uma ([podeGravarMateriasNovas]), sai [LimiteDoGratisException] sem
     * gravar nada (a tela manda para a folha do Pro antes). Uma do app não muda em nenhum plano: sai
     * [MateriaProtegidaException] (a tela não tem como mudar, é só para garantir).
     */
    suspend operator fun invoke(materia: MateriaDomain) {
        val cadastro = materiaRepository.observeMaterias().first()
        if (materia.id == 0L) {
            if (!planoRepository.plano.first().podeGravarMateriasNovas(cadastro, novas = 1)) {
                throw LimiteDoGratisException.deMaterias()
            }
        } else {
            // Vale a origem que está no banco, e não a que veio da tela
            val gravada = cadastro.firstOrNull { it.id == materia.id } ?: materia
            if (!gravada.propria) throw MateriaProtegidaException("As matérias do app não podem ser alteradas")
        }
        materiaRepository.upsert(materia)
    }
}

class ExcluirMateria @Inject constructor(
    private val materiaRepository: MateriaRepository,
) {
    /**
     * Tira a matéria do cadastro. Se ela for do app ou estiver em uso ([MateriaDoCadastro.impedimentoDeExcluir]),
     * nada é apagado e sai [MateriaProtegidaException]. A tela não deixa chegar aqui assim; a conta é
     * refeita no banco só para garantir.
     */
    suspend operator fun invoke(materia: MateriaDomain) {
        val anos = materiaRepository.observeAnosEmUso().first()[materia.id] ?: 0
        if (MateriaDoCadastro(materia, anos).impedimentoDeExcluir != null) {
            throw MateriaProtegidaException("Esta matéria não pode ser excluída")
        }
        materiaRepository.delete(materia)
    }
}

package app.jammes.boletim.presentation.ui.materia

import app.jammes.boletim.domain.model.FonteMateria
import app.jammes.boletim.domain.model.IconeMateria
import app.jammes.boletim.domain.model.MateriaDomain
import app.jammes.boletim.domain.usecase.iconeSugerido
import app.jammes.boletim.presentation.ui.components.nomeRepetido
import app.jammes.boletim.presentation.ui.components.siglaDaDisciplina
import app.jammes.boletim.presentation.ui.theme.CoresDisciplina
import java.io.Serializable

/**
 * Uma matéria nova, para o sheet abrir: criada pela pessoa e com a primeira cor que as [coresUsadas] deixam
 * livre, para não repetir a de outra aba. O ícone fica no padrão até o nome sugerir outro.
 */
internal fun materiaNova(coresUsadas: Collection<Int>) = MateriaDomain(
    cor = CoresDisciplina.primeiraLivre(coresUsadas),
    fonte = FonteMateria.USUARIO,
)

/**
 * Os campos do sheet da matéria, do jeito que estão na tela. É Serializable para o rememberSaveable
 * guardar: o que já foi digitado sobrevive a girar a tela.
 */
internal data class FormularioMateria(
    val nome: String,
    val abreviacao: String,
    val cor: Int,
    val icone: IconeMateria,
    val iconeEscolhido: Boolean, // false: o ícone acompanha o nome, até a pessoa escolher um
) : Serializable {

    fun comNome(novo: String) = copy(nome = novo, icone = if (iconeEscolhido) icone else iconeSugerido(novo))

    fun comIcone(novo: IconeMateria) = copy(icone = novo, iconeEscolhido = true)

    /**
     * O que aparece embaixo do nome; null quando ele está certo. Vazio só é erro numa matéria que já tinha
     * nome ([original]): ao abrir uma nova, ainda não é.
     */
    fun erroDoNome(original: MateriaDomain, existentes: List<String>): String? = when {
        nome.isBlank() -> if (original.id != 0L) "Dê um nome à matéria" else null
        nomeRepetido(nome, existentes) -> "Essa matéria já está na lista"
        else -> null
    }

    /** Uma nova só pede o nome certo; uma que já existe, também alguma mudança. */
    fun podeSalvar(original: MateriaDomain, existentes: List<String>): Boolean =
        nome.isNotBlank() &&
            !nomeRepetido(nome, existentes) &&
            (original.id == 0L || aplicadaEm(original) != de(original).aplicadaEm(original))

    /** A [original] com o que está na tela, como vai para o banco. Sem sigla digitada, ela sai do nome. */
    fun aplicadaEm(original: MateriaDomain): MateriaDomain {
        val nomeLimpo = nome.trim()
        return original.copy(
            nome = nomeLimpo,
            abreviacao = siglaDaDisciplina(abreviacao, nomeLimpo),
            cor = cor,
            icone = icone,
        )
    }

    companion object {
        /** O formulário com a [materia] como está. O ícone de uma que já existe não muda sozinho. */
        fun de(materia: MateriaDomain) = FormularioMateria(
            nome = materia.nome,
            abreviacao = materia.abreviacao.orEmpty(),
            cor = materia.cor,
            icone = materia.icone,
            iconeEscolhido = materia.id != 0L,
        )
    }
}

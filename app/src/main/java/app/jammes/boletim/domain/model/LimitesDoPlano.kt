package app.jammes.boletim.domain.model

import java.time.LocalDate
import java.time.Month

/*
 * O que cada plano deixa criar e mudar. Os limites do grátis valem na hora de criar: o que já existe
 * continua aparecendo e editável, mesmo além deles (um backup importado, um Pro que deixou de valer).
 */

/** Um aluno: o segundo é do Pro. */
const val ALUNOS_NO_GRATIS = 1

/** Além das matérias do app, três criadas pela pessoa: as da escola que o catálogo não tem. */
const val MATERIAS_PROPRIAS_NO_GRATIS = 3

/** Se dá para criar mais um aluno, com [alunos] já cadastrados. */
fun Plano.podeCriarAluno(alunos: Int): Boolean =
    this == Plano.PRO || alunos < ALUNOS_NO_GRATIS

/**
 * Se dá para criar mais uma matéria, com [proprias] já criadas pela pessoa: todas as do cadastro, em
 * uso num ano ou não, e também as que estão no formulário e só vão para o banco no Salvar.
 */
fun Plano.podeCriarMateria(proprias: Int): Boolean =
    this == Plano.PRO || proprias < MATERIAS_PROPRIAS_NO_GRATIS

/** O que acontece ao escolher um ano para um ano letivo novo. */
enum class CriacaoDoAno {
    LIBERADA,
    REPETIDA, // o aluno já tem esse ano: o Pro cria, com um aviso
    JA_EXISTE, // o aluno já tem esse ano, e o grátis não repete
    SO_NO_PRO; // um ano que o grátis ainda não libera

    /** Se dá para criar, com ou sem aviso. */
    val permitida: Boolean get() = this == LIBERADA || this == REPETIDA
}

/**
 * Como fica a criação de um ano letivo em [ano] para um aluno que já tem os [existentes], em [hoje].
 *
 * O grátis não repete ano e cria até o atual. O seguinte ele libera em dezembro, para quem prepara o
 * próximo, e no primeiro ano letivo do aluno, para o onboarding não esbarrar no Pro. O Pro cria
 * qualquer ano e repete com aviso: escola e curso técnico no mesmo ano, por exemplo.
 */
fun Plano.criacaoDoAno(ano: Int, existentes: Set<Int>, hoje: LocalDate): CriacaoDoAno = when (this) {
    Plano.PRO -> if (ano in existentes) CriacaoDoAno.REPETIDA else CriacaoDoAno.LIBERADA
    Plano.GRATIS -> when {
        ano in existentes -> CriacaoDoAno.JA_EXISTE
        ano <= ultimoAnoNoGratis(existentes, hoje) -> CriacaoDoAno.LIBERADA
        else -> CriacaoDoAno.SO_NO_PRO
    }
}

/** O ano atual, ou o seguinte em dezembro e no primeiro ano letivo do aluno. */
private fun ultimoAnoNoGratis(existentes: Set<Int>, hoje: LocalDate): Int =
    if (hoje.month == Month.DECEMBER || existentes.isEmpty()) hoje.year + 1 else hoje.year

/**
 * Se dá para mudar o nome, a sigla, a cor e o ícone de uma disciplina da matéria vinda de [fonte], e
 * voltar aos da matéria com o Redefinir. O grátis só personaliza as matérias que a pessoa criou. O
 * resto da edição (professor, aulas, período, regras) vale em qualquer plano.
 */
fun Plano.podePersonalizarDisciplina(fonte: FonteMateria): Boolean =
    this == Plano.PRO || fonte == FonteMateria.USUARIO

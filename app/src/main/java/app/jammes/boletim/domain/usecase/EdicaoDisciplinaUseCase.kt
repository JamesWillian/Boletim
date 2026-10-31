package app.jammes.boletim.domain.usecase

import app.jammes.boletim.domain.model.DisciplinaDomain
import app.jammes.boletim.domain.model.EdicaoDisciplina
import app.jammes.boletim.domain.model.LimiteDoGratisException
import app.jammes.boletim.domain.model.RegraAvaliacaoDomain
import app.jammes.boletim.domain.model.podePersonalizarDisciplina
import app.jammes.boletim.domain.repository.AnoLetivoRepository
import app.jammes.boletim.domain.repository.DisciplinaRepository
import app.jammes.boletim.domain.repository.MateriaRepository
import app.jammes.boletim.domain.repository.PlanoRepository
import app.jammes.boletim.domain.repository.RegraAvaliacaoRepository
import app.jammes.boletim.domain.repository.Transacao
import jakarta.inject.Inject
import kotlinx.coroutines.flow.first

// ---------------------------------------------------------------------------
// Regras puras
// ---------------------------------------------------------------------------

/**
 * Se gravar [depois] muda o nome, a sigla, a cor ou o ícone da disciplina, comparando com o que está
 * salvo nela ([antes]) e não com a matéria: uma personalização de antes continua como está, e o resto
 * da edição grava sem esbarrar no plano. Espaços nas pontas e sigla vazia não contam como mudança.
 */
internal fun mudaPersonalizacao(antes: DisciplinaDomain, depois: DisciplinaDomain): Boolean =
    antes.nome.trim() != depois.nome.trim() ||
        antes.abreviacao.orEmpty().trim() != depois.abreviacao.orEmpty().trim() ||
        antes.cor != depois.cor ||
        antes.icone != depois.icone

// ---------------------------------------------------------------------------
// Casos de uso
// ---------------------------------------------------------------------------

class ObterEdicaoDisciplina @Inject constructor(
    private val disciplinaRepository: DisciplinaRepository,
    private val materiaRepository: MateriaRepository,
    private val anoLetivoRepository: AnoLetivoRepository,
    private val regraRepository: RegraAvaliacaoRepository,
) {
    /**
     * Uma leitura só, sem Flow, como nos ajustes do ano: a tela edita uma cópia e grava tudo de uma
     * vez no Salvar. Devolve null quando a disciplina não existe mais no banco.
     */
    suspend operator fun invoke(disciplinaId: Long): EdicaoDisciplina? {
        val disciplina = disciplinaRepository.observeById(disciplinaId).first() ?: return null
        val regras = regraRepository.observeByAnoLetivo(disciplina.anoLetivoId).first()

        return EdicaoDisciplina(
            disciplina = disciplina,
            materia = materiaRepository.observeMaterias().first().find { it.id == disciplina.materiaId },
            anoLetivo = anoLetivoRepository.observeById(disciplina.anoLetivoId).first(),
            // Sem a padrão gravada, a que o cálculo usa no lugar dela (e que é do ano 1), como nos ajustes
            regraDoAno = regras.padraoDoAno().copy(anoLetivoId = disciplina.anoLetivoId),
            // A mesma que o paraDisciplina do Boletim e do detalhe encontra
            regraPropria = regras.firstOrNull { it.disciplinaId == disciplina.id },
        )
    }
}

class SalvarEdicaoDisciplina @Inject constructor(
    private val transacao: Transacao,
    private val disciplinaRepository: DisciplinaRepository,
    private val regraRepository: RegraAvaliacaoRepository,
    private val planoRepository: PlanoRepository,
) {
    /**
     * Grava a disciplina e a regra própria dela de uma vez, comparando com o [original] que a tela leu.
     * [regra] null: a disciplina volta a seguir a regra do ano, e a própria, se havia, é apagada.
     *
     * Se o plano não personaliza esta disciplina ([podePersonalizarDisciplina]) e a gravação muda o
     * nome, a sigla, a cor ou o ícone ([mudaPersonalizacao]), sai [LimiteDoGratisException] sem gravar
     * nada (a tela já não deixa mudar esses campos).
     */
    suspend operator fun invoke(original: EdicaoDisciplina, disciplina: DisciplinaDomain, regra: RegraAvaliacaoDomain?) {
        if (mudaPersonalizacao(original.disciplina, disciplina) &&
            !planoRepository.plano.first().podePersonalizarDisciplina(original.fonteDaMateria)
        ) {
            throw LimiteDoGratisException.dePersonalizacao()
        }
        transacao {
            disciplinaRepository.upsert(disciplina)
            val propria = original.regraPropria
            when {
                regra != null -> regraRepository.upsert(regra)
                propria != null -> regraRepository.delete(propria)
            }
        }
    }
}

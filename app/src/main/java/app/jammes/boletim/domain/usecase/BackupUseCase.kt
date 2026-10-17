package app.jammes.boletim.domain.usecase

import app.jammes.boletim.domain.model.AlunoDomain
import app.jammes.boletim.domain.model.AnoLetivoDomain
import app.jammes.boletim.domain.model.BackupLido
import app.jammes.boletim.domain.model.Contexto
import app.jammes.boletim.domain.model.periodoAoAbrir
import app.jammes.boletim.domain.repository.AlunoRepository
import app.jammes.boletim.domain.repository.AnoLetivoRepository
import app.jammes.boletim.domain.repository.BackupRepository
import app.jammes.boletim.domain.repository.ContextoRepository
import jakarta.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate

// ---------------------------------------------------------------------------
// Regras puras
// ---------------------------------------------------------------------------

/**
 * O aluno, o ano e o período que o app abre depois de importar um backup. O que estava [aberto]
 * continua, se o arquivo também tem o aluno e o ano (um backup deste mesmo aparelho): o período, ou
 * o ano inteiro, fica como estava. Senão, abre o primeiro aluno do cadastro que tem ano letivo, no
 * mais recente e no período de [hoje], como ao escolher um ano. null quando nenhum aluno tem ano letivo.
 */
internal fun contextoDepoisDeImportar(
    aberto: Contexto?,
    alunos: List<AlunoDomain>,
    anos: List<AnoLetivoDomain>, // de todos os alunos, com os períodos
    hoje: LocalDate,
): Contexto? {
    val mesmoAno = aberto?.let { contexto -> anos.find { it.id == contexto.anoLetivoId && it.alunoId == contexto.alunoId } }
    if (aberto != null && mesmoAno != null) {
        val periodo = when (val periodoAberto = aberto.periodoId) {
            null -> null // o ano inteiro
            in mesmoAno.periodo.map { it.id } -> periodoAberto
            else -> mesmoAno.periodo.periodoAoAbrir(hoje) // o período não está mais no ano
        }
        return Contexto(mesmoAno.alunoId, mesmoAno.id, periodo)
    }

    val ano = alunos.sortedBy { it.id }.firstNotNullOfOrNull { aluno ->
        anos.filter { it.alunoId == aluno.id }.sortedWith(MAIS_RECENTE_PRIMEIRO).firstOrNull()
    } ?: return null
    return Contexto(ano.alunoId, ano.id, ano.periodo.periodoAoAbrir(hoje))
}

// ---------------------------------------------------------------------------
// Casos de uso
// ---------------------------------------------------------------------------

class ImportarBackup @Inject constructor(
    private val backupRepository: BackupRepository,
    private val alunoRepository: AlunoRepository,
    private val anoLetivoRepository: AnoLetivoRepository,
    private val contextoRepository: ContextoRepository,
) {
    /**
     * Troca todos os dados do aparelho pelos do [backup] e abre o app neles, pelo
     * [contextoDepoisDeImportar]. Não para no meio se a tela fechar: com os dados já trocados, o
     * contexto salvo também precisa mudar, ou ele apontaria para um aluno que não existe mais.
     */
    suspend operator fun invoke(backup: BackupLido, hoje: LocalDate = LocalDate.now()) {
        withContext(NonCancellable) {
            val aberto = contextoRepository.contexto.first()
            backupRepository.substituirTudo(backup)
            val novo = contextoDepoisDeImportar(
                aberto = aberto,
                alunos = alunoRepository.observeAluno().first(),
                anos = anoLetivoRepository.observeAll().first(),
                hoje = hoje,
            ) ?: return@withContext // não acontece: o arquivo sem aluno com ano letivo é recusado ao ser lido
            contextoRepository.selecionarAluno(novo.alunoId, novo.anoLetivoId, novo.periodoId)
        }
    }
}

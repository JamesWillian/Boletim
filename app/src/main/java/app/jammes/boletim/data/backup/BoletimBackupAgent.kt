package app.jammes.boletim.data.backup

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.app.backup.FullBackupDataOutput
import android.os.Build
import android.os.ParcelFileDescriptor
import app.jammes.boletim.data.local.preferences.ConfiguracoesPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * O Auto Backup do Android, com a chave "Backup automático no Google" das configurações. Ligada, o
 * backup é o de sempre: os arquivos do app, pelas regras de backup_rules e data_extraction_rules.
 * Desligada, o que vai para a nuvem sai vazio, e no próximo envio ele toma o lugar da cópia que
 * estava lá (o Google guarda só a mais recente). A cópia direto de um aparelho para outro, por cabo
 * ou Wi-Fi ao configurar o aparelho novo, continua: ela não passa pela conta Google.
 *
 * O sistema roda o backup com o app fechado, num modo em que a BoletimApp não é criada: sem ela não
 * há Hilt, e a preferência é lida direto.
 */
class BoletimBackupAgent : BackupAgent() {

    override fun onFullBackup(data: FullBackupDataOutput) {
        if (entraNoBackup(backupAutomatico = backupAutomaticoLigado(), deAparelhoParaAparelho = deAparelhoParaAparelho(data))) {
            super.onFullBackup(data)
        }
    }

    private fun backupAutomaticoLigado(): Boolean = runBlocking {
        ConfiguracoesPreferences(applicationContext).configuracoes.first().backupAutomatico
    }

    // Antes do Android 9 não dá para saber: conta como nuvem, que é o que a chave desligada recusa
    private fun deAparelhoParaAparelho(data: FullBackupDataOutput): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            (data.transportFlags and BackupAgent.FLAG_DEVICE_TO_DEVICE_TRANSFER) != 0

    // O app usa só o Auto Backup (fullBackupOnly no manifesto): o backup por chave e valor não tem nada
    override fun onBackup(oldState: ParcelFileDescriptor?, data: BackupDataOutput?, newState: ParcelFileDescriptor?) = Unit

    override fun onRestore(data: BackupDataInput?, appVersionCode: Int, newState: ParcelFileDescriptor?) = Unit
}

/** Se os arquivos do app vão neste backup: sempre na cópia entre aparelhos, e na nuvem só com a chave ligada. */
internal fun entraNoBackup(backupAutomatico: Boolean, deAparelhoParaAparelho: Boolean): Boolean =
    backupAutomatico || deAparelhoParaAparelho

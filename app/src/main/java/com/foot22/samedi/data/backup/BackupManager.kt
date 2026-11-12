package com.foot22.samedi.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.foot22.samedi.data.local.AppDatabase
import com.foot22.samedi.data.local.AppSettings
import com.foot22.samedi.data.local.CashTransactionEntity
import com.foot22.samedi.data.local.MatchEntity
import com.foot22.samedi.data.local.MatchParticipationEntity
import com.foot22.samedi.data.local.PlayerEntity
import com.foot22.samedi.data.local.SettingsRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class FullBackupPayload(
    val schemaVersion: Int = 1,
    val exportedAtEpoch: Long = System.currentTimeMillis(),
    val settings: AppSettings,
    val players: List<PlayerEntity>,
    val matches: List<MatchEntity>,
    val participations: List<MatchParticipationEntity>,
    val transactions: List<CashTransactionEntity>
)

class BackupManager(
    private val context: Context,
    private val db: AppDatabase,
    private val settingsRepository: SettingsRepository
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun exportToUri(uri: Uri): Result<Int> = runCatching {
        val payload = FullBackupPayload(
            settings = settingsRepository.getSettingsOnce(),
            players = db.playerDao().getAllPlayersOnce(),
            matches = db.matchDao().getAllMatchesOnce(),
            participations = db.participationDao().getAllParticipationsOnce(),
            transactions = db.cashTransactionDao().getAllTransactionsOnce()
        )
        val raw = json.encodeToString(payload)
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(raw.toByteArray(Charsets.UTF_8))
        } ?: error("Impossible d'ouvrir le fichier de destination")
        payload.players.size + payload.matches.size
    }

    suspend fun importFromUri(uri: Uri): Result<FullBackupPayload> = runCatching {
        val raw = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader(Charsets.UTF_8).readText()
        } ?: error("Impossible de lire le fichier sélectionné")
        val payload = json.decodeFromString<FullBackupPayload>(raw)
        db.withTransaction {
            db.participationDao().deleteAllParticipations()
            db.cashTransactionDao().deleteAllTransactions()
            db.matchDao().deleteAllMatches()
            db.playerDao().deleteAllPlayers()

            db.playerDao().insertPlayers(payload.players)
            db.matchDao().insertMatches(payload.matches)
            db.participationDao().upsertAll(payload.participations)
            db.cashTransactionDao().insertTransactions(payload.transactions)
        }
        settingsRepository.replaceSettings(payload.settings)
        payload
    }
}

package com.foot22.samedi.data.repository

import androidx.room.withTransaction
import com.foot22.samedi.data.local.AppDatabase
import com.foot22.samedi.data.local.AppSettings
import com.foot22.samedi.data.local.CashTransactionEntity
import com.foot22.samedi.data.local.MatchEntity
import com.foot22.samedi.data.local.MatchParticipationEntity
import com.foot22.samedi.data.local.PlayerEntity
import com.foot22.samedi.data.local.SettingsRepository
import com.foot22.samedi.data.model.AttendanceStatus
import com.foot22.samedi.data.model.FeeMode
import com.foot22.samedi.data.model.MatchStatus
import com.foot22.samedi.data.model.PaymentMethod
import com.foot22.samedi.data.model.PlayerPosition
import com.foot22.samedi.data.model.PlayerStatus
import com.foot22.samedi.data.model.TeamSide
import com.foot22.samedi.data.model.TransactionType
import com.foot22.samedi.domain.BalancedTeamsResult
import com.foot22.samedi.domain.FeeCalculator
import com.foot22.samedi.domain.SaturdayScheduler
import com.foot22.samedi.domain.TeamBalancer
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek

class FootRepository(
    private val db: AppDatabase,
    private val settingsRepository: SettingsRepository
) {
    private val playerDao = db.playerDao()
    private val matchDao = db.matchDao()
    private val participationDao = db.participationDao()
    private val cashDao = db.cashTransactionDao()

    val settingsFlow: Flow<AppSettings> = settingsRepository.settingsFlow
    val playersFlow: Flow<List<PlayerEntity>> = playerDao.observeAllPlayers()
    val matchesFlow: Flow<List<MatchEntity>> = matchDao.observeAllMatches()
    val upcomingMatchFlow: Flow<MatchEntity?> = matchDao.observeUpcomingMatch()
    val participationsFlow: Flow<List<MatchParticipationEntity>> = participationDao.observeAllParticipations()
    val transactionsFlow: Flow<List<CashTransactionEntity>> = cashDao.observeAllTransactions()

    suspend fun ensureUpcomingMatchExists(): MatchEntity {
        val existing = matchDao.getUpcomingMatchOnce()
        if (existing != null) return existing
        val settings = settingsRepository.getSettingsOnce()
        val day = DayOfWeek.of(settings.matchDayOfWeek.coerceIn(1, 7))
        val nextDt = SaturdayScheduler.nextMatchDateTime(
            dayOfWeek = day,
            hour = settings.matchHour,
            minute = settings.matchMinute
        )
        val created = MatchEntity(
            dateTimeIso = SaturdayScheduler.toIso(nextDt),
            location = settings.defaultLocation,
            status = MatchStatus.OPEN,
            maxPlayers = settings.defaultMaxPlayers,
            pitchCost = settings.defaultPitchCost,
            feeMode = settings.defaultFeeMode,
            fixedFeePerPlayer = settings.defaultFixedFee
        )
        val id = matchDao.insertMatch(created)
        return created.copy(id = id)
    }

    suspend fun createNextMatchAfter(referenceIso: String?): MatchEntity {
        val settings = settingsRepository.getSettingsOnce()
        val day = DayOfWeek.of(settings.matchDayOfWeek.coerceIn(1, 7))
        val nextDt = if (referenceIso != null) {
            SaturdayScheduler.nextWeekFrom(referenceIso, day, settings.matchHour, settings.matchMinute)
        } else {
            SaturdayScheduler.nextMatchDateTime(dayOfWeek = day, hour = settings.matchHour, minute = settings.matchMinute)
        }
        val match = MatchEntity(
            dateTimeIso = SaturdayScheduler.toIso(nextDt),
            location = settings.defaultLocation,
            status = MatchStatus.OPEN,
            maxPlayers = settings.defaultMaxPlayers,
            pitchCost = settings.defaultPitchCost,
            feeMode = settings.defaultFeeMode,
            fixedFeePerPlayer = settings.defaultFixedFee
        )
        val id = matchDao.insertMatch(match)
        return match.copy(id = id)
    }

    suspend fun updateMatchDetails(match: MatchEntity) {
        matchDao.updateMatch(match)
        recalculateMatchFees(match.id)
    }

    suspend fun cancelMatchAndScheduleNext(match: MatchEntity) {
        matchDao.updateMatch(match.copy(status = MatchStatus.CANCELLED))
        createNextMatchAfter(match.dateTimeIso)
    }

    suspend fun savePlayer(player: PlayerEntity): Long {
        val id = if (player.id == 0L) {
            playerDao.insertPlayer(player)
        } else {
            playerDao.updatePlayer(player)
            player.id
        }
        if (player.status == PlayerStatus.BLESSE) {
            // If marked injured, remove from upcoming match active lineup if not yet finished
            val upcoming = matchDao.getUpcomingMatchOnce()
            if (upcoming != null) {
                val existing = participationDao.getOne(upcoming.id, id)
                if (existing != null && existing.attendance == AttendanceStatus.PRESENT) {
                    participationDao.upsert(
                        existing.copy(attendance = AttendanceStatus.ABSENT, teamSide = null)
                    )
                    recalculateMatchFees(upcoming.id)
                }
            }
        }
        return id
    }

    suspend fun deletePlayer(player: PlayerEntity) {
        playerDao.deletePlayer(player)
    }

    suspend fun setAttendance(
        matchId: Long,
        playerId: Long,
        attendance: AttendanceStatus
    ) {
        val existing = participationDao.getOne(matchId, playerId)
        val updated = if (existing == null) {
            MatchParticipationEntity(
                matchId = matchId,
                playerId = playerId,
                attendance = attendance,
                teamSide = null
            )
        } else {
            existing.copy(
                attendance = attendance,
                teamSide = if (attendance == AttendanceStatus.PRESENT) existing.teamSide else null
            )
        }
        participationDao.upsert(updated)
        recalculateMatchFees(matchId)
    }

    suspend fun markPlayersPresentBulk(matchId: Long, playerIds: List<Long>) {
        db.withTransaction {
            for (pid in playerIds) {
                val existing = participationDao.getOne(matchId, pid)
                if (existing == null) {
                    participationDao.upsert(
                        MatchParticipationEntity(
                            matchId = matchId,
                            playerId = pid,
                            attendance = AttendanceStatus.PRESENT
                        )
                    )
                } else if (existing.attendance != AttendanceStatus.PRESENT) {
                    participationDao.upsert(existing.copy(attendance = AttendanceStatus.PRESENT))
                }
            }
        }
        recalculateMatchFees(matchId)
    }

    suspend fun generateBalancedTeams(matchId: Long, variantSeed: Int): BalancedTeamsResult {
        val match = matchDao.getMatchById(matchId) ?: error("Match introuvable")
        val settings = settingsRepository.getSettingsOnce()
        val players = playerDao.getAllPlayersOnce().associateBy { it.id }
        val participations = participationDao.getByMatchOnce(matchId)

        val presentPlayers = participations
            .filter { it.attendance == AttendanceStatus.PRESENT }
            .mapNotNull { players[it.playerId] }

        val result = TeamBalancer.generateTeams(
            presentPlayers = presentPlayers,
            recoveryFactor = settings.recoveryRatingFactor,
            variantSeed = variantSeed
        )

        db.withTransaction {
            val partByPlayer = participations.associateBy { it.playerId }
            result.blueTeam.forEach { p ->
                val row = partByPlayer[p.id] ?: MatchParticipationEntity(matchId, p.id, AttendanceStatus.PRESENT)
                participationDao.upsert(
                    row.copy(
                        teamSide = TeamSide.BLEU,
                        snapshotSkill = result.effectiveRatings[p.id] ?: p.skillRating
                    )
                )
            }
            result.orangeTeam.forEach { p ->
                val row = partByPlayer[p.id] ?: MatchParticipationEntity(matchId, p.id, AttendanceStatus.PRESENT)
                participationDao.upsert(
                    row.copy(
                        teamSide = TeamSide.ORANGE,
                        snapshotSkill = result.effectiveRatings[p.id] ?: p.skillRating
                    )
                )
            }
            if (match.status == MatchStatus.OPEN) {
                matchDao.updateMatch(match.copy(status = MatchStatus.TEAMS_READY))
            }
        }
        recalculateMatchFees(matchId)
        return result
    }

    suspend fun swapPlayers(matchId: Long, firstPlayerId: Long, secondPlayerId: Long) {
        db.withTransaction {
            val p1 = participationDao.getOne(matchId, firstPlayerId) ?: return@withTransaction
            val p2 = participationDao.getOne(matchId, secondPlayerId) ?: return@withTransaction
            participationDao.upsert(p1.copy(teamSide = p2.teamSide))
            participationDao.upsert(p2.copy(teamSide = p1.teamSide))
        }
    }

    suspend fun movePlayerToTeam(matchId: Long, playerId: Long, side: TeamSide) {
        val existing = participationDao.getOne(matchId, playerId) ?: return
        val player = playerDao.getPlayerById(playerId)
        val settings = settingsRepository.getSettingsOnce()
        participationDao.upsert(
            existing.copy(
                teamSide = side,
                snapshotSkill = existing.snapshotSkill
                    ?: player?.effectiveRating(settings.recoveryRatingFactor)
            )
        )
    }

    suspend fun recalculateMatchFees(matchId: Long) {
        val match = matchDao.getMatchById(matchId) ?: return
        val participations = participationDao.getByMatchOnce(matchId)
        val payingCount = participations.count {
            it.attendance == AttendanceStatus.PRESENT && !it.feeExempt
        }
        val feeEach = FeeCalculator.calculatePerPlayerFee(match, payingCount)

        db.withTransaction {
            for (row in participations) {
                val due = if (row.attendance == AttendanceStatus.PRESENT && !row.feeExempt) feeEach else 0.0
                val updated = row.copy(
                    feeDue = due,
                    feePaid = if (row.isPaid && row.feePaid == 0.0) due else row.feePaid
                )
                participationDao.upsert(updated)
            }
        }
    }

    suspend fun togglePaymentStatus(
        matchId: Long,
        playerId: Long,
        method: PaymentMethod = PaymentMethod.CASH
    ) {
        val row = participationDao.getOne(matchId, playerId) ?: return
        val player = playerDao.getPlayerById(playerId)
        val match = matchDao.getMatchById(matchId)
        val nowPaid = !row.isPaid

        db.withTransaction {
            val updated = row.copy(
                isPaid = nowPaid,
                feePaid = if (nowPaid) row.feeDue else 0.0,
                paymentMethod = method
            )
            participationDao.upsert(updated)
            cashDao.deleteByMatchAndPlayer(matchId, playerId)
            if (nowPaid && row.feeDue > 0.0) {
                val dateLabel = match?.let { SaturdayScheduler.formatShortDate(it.dateTimeIso) } ?: ""
                cashDao.insertTransaction(
                    CashTransactionEntity(
                        type = TransactionType.INCOME,
                        amount = row.feeDue,
                        label = "Cotisation ${player?.displayName ?: "Joueur"} ($dateLabel)",
                        matchId = matchId,
                        playerId = playerId,
                        paymentMethod = method
                    )
                )
            }
        }
    }

    suspend fun setFeeExempt(matchId: Long, playerId: Long, exempt: Boolean) {
        val row = participationDao.getOne(matchId, playerId) ?: return
        participationDao.upsert(row.copy(feeExempt = exempt))
        if (exempt) {
            cashDao.deleteByMatchAndPlayer(matchId, playerId)
        }
        recalculateMatchFees(matchId)
    }

    suspend fun updateParticipationFeeOverride(matchId: Long, playerId: Long, customFee: Double) {
        val row = participationDao.getOne(matchId, playerId) ?: return
        val clamped = FeeCalculator.roundMoney(customFee.coerceAtLeast(0.0))
        participationDao.upsert(
            row.copy(
                feeDue = clamped,
                feePaid = if (row.isPaid) clamped else row.feePaid
            )
        )
    }

    suspend fun addCashTransaction(
        type: TransactionType,
        amount: Double,
        label: String,
        paymentMethod: PaymentMethod = PaymentMethod.CASH,
        matchId: Long? = null
    ) {
        if (amount <= 0.0 || label.isBlank()) return
        cashDao.insertTransaction(
            CashTransactionEntity(
                type = type,
                amount = FeeCalculator.roundMoney(amount),
                label = label.trim(),
                matchId = matchId,
                paymentMethod = paymentMethod
            )
        )
    }

    suspend fun deleteCashTransaction(transaction: CashTransactionEntity) {
        cashDao.deleteTransaction(transaction)
    }

    suspend fun closeMatch(
        matchId: Long,
        blueScore: Int,
        orangeScore: Int,
        mvpPlayerId: Long?,
        notes: String,
        goalsByPlayer: Map<Long, Int>,
        injuredPlayerIds: Set<Long>,
        logPitchExpense: Boolean
    ) {
        val match = matchDao.getMatchById(matchId) ?: return
        val settings = settingsRepository.getSettingsOnce()
        val now = System.currentTimeMillis()

        db.withTransaction {
            matchDao.updateMatch(
                match.copy(
                    status = MatchStatus.FINISHED,
                    blueScore = blueScore.coerceAtLeast(0),
                    orangeScore = orangeScore.coerceAtLeast(0),
                    mvpPlayerId = mvpPlayerId,
                    notes = notes.trim()
                )
            )
            val rows = participationDao.getByMatchOnce(matchId)
            val players = playerDao.getAllPlayersOnce().associateBy { it.id }

            for (row in rows) {
                val player = players[row.playerId]
                val injuredNow = row.playerId in injuredPlayerIds
                val snapshot = row.snapshotSkill ?: player?.effectiveRating(settings.recoveryRatingFactor)
                participationDao.upsert(
                    row.copy(
                        goals = (goalsByPlayer[row.playerId] ?: row.goals).coerceAtLeast(0),
                        injuredInMatch = injuredNow,
                        snapshotSkill = snapshot
                    )
                )
                if (injuredNow && player != null) {
                    playerDao.updatePlayer(
                        player.copy(
                            status = PlayerStatus.BLESSE,
                            injuryDateEpoch = now,
                            injuryNote = player.injuryNote.ifBlank {
                                "Blessé lors du match du ${SaturdayScheduler.formatShortDate(match.dateTimeIso)}"
                            }
                        )
                    )
                }
            }

            if (logPitchExpense && match.pitchCost > 0.0) {
                cashDao.insertTransaction(
                    CashTransactionEntity(
                        type = TransactionType.EXPENSE,
                        amount = match.pitchCost,
                        label = "Location terrain (${SaturdayScheduler.formatShortDate(match.dateTimeIso)})",
                        matchId = matchId
                    )
                )
            }
        }
        createNextMatchAfter(match.dateTimeIso)
    }

    suspend fun updateFinishedMatchScore(
        matchId: Long,
        blueScore: Int,
        orangeScore: Int,
        mvpPlayerId: Long?,
        notes: String,
        goalsByPlayer: Map<Long, Int>
    ) {
        val match = matchDao.getMatchById(matchId) ?: return
        db.withTransaction {
            matchDao.updateMatch(
                match.copy(
                    blueScore = blueScore.coerceAtLeast(0),
                    orangeScore = orangeScore.coerceAtLeast(0),
                    mvpPlayerId = mvpPlayerId,
                    notes = notes.trim()
                )
            )
            val rows = participationDao.getByMatchOnce(matchId)
            for (row in rows) {
                if (goalsByPlayer.containsKey(row.playerId)) {
                    participationDao.upsert(
                        row.copy(goals = (goalsByPlayer[row.playerId] ?: 0).coerceAtLeast(0))
                    )
                }
            }
        }
    }

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        settingsRepository.updateSettings(transform)
    }

    suspend fun seedDemoPlayersAndMatch() {
        val existingPlayers = playerDao.getAllPlayersOnce()
        if (existingPlayers.isNotEmpty()) return

        val demoPlayers = listOf(
            PlayerEntity(name = "Karim", position = PlayerPosition.GARDIEN, skillRating = 7.5),
            PlayerEntity(name = "Youssef", position = PlayerPosition.GARDIEN, skillRating = 7.0),
            PlayerEntity(name = "Badr", position = PlayerPosition.DEFENSEUR, skillRating = 8.0),
            PlayerEntity(name = "Yassine", position = PlayerPosition.DEFENSEUR, skillRating = 7.0),
            PlayerEntity(name = "Hamza", position = PlayerPosition.DEFENSEUR, skillRating = 6.5),
            PlayerEntity(name = "Tarik", position = PlayerPosition.MILIEU, skillRating = 8.5),
            PlayerEntity(name = "Amine", position = PlayerPosition.MILIEU, skillRating = 7.5),
            PlayerEntity(name = "Mehdi", position = PlayerPosition.MILIEU, skillRating = 6.0),
            PlayerEntity(name = "Omar", position = PlayerPosition.POLYVALENT, skillRating = 7.0),
            PlayerEntity(name = "Sami", position = PlayerPosition.ATTAQUANT, skillRating = 9.0),
            PlayerEntity(name = "Reda", position = PlayerPosition.ATTAQUANT, skillRating = 8.0),
            PlayerEntity(name = "Anas", position = PlayerPosition.ATTAQUANT, skillRating = 6.5),
            PlayerEntity(
                name = "Nabil",
                position = PlayerPosition.MILIEU,
                skillRating = 7.5,
                status = PlayerStatus.EN_REPRISE,
                injuryNote = "Reprise après élongation"
            ),
            PlayerEntity(
                name = "Sofiane",
                position = PlayerPosition.DEFENSEUR,
                skillRating = 7.0,
                status = PlayerStatus.BLESSE,
                injuryNote = "Entorse cheville droite",
                injuryDateEpoch = System.currentTimeMillis() - 7L * 24 * 3600 * 1000,
                expectedReturnText = "Dans 2 semaines"
            )
        )
        val ids = playerDao.insertPlayers(demoPlayers)
        val match = ensureUpcomingMatchExists()
        // Mark the first 12 active/recovering players as present for immediate testing
        markPlayersPresentBulk(match.id, ids.take(12))
    }
}

package com.foot22.samedi.domain

import com.foot22.samedi.data.local.AppSettings
import com.foot22.samedi.data.local.CashTransactionEntity
import com.foot22.samedi.data.local.MatchEntity
import com.foot22.samedi.data.local.MatchParticipationEntity
import com.foot22.samedi.data.local.PlayerEntity
import com.foot22.samedi.data.model.AttendanceStatus
import com.foot22.samedi.data.model.FeeMode
import com.foot22.samedi.data.model.PlayerStatus
import com.foot22.samedi.data.model.TransactionType
import kotlin.math.roundToInt

data class PlayerDebtSummary(
    val player: PlayerEntity,
    val totalDue: Double,
    val totalPaid: Double,
    val unpaidBalance: Double,
    val unpaidMatchCount: Int
)

data class TreasurySummary(
    val totalIncome: Double,
    val totalExpense: Double,
    val balance: Double,
    val totalUnpaidAcrossPlayers: Double,
    val playerDebts: List<PlayerDebtSummary>
)

object FeeCalculator {

    fun calculatePerPlayerFee(
        match: MatchEntity,
        payingParticipantCount: Int
    ): Double {
        if (payingParticipantCount <= 0) return 0.0
        val raw = when (match.feeMode) {
            FeeMode.SPLIT_PITCH -> match.pitchCost / payingParticipantCount
            FeeMode.FIXED_PER_MATCH -> match.fixedFeePerPlayer
            FeeMode.MONTHLY -> 0.0
        }
        return roundMoney(raw)
    }

    fun calculateMonthlyPlayerFee(
        player: PlayerEntity,
        settings: AppSettings
    ): Double {
        if (player.status == PlayerStatus.BLESSE && settings.freezeFeeWhenInjured) {
            return 0.0
        }
        if (player.status == PlayerStatus.INACTIF) {
            return 0.0
        }
        return roundMoney(settings.monthlyFeeAmount)
    }

    fun summarizeTreasury(
        players: List<PlayerEntity>,
        participations: List<MatchParticipationEntity>,
        transactions: List<CashTransactionEntity>
    ): TreasurySummary {
        val totalIncome = transactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }
        val totalExpense = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }

        val byPlayer = participations
            .filter { it.attendance == AttendanceStatus.PRESENT && !it.feeExempt && it.feeDue > 0.0 }
            .groupBy { it.playerId }

        val debts = players.mapNotNull { player ->
            val rows = byPlayer[player.id].orEmpty()
            if (rows.isEmpty()) return@mapNotNull null
            val due = rows.sumOf { it.feeDue }
            val paid = rows.sumOf { if (it.isPaid) it.feePaid.coerceAtLeast(it.feeDue) else it.feePaid }
            val unpaid = (due - paid).coerceAtLeast(0.0)
            val unpaidMatches = rows.count { !it.isPaid && (it.feeDue - it.feePaid) > 0.01 }
            if (unpaid <= 0.01) return@mapNotNull null
            PlayerDebtSummary(
                player = player,
                totalDue = roundMoney(due),
                totalPaid = roundMoney(paid),
                unpaidBalance = roundMoney(unpaid),
                unpaidMatchCount = unpaidMatches
            )
        }.sortedByDescending { it.unpaidBalance }

        return TreasurySummary(
            totalIncome = roundMoney(totalIncome),
            totalExpense = roundMoney(totalExpense),
            balance = roundMoney(totalIncome - totalExpense),
            totalUnpaidAcrossPlayers = roundMoney(debts.sumOf { it.unpaidBalance }),
            playerDebts = debts
        )
    }

    fun roundMoney(value: Double): Double = (value * 100.0).roundToInt() / 100.0

    fun formatMoney(amount: Double, currency: String): String {
        val rounded = roundMoney(amount)
        val formatted = if (rounded % 1.0 == 0.0) {
            rounded.toInt().toString()
        } else {
            String.format(java.util.Locale.US, "%.2f", rounded).trimEnd('0').trimEnd('.')
        }
        return "$formatted $currency"
    }
}

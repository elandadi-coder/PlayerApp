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
import org.junit.Assert.assertEquals
import org.junit.Test

class FeeCalculatorTest {

    @Test
    fun `splits pitch cost evenly across paying present players`() {
        val match = MatchEntity(
            id = 1,
            dateTimeIso = "2026-11-14T22:00",
            pitchCost = 360.0,
            feeMode = FeeMode.SPLIT_PITCH
        )
        assertEquals(30.0, FeeCalculator.calculatePerPlayerFee(match, 12), 0.01)
    }

    @Test
    fun `freezes monthly fee for injured player when enabled`() {
        val injured = PlayerEntity(id = 1, name = "Sofiane", status = PlayerStatus.BLESSE)
        val active = PlayerEntity(id = 2, name = "Karim", status = PlayerStatus.ACTIF)
        val settings = AppSettings(monthlyFeeAmount = 120.0, freezeFeeWhenInjured = true)

        assertEquals(0.0, FeeCalculator.calculateMonthlyPlayerFee(injured, settings), 0.01)
        assertEquals(120.0, FeeCalculator.calculateMonthlyPlayerFee(active, settings), 0.01)
    }

    @Test
    fun `summarizes treasury balance and player debts`() {
        val p1 = PlayerEntity(id = 1, name = "Karim")
        val p2 = PlayerEntity(id = 2, name = "Badr")
        val parts = listOf(
            MatchParticipationEntity(1, 1, AttendanceStatus.PRESENT, feeDue = 30.0, feePaid = 30.0, isPaid = true),
            MatchParticipationEntity(1, 2, AttendanceStatus.PRESENT, feeDue = 30.0, feePaid = 0.0, isPaid = false)
        )
        val txs = listOf(
            CashTransactionEntity(id = 1, type = TransactionType.INCOME, amount = 30.0, label = "Karim"),
            CashTransactionEntity(id = 2, type = TransactionType.EXPENSE, amount = 10.0, label = "Eau")
        )

        val summary = FeeCalculator.summarizeTreasury(listOf(p1, p2), parts, txs)
        assertEquals(20.0, summary.balance, 0.01)
        assertEquals(30.0, summary.totalUnpaidAcrossPlayers, 0.01)
        assertEquals(1, summary.playerDebts.size)
        assertEquals("Badr", summary.playerDebts.first().player.name)
    }
}

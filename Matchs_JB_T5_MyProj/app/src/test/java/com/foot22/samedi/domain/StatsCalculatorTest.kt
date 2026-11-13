package com.foot22.samedi.domain

import com.foot22.samedi.data.local.MatchEntity
import com.foot22.samedi.data.local.MatchParticipationEntity
import com.foot22.samedi.data.local.PlayerEntity
import com.foot22.samedi.data.model.AttendanceStatus
import com.foot22.samedi.data.model.MatchStatus
import com.foot22.samedi.data.model.TeamSide
import org.junit.Assert.assertEquals
import org.junit.Test

class StatsCalculatorTest {

    @Test
    fun `computes wins losses goals and mvp count from finished matches`() {
        val p1 = PlayerEntity(id = 1, name = "Badr")
        val p2 = PlayerEntity(id = 2, name = "Sami")
        val match = MatchEntity(
            id = 10,
            dateTimeIso = "2026-11-07T22:00",
            status = MatchStatus.FINISHED,
            blueScore = 6,
            orangeScore = 4,
            mvpPlayerId = 1
        )
        val parts = listOf(
            MatchParticipationEntity(10, 1, AttendanceStatus.PRESENT, TeamSide.BLEU, goals = 2),
            MatchParticipationEntity(10, 2, AttendanceStatus.PRESENT, TeamSide.ORANGE, goals = 3)
        )

        val stats = StatsCalculator.computeLeaderboard(listOf(p1, p2), listOf(match), parts)
        val badrStats = stats.first { it.player.id == 1L }
        val samiStats = stats.first { it.player.id == 2L }

        assertEquals(1, badrStats.wins)
        assertEquals(100, badrStats.winRatePercent)
        assertEquals(2, badrStats.goals)
        assertEquals(1, badrStats.mvpCount)
        assertEquals(1, samiStats.losses)
        assertEquals(0, samiStats.winRatePercent)
    }
}

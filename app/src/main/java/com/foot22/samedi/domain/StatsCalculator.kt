package com.foot22.samedi.domain

import com.foot22.samedi.data.local.MatchEntity
import com.foot22.samedi.data.local.MatchParticipationEntity
import com.foot22.samedi.data.local.PlayerEntity
import com.foot22.samedi.data.model.AttendanceStatus
import com.foot22.samedi.data.model.MatchStatus
import com.foot22.samedi.data.model.TeamSide

enum class MatchOutcome { WIN, DRAW, LOSS }

data class PlayerSeasonStats(
    val player: PlayerEntity,
    val matchesPlayed: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val winRatePercent: Int,
    val goals: Int,
    val assists: Int,
    val mvpCount: Int,
    val recentOutcomes: List<MatchOutcome>
)

object StatsCalculator {

    fun computeLeaderboard(
        players: List<PlayerEntity>,
        matches: List<MatchEntity>,
        participations: List<MatchParticipationEntity>
    ): List<PlayerSeasonStats> {
        val finishedMatches = matches
            .filter { it.status == MatchStatus.FINISHED && it.blueScore != null && it.orangeScore != null }
            .sortedByDescending { it.dateTimeIso }

        val matchMap = finishedMatches.associateBy { it.id }
        val participationsByPlayer = participations
            .filter { it.attendance == AttendanceStatus.PRESENT && it.teamSide != null && matchMap.containsKey(it.matchId) }
            .groupBy { it.playerId }

        return players.map { player ->
            val rows = participationsByPlayer[player.id].orEmpty()
                .sortedByDescending { matchMap[it.matchId]?.dateTimeIso ?: "" }

            var wins = 0
            var draws = 0
            var losses = 0
            var goals = 0
            var assists = 0
            var mvps = 0
            val recent = mutableListOf<MatchOutcome>()

            for (row in rows) {
                val match = matchMap[row.matchId] ?: continue
                val blue = match.blueScore ?: continue
                val orange = match.orangeScore ?: continue
                goals += row.goals
                assists += row.assists
                if (match.mvpPlayerId == player.id) mvps++

                val outcome = when {
                    blue == orange -> MatchOutcome.DRAW
                    row.teamSide == TeamSide.BLEU && blue > orange -> MatchOutcome.WIN
                    row.teamSide == TeamSide.ORANGE && orange > blue -> MatchOutcome.WIN
                    else -> MatchOutcome.LOSS
                }
                when (outcome) {
                    MatchOutcome.WIN -> wins++
                    MatchOutcome.DRAW -> draws++
                    MatchOutcome.LOSS -> losses++
                }
                if (recent.size < 5) recent.add(outcome)
            }

            val played = rows.size
            val winRate = if (played == 0) 0 else ((wins * 100.0) / played).toInt()

            PlayerSeasonStats(
                player = player,
                matchesPlayed = played,
                wins = wins,
                draws = draws,
                losses = losses,
                winRatePercent = winRate,
                goals = goals,
                assists = assists,
                mvpCount = mvps,
                recentOutcomes = recent
            )
        }.sortedWith(
            compareByDescending<PlayerSeasonStats> { it.wins }
                .thenByDescending { it.winRatePercent }
                .thenByDescending { it.goals }
                .thenByDescending { it.matchesPlayed }
                .thenBy { it.player.displayName }
        )
    }
}

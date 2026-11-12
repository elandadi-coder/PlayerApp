package com.foot22.samedi.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.foot22.samedi.SamediApp
import com.foot22.samedi.data.local.AppSettings
import com.foot22.samedi.data.local.MatchEntity
import com.foot22.samedi.data.local.MatchParticipationEntity
import com.foot22.samedi.data.local.PlayerEntity
import com.foot22.samedi.data.model.AttendanceStatus
import com.foot22.samedi.data.model.MatchStatus
import com.foot22.samedi.data.model.TeamSide
import com.foot22.samedi.domain.PlayerSeasonStats
import com.foot22.samedi.domain.StatsCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoricalMatchItem(
    val match: MatchEntity,
    val bluePlayers: List<Pair<PlayerEntity, MatchParticipationEntity>>,
    val orangePlayers: List<Pair<PlayerEntity, MatchParticipationEntity>>,
    val mvp: PlayerEntity?,
    val blueAvgSnapshot: Double,
    val orangeAvgSnapshot: Double
)

data class HistoryUiState(
    val settings: AppSettings = AppSettings(),
    val matches: List<HistoricalMatchItem> = emptyList(),
    val leaderboard: List<PlayerSeasonStats> = emptyList()
)

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as SamediApp).container.repository

    val uiState: StateFlow<HistoryUiState> = combine(
        repo.settingsFlow,
        repo.matchesFlow,
        repo.playersFlow,
        repo.participationsFlow
    ) { settings, matches, players, participations ->
        val playerMap = players.associateBy { it.id }
        val pastMatches = matches
            .filter { it.status == MatchStatus.FINISHED || it.status == MatchStatus.CANCELLED }
            .sortedByDescending { it.dateTimeIso }

        val partsByMatch = participations.groupBy { it.matchId }

        val items = pastMatches.map { match ->
            val rows = partsByMatch[match.id].orEmpty()
                .filter { it.attendance == AttendanceStatus.PRESENT }

            val blue = rows.filter { it.teamSide == TeamSide.BLEU }
                .mapNotNull { r -> playerMap[r.playerId]?.let { it to r } }
            val orange = rows.filter { it.teamSide == TeamSide.ORANGE }
                .mapNotNull { r -> playerMap[r.playerId]?.let { it to r } }

            val blueAvg = if (blue.isEmpty()) 0.0 else {
                blue.map { it.second.snapshotSkill ?: it.first.skillRating }.average()
            }
            val orangeAvg = if (orange.isEmpty()) 0.0 else {
                orange.map { it.second.snapshotSkill ?: it.first.skillRating }.average()
            }

            HistoricalMatchItem(
                match = match,
                bluePlayers = blue,
                orangePlayers = orange,
                mvp = match.mvpPlayerId?.let { playerMap[it] },
                blueAvgSnapshot = blueAvg,
                orangeAvgSnapshot = orangeAvg
            )
        }

        val leaderboard = StatsCalculator.computeLeaderboard(players, matches, participations)

        HistoryUiState(
            settings = settings,
            matches = items,
            leaderboard = leaderboard
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState()
    )

    fun updatePastMatchScore(
        matchId: Long,
        blueScore: Int,
        orangeScore: Int,
        mvpPlayerId: Long?,
        notes: String,
        goalsByPlayer: Map<Long, Int>
    ) {
        viewModelScope.launch {
            repo.updateFinishedMatchScore(
                matchId = matchId,
                blueScore = blueScore,
                orangeScore = orangeScore,
                mvpPlayerId = mvpPlayerId,
                notes = notes,
                goalsByPlayer = goalsByPlayer
            )
        }
    }
}

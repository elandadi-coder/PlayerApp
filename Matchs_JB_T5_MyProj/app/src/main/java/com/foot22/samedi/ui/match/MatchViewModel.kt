package com.foot22.samedi.ui.match

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.foot22.samedi.SamediApp
import com.foot22.samedi.data.local.AppSettings
import com.foot22.samedi.data.local.MatchEntity
import com.foot22.samedi.data.local.MatchParticipationEntity
import com.foot22.samedi.data.local.PlayerEntity
import com.foot22.samedi.data.model.AttendanceStatus
import com.foot22.samedi.data.model.FeeMode
import com.foot22.samedi.data.model.PlayerPosition
import com.foot22.samedi.data.model.PlayerStatus
import com.foot22.samedi.data.model.TeamSide
import com.foot22.samedi.domain.BalancedTeamsResult
import com.foot22.samedi.domain.TeamBalancer
import com.foot22.samedi.domain.WhatsAppFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MatchUiState(
    val match: MatchEntity? = null,
    val settings: AppSettings = AppSettings(),
    val allPlayers: List<PlayerEntity> = emptyList(),
    val participationsByPlayer: Map<Long, MatchParticipationEntity> = emptyMap(),
    val presentPlayers: List<PlayerEntity> = emptyList(),
    val uncertainPlayers: List<PlayerEntity> = emptyList(),
    val waitlistPlayers: List<PlayerEntity> = emptyList(),
    val injuredPlayers: List<PlayerEntity> = emptyList(),
    val blueTeam: List<PlayerEntity> = emptyList(),
    val orangeTeam: List<PlayerEntity> = emptyList(),
    val unassignedPresent: List<PlayerEntity> = emptyList(),
    val teamSummary: BalancedTeamsResult? = null,
    val selectedSwapPlayerId: Long? = null,
    val variantSeed: Int = 0
)

class MatchViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as SamediApp).container
    private val repo = container.repository

    private val selectedSwapId = MutableStateFlow<Long?>(null)
    private val variantSeed = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            repo.ensureUpcomingMatchExists()
        }
    }

    val uiState: StateFlow<MatchUiState> = combine(
        combine(
            repo.upcomingMatchFlow,
            repo.playersFlow,
            repo.participationsFlow,
            ::Triple
        ),
        repo.settingsFlow,
        selectedSwapId,
        variantSeed
    ) { (match, players, allParticipations), settings, swapId, seed ->

        if (match == null) {
            return@combine MatchUiState(settings = settings, allPlayers = players)
        }

        val matchParts = allParticipations.filter { it.matchId == match.id }
        val partMap = matchParts.associateBy { it.playerId }
        val playerMap = players.associateBy { it.id }

        val injured = players.filter { it.status == PlayerStatus.BLESSE }
        val activePool = players.filter { it.status != PlayerStatus.BLESSE && it.status != PlayerStatus.INACTIF }

        val present = activePool.filter { partMap[it.id]?.attendance == AttendanceStatus.PRESENT }
        val uncertain = activePool.filter { partMap[it.id]?.attendance == AttendanceStatus.UNCERTAIN }
        val waitlist = activePool.filter { partMap[it.id]?.attendance == AttendanceStatus.WAITLIST }

        val blue = present.filter { partMap[it.id]?.teamSide == TeamSide.BLEU }
        val orange = present.filter { partMap[it.id]?.teamSide == TeamSide.ORANGE }
        val unassigned = present.filter { partMap[it.id]?.teamSide == null }

        val ratings = present.associate { it.id to it.effectiveRating(settings.recoveryRatingFactor) }
        val summary = if (blue.isNotEmpty() || orange.isNotEmpty()) {
            TeamBalancer.summarize(blue, orange, ratings)
        } else {
            null
        }

        MatchUiState(
            match = match,
            settings = settings,
            allPlayers = players,
            participationsByPlayer = partMap,
            presentPlayers = present,
            uncertainPlayers = uncertain,
            waitlistPlayers = waitlist,
            injuredPlayers = injured,
            blueTeam = blue,
            orangeTeam = orange,
            unassignedPresent = unassigned,
            teamSummary = summary,
            selectedSwapPlayerId = if (playerMap.containsKey(swapId)) swapId else null,
            variantSeed = seed
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MatchUiState()
    )

    fun setAttendance(playerId: Long, status: AttendanceStatus) {
        val matchId = uiState.value.match?.id ?: return
        viewModelScope.launch {
            repo.setAttendance(matchId, playerId, status)
        }
    }

    fun markRecoveringAndPresent(player: PlayerEntity) {
        val matchId = uiState.value.match?.id ?: return
        viewModelScope.launch {
            repo.savePlayer(player.copy(status = PlayerStatus.EN_REPRISE))
            repo.setAttendance(matchId, player.id, AttendanceStatus.PRESENT)
        }
    }

    fun generateTeams(reshuffle: Boolean = false) {
        val matchId = uiState.value.match?.id ?: return
        val nextSeed = if (reshuffle) variantSeed.value + 1 else variantSeed.value
        variantSeed.value = nextSeed
        selectedSwapId.value = null
        viewModelScope.launch {
            repo.generateBalancedTeams(matchId, nextSeed)
        }
    }

    fun onPlayerClickForSwap(playerId: Long) {
        val matchId = uiState.value.match?.id ?: return
        val first = selectedSwapId.value
        if (first == null) {
            selectedSwapId.value = playerId
        } else if (first == playerId) {
            selectedSwapId.value = null
        } else {
            selectedSwapId.value = null
            viewModelScope.launch {
                repo.swapPlayers(matchId, first, playerId)
            }
        }
    }

    fun assignPlayerToSide(playerId: Long, side: TeamSide) {
        val matchId = uiState.value.match?.id ?: return
        selectedSwapId.value = null
        viewModelScope.launch {
            repo.movePlayerToTeam(matchId, playerId, side)
        }
    }

    fun clearSwapSelection() {
        selectedSwapId.value = null
    }

    fun addQuickGuest(name: String, position: PlayerPosition, skill: Double) {
        val matchId = uiState.value.match?.id ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            val newId = repo.savePlayer(
                PlayerEntity(
                    name = name.trim(),
                    position = position,
                    skillRating = skill.coerceIn(1.0, 10.0),
                    isGuest = true
                )
            )
            repo.setAttendance(matchId, newId, AttendanceStatus.PRESENT)
        }
    }

    fun parseWhatsAppList(rawText: String): List<PlayerEntity> {
        val eligible = uiState.value.allPlayers.filter {
            it.status != PlayerStatus.BLESSE && it.status != PlayerStatus.INACTIF
        }
        return WhatsAppFormatter.parseAttendanceNames(rawText, eligible)
    }

    fun applyImportedPresentPlayers(playerIds: List<Long>) {
        val matchId = uiState.value.match?.id ?: return
        viewModelScope.launch {
            repo.markPlayersPresentBulk(matchId, playerIds)
        }
    }

    fun updateMatchSettings(
        dateTimeIso: String,
        location: String,
        maxPlayers: Int,
        pitchCost: Double,
        feeMode: FeeMode,
        fixedFee: Double
    ) {
        val current = uiState.value.match ?: return
        viewModelScope.launch {
            repo.updateMatchDetails(
                current.copy(
                    dateTimeIso = dateTimeIso,
                    location = location.ifBlank { current.location },
                    maxPlayers = maxPlayers.coerceIn(4, 30),
                    pitchCost = pitchCost.coerceAtLeast(0.0),
                    feeMode = feeMode,
                    fixedFeePerPlayer = fixedFee.coerceAtLeast(0.0)
                )
            )
        }
    }

    fun cancelCurrentMatch() {
        val current = uiState.value.match ?: return
        viewModelScope.launch {
            repo.cancelMatchAndScheduleNext(current)
        }
    }

    fun closeMatch(
        blueScore: Int,
        orangeScore: Int,
        mvpPlayerId: Long?,
        notes: String,
        goalsByPlayer: Map<Long, Int>,
        injuredPlayerIds: Set<Long>,
        logPitchExpense: Boolean
    ) {
        val current = uiState.value.match ?: return
        viewModelScope.launch {
            repo.closeMatch(
                matchId = current.id,
                blueScore = blueScore,
                orangeScore = orangeScore,
                mvpPlayerId = mvpPlayerId,
                notes = notes,
                goalsByPlayer = goalsByPlayer,
                injuredPlayerIds = injuredPlayerIds,
                logPitchExpense = logPitchExpense
            )
        }
    }

    fun seedDemoData() {
        viewModelScope.launch {
            repo.seedDemoPlayersAndMatch()
        }
    }
}

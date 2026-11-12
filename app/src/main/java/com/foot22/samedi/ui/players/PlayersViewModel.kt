package com.foot22.samedi.ui.players

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.foot22.samedi.SamediApp
import com.foot22.samedi.data.local.AppSettings
import com.foot22.samedi.data.local.PlayerEntity
import com.foot22.samedi.data.model.PlayerStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PlayerFilter(val label: String) {
    ALL("Tous"),
    ACTIVE("Actifs"),
    INJURED("Infirmerie 🩹"),
    RECOVERING("En reprise")
}

enum class PlayerSort(val label: String) {
    RATING_DESC("Niveau ↓"),
    NAME_ASC("Nom A-Z")
}

data class PlayersUiState(
    val players: List<PlayerEntity> = emptyList(),
    val filteredPlayers: List<PlayerEntity> = emptyList(),
    val searchQuery: String = "",
    val filter: PlayerFilter = PlayerFilter.ALL,
    val sort: PlayerSort = PlayerSort.RATING_DESC,
    val settings: AppSettings = AppSettings(),
    val activeCount: Int = 0,
    val injuredCount: Int = 0,
    val recoveringCount: Int = 0
)

class PlayersViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as SamediApp).container.repository

    private val searchQuery = MutableStateFlow("")
    private val selectedFilter = MutableStateFlow(PlayerFilter.ALL)
    private val selectedSort = MutableStateFlow(PlayerSort.RATING_DESC)

    val uiState: StateFlow<PlayersUiState> = combine(
        repo.playersFlow,
        repo.settingsFlow,
        searchQuery,
        selectedFilter,
        selectedSort
    ) { players, settings, query, filter, sort ->
        val filtered = players
            .filter { p ->
                val matchesFilter = when (filter) {
                    PlayerFilter.ALL -> true
                    PlayerFilter.ACTIVE -> p.status == PlayerStatus.ACTIF
                    PlayerFilter.INJURED -> p.status == PlayerStatus.BLESSE
                    PlayerFilter.RECOVERING -> p.status == PlayerStatus.EN_REPRISE
                }
                val q = query.trim().lowercase()
                val matchesQuery = q.isEmpty() ||
                    p.name.lowercase().contains(q) ||
                    p.nickname.lowercase().contains(q) ||
                    p.position.label.lowercase().contains(q)
                matchesFilter && matchesQuery
            }
            .let { list ->
                when (sort) {
                    PlayerSort.RATING_DESC -> list.sortedByDescending {
                        it.effectiveRating(settings.recoveryRatingFactor)
                    }
                    PlayerSort.NAME_ASC -> list.sortedBy { it.displayName.lowercase() }
                }
            }

        PlayersUiState(
            players = players,
            filteredPlayers = filtered,
            searchQuery = query,
            filter = filter,
            sort = sort,
            settings = settings,
            activeCount = players.count { it.status == PlayerStatus.ACTIF },
            injuredCount = players.count { it.status == PlayerStatus.BLESSE },
            recoveringCount = players.count { it.status == PlayerStatus.EN_REPRISE }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlayersUiState()
    )

    fun setSearch(query: String) {
        searchQuery.value = query
    }

    fun setFilter(filter: PlayerFilter) {
        selectedFilter.value = filter
    }

    fun setSort(sort: PlayerSort) {
        selectedSort.value = sort
    }

    fun savePlayer(player: PlayerEntity) {
        viewModelScope.launch {
            val withInjuryDate = if (player.status == PlayerStatus.BLESSE && player.injuryDateEpoch == null) {
                player.copy(injuryDateEpoch = System.currentTimeMillis())
            } else if (player.status == PlayerStatus.ACTIF) {
                player.copy(injuryDateEpoch = null, injuryNote = "", expectedReturnText = "")
            } else {
                player
            }
            repo.savePlayer(withInjuryDate)
        }
    }

    fun deletePlayer(player: PlayerEntity) {
        viewModelScope.launch {
            repo.deletePlayer(player)
        }
    }

    fun seedDemoPlayers() {
        viewModelScope.launch {
            repo.seedDemoPlayersAndMatch()
        }
    }
}

package com.foot22.samedi.ui.fees

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.foot22.samedi.SamediApp
import com.foot22.samedi.data.local.AppSettings
import com.foot22.samedi.data.local.CashTransactionEntity
import com.foot22.samedi.data.local.MatchEntity
import com.foot22.samedi.data.local.MatchParticipationEntity
import com.foot22.samedi.data.local.PlayerEntity
import com.foot22.samedi.data.model.AttendanceStatus
import com.foot22.samedi.data.model.PaymentMethod
import com.foot22.samedi.data.model.PlayerStatus
import com.foot22.samedi.data.model.TransactionType
import com.foot22.samedi.domain.FeeCalculator
import com.foot22.samedi.domain.TreasurySummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MatchFeeParticipantRow(
    val player: PlayerEntity,
    val participation: MatchParticipationEntity
)

data class FeesUiState(
    val settings: AppSettings = AppSettings(),
    val upcomingMatch: MatchEntity? = null,
    val currentMatchRows: List<MatchFeeParticipantRow> = emptyList(),
    val monthlyFreezePlayers: List<PlayerEntity> = emptyList(),
    val treasury: TreasurySummary = TreasurySummary(0.0, 0.0, 0.0, 0.0, emptyList()),
    val transactions: List<CashTransactionEntity> = emptyList()
)

class FeesViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as SamediApp).container.repository

    val uiState: StateFlow<FeesUiState> = combine(
        repo.settingsFlow,
        repo.upcomingMatchFlow,
        repo.playersFlow,
        repo.participationsFlow,
        repo.transactionsFlow
    ) { settings, upcoming, players, participations, transactions ->
        val playerMap = players.associateBy { it.id }
        val currentRows = if (upcoming != null) {
            participations
                .filter { it.matchId == upcoming.id && it.attendance == AttendanceStatus.PRESENT }
                .mapNotNull { part ->
                    playerMap[part.playerId]?.let { MatchFeeParticipantRow(it, part) }
                }
                .sortedBy { it.player.displayName.lowercase() }
        } else {
            emptyList()
        }

        val frozenPlayers = if (settings.freezeFeeWhenInjured) {
            players.filter { it.status == PlayerStatus.BLESSE }
        } else {
            emptyList()
        }

        val treasury = FeeCalculator.summarizeTreasury(players, participations, transactions)

        FeesUiState(
            settings = settings,
            upcomingMatch = upcoming,
            currentMatchRows = currentRows,
            monthlyFreezePlayers = frozenPlayers,
            treasury = treasury,
            transactions = transactions
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FeesUiState()
    )

    fun togglePaid(matchId: Long, playerId: Long, method: PaymentMethod) {
        viewModelScope.launch {
            repo.togglePaymentStatus(matchId, playerId, method)
        }
    }

    fun toggleExempt(matchId: Long, playerId: Long, currentlyExempt: Boolean) {
        viewModelScope.launch {
            repo.setFeeExempt(matchId, playerId, !currentlyExempt)
        }
    }

    fun overridePlayerFee(matchId: Long, playerId: Long, customAmount: Double) {
        viewModelScope.launch {
            repo.updateParticipationFeeOverride(matchId, playerId, customAmount)
        }
    }

    fun addManualTransaction(type: TransactionType, amount: Double, label: String, method: PaymentMethod) {
        viewModelScope.launch {
            repo.addCashTransaction(type, amount, label, method)
        }
    }

    fun deleteTransaction(tx: CashTransactionEntity) {
        viewModelScope.launch {
            repo.deleteCashTransaction(tx)
        }
    }
}

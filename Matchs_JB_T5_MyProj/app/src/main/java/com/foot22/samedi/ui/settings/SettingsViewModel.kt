package com.foot22.samedi.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.foot22.samedi.SamediApp
import com.foot22.samedi.data.local.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val statusMessage: String? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as SamediApp).container
    private val repo = container.repository
    private val backupManager = container.backupManager

    private val messageFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        repo.settingsFlow,
        messageFlow
    ) { settings, msg ->
        SettingsUiState(settings = settings, statusMessage = msg)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            repo.updateSettings(transform)
        }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            backupManager.exportToUri(uri)
                .onSuccess { count ->
                    messageFlow.value = "Sauvegarde exportée ($count éléments)."
                }
                .onFailure { err ->
                    messageFlow.value = "Erreur export : ${err.localizedMessage}"
                }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            backupManager.importFromUri(uri)
                .onSuccess { payload ->
                    messageFlow.value =
                        "Restauration réussie (${payload.players.size} joueurs, ${payload.matches.size} matchs)."
                }
                .onFailure { err ->
                    messageFlow.value = "Erreur import : ${err.localizedMessage}"
                }
        }
    }

    fun seedDemoPlayers() {
        viewModelScope.launch {
            repo.seedDemoPlayersAndMatch()
            messageFlow.value = "Joueurs d'exemple chargés (si la liste était vide)."
        }
    }

    fun clearMessage() {
        messageFlow.value = null
    }
}

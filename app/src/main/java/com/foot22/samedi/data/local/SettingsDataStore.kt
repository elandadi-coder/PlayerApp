package com.foot22.samedi.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.foot22.samedi.data.model.FeeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import java.time.DayOfWeek

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "samedi22_settings")

@Serializable
data class AppSettings(
    val matchDayOfWeek: Int = DayOfWeek.SATURDAY.value, // 6 = Samedi
    val matchHour: Int = 22,
    val matchMinute: Int = 0,
    val defaultLocation: String = "Terrain habituel",
    val defaultMaxPlayers: Int = 12,
    val blueTeamName: String = "Équipe Bleue",
    val orangeTeamName: String = "Équipe Orange",
    val defaultFeeMode: FeeMode = FeeMode.SPLIT_PITCH,
    val defaultPitchCost: Double = 360.0,
    val defaultFixedFee: Double = 30.0,
    val monthlyFeeAmount: Double = 120.0,
    val freezeFeeWhenInjured: Boolean = true,
    val currency: String = "DH",
    val recoveryRatingFactor: Double = 0.85
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val MATCH_DAY = intPreferencesKey("match_day_of_week")
        val MATCH_HOUR = intPreferencesKey("match_hour")
        val MATCH_MINUTE = intPreferencesKey("match_minute")
        val LOCATION = stringPreferencesKey("default_location")
        val MAX_PLAYERS = intPreferencesKey("default_max_players")
        val BLUE_NAME = stringPreferencesKey("blue_team_name")
        val ORANGE_NAME = stringPreferencesKey("orange_team_name")
        val FEE_MODE = stringPreferencesKey("default_fee_mode")
        val PITCH_COST = doublePreferencesKey("default_pitch_cost")
        val FIXED_FEE = doublePreferencesKey("default_fixed_fee")
        val MONTHLY_FEE = doublePreferencesKey("monthly_fee_amount")
        val FREEZE_INJURED = booleanPreferencesKey("freeze_fee_when_injured")
        val CURRENCY = stringPreferencesKey("currency")
        val RECOVERY_FACTOR = doublePreferencesKey("recovery_rating_factor")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map(::mapPreferences)

    suspend fun getSettingsOnce(): AppSettings = settingsFlow.first()

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        context.settingsDataStore.edit { prefs ->
            val updated = transform(mapPreferences(prefs))
            prefs[Keys.MATCH_DAY] = updated.matchDayOfWeek.coerceIn(1, 7)
            prefs[Keys.MATCH_HOUR] = updated.matchHour.coerceIn(0, 23)
            prefs[Keys.MATCH_MINUTE] = updated.matchMinute.coerceIn(0, 59)
            prefs[Keys.LOCATION] = updated.defaultLocation.ifBlank { "Terrain habituel" }
            prefs[Keys.MAX_PLAYERS] = updated.defaultMaxPlayers.coerceIn(4, 30)
            prefs[Keys.BLUE_NAME] = updated.blueTeamName.ifBlank { "Équipe Bleue" }
            prefs[Keys.ORANGE_NAME] = updated.orangeTeamName.ifBlank { "Équipe Orange" }
            prefs[Keys.FEE_MODE] = updated.defaultFeeMode.name
            prefs[Keys.PITCH_COST] = updated.defaultPitchCost.coerceAtLeast(0.0)
            prefs[Keys.FIXED_FEE] = updated.defaultFixedFee.coerceAtLeast(0.0)
            prefs[Keys.MONTHLY_FEE] = updated.monthlyFeeAmount.coerceAtLeast(0.0)
            prefs[Keys.FREEZE_INJURED] = updated.freezeFeeWhenInjured
            prefs[Keys.CURRENCY] = updated.currency.ifBlank { "DH" }
            prefs[Keys.RECOVERY_FACTOR] = updated.recoveryRatingFactor.coerceIn(0.5, 1.0)
        }
    }

    private fun mapPreferences(prefs: Preferences): AppSettings = AppSettings(
        matchDayOfWeek = prefs[Keys.MATCH_DAY] ?: DayOfWeek.SATURDAY.value,
        matchHour = prefs[Keys.MATCH_HOUR] ?: 22,
        matchMinute = prefs[Keys.MATCH_MINUTE] ?: 0,
        defaultLocation = prefs[Keys.LOCATION] ?: "Terrain habituel",
        defaultMaxPlayers = prefs[Keys.MAX_PLAYERS] ?: 12,
        blueTeamName = prefs[Keys.BLUE_NAME] ?: "Équipe Bleue",
        orangeTeamName = prefs[Keys.ORANGE_NAME] ?: "Équipe Orange",
        defaultFeeMode = prefs[Keys.FEE_MODE]?.let {
            runCatching { FeeMode.valueOf(it) }.getOrNull()
        } ?: FeeMode.SPLIT_PITCH,
        defaultPitchCost = prefs[Keys.PITCH_COST] ?: 360.0,
        defaultFixedFee = prefs[Keys.FIXED_FEE] ?: 30.0,
        monthlyFeeAmount = prefs[Keys.MONTHLY_FEE] ?: 120.0,
        freezeFeeWhenInjured = prefs[Keys.FREEZE_INJURED] ?: true,
        currency = prefs[Keys.CURRENCY] ?: "DH",
        recoveryRatingFactor = prefs[Keys.RECOVERY_FACTOR] ?: 0.85
    )

    suspend fun replaceSettings(updated: AppSettings) {
        updateSettings { updated }
    }
}

package com.foot22.samedi.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.foot22.samedi.data.model.FeeMode
import com.foot22.samedi.ui.theme.StadiumOutline
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = state.settings
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.statusMessage) {
        val msg = state.statusMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) viewModel.exportBackup(uri)
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importBackup(uri)
    }

    var location by remember(settings.defaultLocation) { mutableStateOf(settings.defaultLocation) }
    var blueName by remember(settings.blueTeamName) { mutableStateOf(settings.blueTeamName) }
    var orangeName by remember(settings.orangeTeamName) { mutableStateOf(settings.orangeTeamName) }
    var maxPlayersText by remember(settings.defaultMaxPlayers) { mutableStateOf(settings.defaultMaxPlayers.toString()) }
    var hourText by remember(settings.matchHour) { mutableStateOf(settings.matchHour.toString()) }
    var minuteText by remember(settings.matchMinute) { mutableStateOf("%02d".format(settings.matchMinute)) }
    var currency by remember(settings.currency) { mutableStateOf(settings.currency) }
    var pitchCostText by remember(settings.defaultPitchCost) { mutableStateOf(settings.defaultPitchCost.toInt().toString()) }
    var fixedFeeText by remember(settings.defaultFixedFee) { mutableStateOf(settings.defaultFixedFee.toInt().toString()) }
    var monthlyFeeText by remember(settings.monthlyFeeAmount) { mutableStateOf(settings.monthlyFeeAmount.toInt().toString()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres & Sauvegarde") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SettingsSectionCard(title = "Match récurrent & Équipes") {
                    Text("Jour habituel :", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DayOfWeek.entries.forEach { day ->
                            FilterChip(
                                selected = settings.matchDayOfWeek == day.value,
                                onClick = { viewModel.updateSettings { it.copy(matchDayOfWeek = day.value) } },
                                label = {
                                    Text(day.getDisplayName(TextStyle.SHORT, Locale.FRENCH))
                                }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = hourText,
                            onValueChange = { hourText = it.filter(Char::isDigit) },
                            label = { Text("Heure") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = minuteText,
                            onValueChange = { minuteText = it.filter(Char::isDigit) },
                            label = { Text("Minute") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = maxPlayersText,
                            onValueChange = { maxPlayersText = it.filter(Char::isDigit) },
                            label = { Text("Joueurs max") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1.2f)
                        )
                    }

                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Terrain par défaut") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = blueName,
                            onValueChange = { blueName = it },
                            label = { Text("Nom équipe 1") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = orangeName,
                            onValueChange = { orangeName = it },
                            label = { Text("Nom équipe 2") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text(
                        text = "Niveau joueur en reprise : ${(settings.recoveryRatingFactor * 100).roundToInt()}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = settings.recoveryRatingFactor.toFloat(),
                        onValueChange = { v ->
                            val snapped = (v * 20f).roundToInt() / 20.0
                            viewModel.updateSettings { it.copy(recoveryRatingFactor = snapped) }
                        },
                        valueRange = 0.6f..1.0f
                    )
                }
            }

            item {
                SettingsSectionCard(title = "Cotisations & Devise") {
                    Text("Mode par défaut :", style = MaterialTheme.typography.labelMedium)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FeeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = settings.defaultFeeMode == mode,
                                onClick = { viewModel.updateSettings { it.copy(defaultFeeMode = mode) } },
                                label = { Text(mode.label) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = pitchCostText,
                            onValueChange = { pitchCostText = it },
                            label = { Text("Coût terrain") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = fixedFeeText,
                            onValueChange = { fixedFeeText = it },
                            label = { Text("Tarif fixe") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = currency,
                            onValueChange = { currency = it },
                            label = { Text("Devise") },
                            singleLine = true,
                            modifier = Modifier.weight(0.7f)
                        )
                    }

                    OutlinedTextField(
                        value = monthlyFeeText,
                        onValueChange = { monthlyFeeText = it },
                        label = { Text("Cotisation mensuelle (${settings.currency})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Geler cotisation mensuelle si blessé 🩹",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = settings.freezeFeeWhenInjured,
                            onCheckedChange = { checked ->
                                viewModel.updateSettings { it.copy(freezeFeeWhenInjured = checked) }
                            }
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.updateSettings { s ->
                                s.copy(
                                    matchHour = hourText.toIntOrNull() ?: s.matchHour,
                                    matchMinute = minuteText.toIntOrNull() ?: s.matchMinute,
                                    defaultMaxPlayers = maxPlayersText.toIntOrNull() ?: s.defaultMaxPlayers,
                                    defaultLocation = location,
                                    blueTeamName = blueName,
                                    orangeTeamName = orangeName,
                                    defaultPitchCost = pitchCostText.toDoubleOrNull() ?: s.defaultPitchCost,
                                    defaultFixedFee = fixedFeeText.toDoubleOrNull() ?: s.defaultFixedFee,
                                    monthlyFeeAmount = monthlyFeeText.toDoubleOrNull() ?: s.monthlyFeeAmount,
                                    currency = currency
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Enregistrer les réglages")
                    }
                }
            }

            item {
                SettingsSectionCard(title = "Sauvegarde JSON & Données") {
                    Text(
                        text = "Exporte toutes tes données (joueurs, matchs, historique, caisse) en fichier JSON sur ton téléphone ou Drive.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { exportLauncher.launch("samedi22_backup.json") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudUpload, null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Exporter")
                        }
                        OutlinedButton(
                            onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudDownload, null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Importer")
                        }
                    }
                    OutlinedButton(
                        onClick = { viewModel.seedDemoPlayers() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Charger joueurs de démo (si liste vide)")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, StadiumOutline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

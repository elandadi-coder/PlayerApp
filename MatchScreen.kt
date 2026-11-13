package com.foot22.samedi.ui.match

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.foot22.samedi.data.local.AppSettings
import com.foot22.samedi.data.local.MatchEntity
import com.foot22.samedi.data.local.PlayerEntity
import com.foot22.samedi.data.model.AttendanceStatus
import com.foot22.samedi.data.model.FeeMode
import com.foot22.samedi.data.model.PlayerPosition
import com.foot22.samedi.data.model.PlayerStatus
import com.foot22.samedi.data.model.TeamSide
import com.foot22.samedi.domain.BalancedTeamsResult
import com.foot22.samedi.domain.FeeCalculator
import com.foot22.samedi.domain.SaturdayScheduler
import com.foot22.samedi.domain.WhatsAppFormatter
import com.foot22.samedi.ui.components.SkillBadge
import com.foot22.samedi.ui.components.StatusBadge
import com.foot22.samedi.ui.components.teamAccentColor
import com.foot22.samedi.ui.components.teamContainerColor
import com.foot22.samedi.ui.components.teamSolidColor
import com.foot22.samedi.ui.theme.InjuryRed
import com.foot22.samedi.ui.theme.InjuryRedContainer
import com.foot22.samedi.ui.theme.PitchEmerald
import com.foot22.samedi.ui.theme.PitchEmeraldContainer
import com.foot22.samedi.ui.theme.StadiumCardAlt
import com.foot22.samedi.ui.theme.StadiumOutline
import com.foot22.samedi.ui.theme.TeamBlue
import com.foot22.samedi.ui.theme.TeamBlueContainer
import com.foot22.samedi.ui.theme.TeamBlueLight
import com.foot22.samedi.ui.theme.TeamOrange
import com.foot22.samedi.ui.theme.TeamOrangeContainer
import com.foot22.samedi.ui.theme.TeamOrangeLight
import com.foot22.samedi.ui.theme.WarningAmber
import com.foot22.samedi.ui.theme.WarningAmberContainer
import com.foot22.samedi.util.MatchCardImageGenerator
import com.foot22.samedi.util.SmsShareHelper
import com.foot22.samedi.util.WhatsAppShareHelper
import java.time.LocalDateTime
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun MatchScreen(
    viewModel: MatchViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showShareMenu by remember { mutableStateOf(false) }
    var showSmartPasteDialog by remember { mutableStateOf(false) }
    var showGuestDialog by remember { mutableStateOf(false) }
    var showEditMatchDialog by remember { mutableStateOf(false) }
    var showCloseMatchDialog by remember { mutableStateOf(false) }

    val match = state.match
    if (match == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Préparation du match de samedi...")
        }
        return
    }

    val activePool = remember(state.allPlayers) {
        state.allPlayers.filter { it.status != PlayerStatus.BLESSE && it.status != PlayerStatus.INACTIF }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Hero Match Header
        item {
            MatchHeroCard(
                match = match,
                presentCount = state.presentPlayers.size,
                uncertainCount = state.uncertainPlayers.size,
                injuredCount = state.injuredPlayers.size,
                onEditClick = { showEditMatchDialog = true }
            )
        }

        // 2. Empty players onboarding CTA
        if (state.allPlayers.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, StadiumOutline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Aucun joueur enregistré pour le moment",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Ajoute tes joueurs dans l'onglet Joueurs ou charge un groupe de démonstration pour tester tout de suite.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = { viewModel.seedDemoData() }) {
                            Text("Charger 14 joueurs d'exemple")
                        }
                    }
                }
            }
        }

        // 3. Action bar: Generate Bleu vs Orange + Share WhatsApp + Close Match
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val alreadyHasTeams = state.blueTeam.isNotEmpty() || state.orangeTeam.isNotEmpty()
                            viewModel.generateTeams(reshuffle = alreadyHasTeams)
                        },
                        enabled = state.presentPlayers.size >= 2,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (state.blueTeam.isEmpty() && state.orangeTeam.isEmpty()) {
                                "Faire Bleu vs Orange"
                            } else {
                                "Re-mélanger"
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box {
                        OutlinedButton(
                            onClick = { showShareMenu = true },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Partager ou SMS",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Partager / SMS")
                        }
                        DropdownMenu(
                            expanded = showShareMenu,
                            onDismissRequest = { showShareMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Confirmer par SMS aux joueurs") },
                                leadingIcon = { Icon(Icons.Default.Sms, null) },
                                enabled = state.presentPlayers.isNotEmpty(),
                                onClick = {
                                    showShareMenu = false
                                    val summary = state.teamSummary
                                    val msg = WhatsAppFormatter.formatMatchConfirmationSms(
                                        match = match,
                                        bluePlayers = summary?.blueTeam ?: emptyList(),
                                        orangePlayers = summary?.orangeTeam ?: emptyList(),
                                        settings = state.settings
                                    )
                                    val phones = state.presentPlayers.map { it.phone }
                                    SmsShareHelper.sendMatchConfirmationSms(context, phones, msg)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Appel aux présences WhatsApp") },
                                leadingIcon = { Icon(Icons.Default.Share, null) },
                                onClick = {
                                    showShareMenu = false
                                    val msg = WhatsAppFormatter.formatAttendanceCall(
                                        match = match,
                                        present = state.presentPlayers,
                                        uncertain = state.uncertainPlayers,
                                        injured = state.injuredPlayers,
                                        settings = state.settings
                                    )
                                    WhatsAppShareHelper.shareTextToWhatsApp(context, msg)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Compo Bleu vs Orange (texte)") },
                                leadingIcon = { Icon(Icons.Default.SportsSoccer, null) },
                                enabled = state.blueTeam.isNotEmpty() || state.orangeTeam.isNotEmpty(),
                                onClick = {
                                    showShareMenu = false
                                    val summary = state.teamSummary ?: return@DropdownMenuItem
                                    val msg = WhatsAppFormatter.formatTeamsLineup(
                                        match = match,
                                        teams = summary,
                                        injured = state.injuredPlayers,
                                        settings = state.settings
                                    )
                                    WhatsAppShareHelper.shareTextToWhatsApp(context, msg)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Carte visuelle Bleu vs Orange (image)") },
                                leadingIcon = { Icon(Icons.Default.Image, null) },
                                enabled = state.blueTeam.isNotEmpty() || state.orangeTeam.isNotEmpty(),
                                onClick = {
                                    showShareMenu = false
                                    val summary = state.teamSummary ?: return@DropdownMenuItem
                                    val uri = MatchCardImageGenerator.generateLineupOrScoreCard(
                                        context = context,
                                        match = match,
                                        bluePlayers = summary.blueTeam,
                                        orangePlayers = summary.orangeTeam,
                                        settings = state.settings,
                                        blueAvg = summary.blueAvgSkill,
                                        orangeAvg = summary.orangeAvgSkill
                                    )
                                    val caption = "⚽ ${SaturdayScheduler.formatFrenchDateTime(match.dateTimeIso)} — ${state.settings.blueTeamName} vs ${state.settings.orangeTeamName}"
                                    WhatsAppShareHelper.shareImageAndTextToWhatsApp(context, uri, caption)
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showSmartPasteDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentPaste, null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Coller WhatsApp", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = { showGuestDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ Invité", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                if (state.presentPlayers.isNotEmpty()) {
                    Button(
                        onClick = { showCloseMatchDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PitchEmeraldContainer,
                            contentColor = PitchEmerald
                        ),
                        border = BorderStroke(1.dp, PitchEmerald.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Saisir le score & Clôturer le match")
                    }
                }
            }
        }

        // 4. Generated Bleu vs Orange Teams Card
        if (state.blueTeam.isNotEmpty() || state.orangeTeam.isNotEmpty()) {
            item {
                TeamsComparisonCard(
                    settings = state.settings,
                    summary = state.teamSummary!!,
                    unassigned = state.unassignedPresent,
                    selectedSwapId = state.selectedSwapPlayerId,
                    onPlayerTapForSwap = { viewModel.onPlayerClickForSwap(it) },
                    onClearSwap = { viewModel.clearSwapSelection() },
                    onAssignSide = { playerId, side -> viewModel.assignPlayerToSide(playerId, side) }
                )
            }
        }

        // 5. Attendance Checklist Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Présences (${state.presentPlayers.size}/${match.maxPlayers})",
                    style = MaterialTheme.typography.titleMedium
                )
                val perPlayerFee = FeeCalculator.calculatePerPlayerFee(match, state.presentPlayers.size)
                if (match.feeMode != FeeMode.MONTHLY && state.presentPlayers.isNotEmpty()) {
                    StatusBadge(
                        text = "${FeeCalculator.formatMoney(perPlayerFee, state.settings.currency)} / joueur",
                        containerColor = PitchEmeraldContainer,
                        contentColor = PitchEmerald
                    )
                }
            }
        }

        items(activePool, key = { it.id }) { player ->
            val currentAttendance = state.participationsByPlayer[player.id]?.attendance ?: AttendanceStatus.ABSENT
            val assignedSide = state.participationsByPlayer[player.id]?.teamSide
            AttendancePlayerRow(
                player = player,
                attendance = currentAttendance,
                assignedSide = assignedSide,
                recoveryFactor = state.settings.recoveryRatingFactor,
                onSelectAttendance = { viewModel.setAttendance(player.id, it) }
            )
        }

        // 6. Infirmerie (Injured players)
        if (state.injuredPlayers.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalHospital,
                        contentDescription = null,
                        tint = InjuryRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Infirmerie (${state.injuredPlayers.size}) — exclus du tirage",
                        style = MaterialTheme.typography.titleMedium,
                        color = InjuryRed
                    )
                }
            }

            items(state.injuredPlayers, key = { "inj_${it.id}" }) { injured ->
                Surface(
                    color = InjuryRedContainer.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, InjuryRed.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = injured.displayName,
                                style = MaterialTheme.typography.titleMedium
                            )
                            val note = buildString {
                                if (injured.injuryNote.isNotBlank()) append(injured.injuryNote)
                                if (injured.expectedReturnText.isNotBlank()) {
                                    if (isNotEmpty()) append(" • ")
                                    append("Retour : ${injured.expectedReturnText}")
                                }
                            }
                            if (note.isNotBlank()) {
                                Text(
                                    text = note,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        TextButton(onClick = { viewModel.markRecoveringAndPresent(injured) }) {
                            Text("Reprise + Présent")
                        }
                    }
                }
            }
        }
    }

    if (showSmartPasteDialog) {
        SmartPasteWhatsAppDialog(
            onDismiss = { showSmartPasteDialog = false },
            onParse = { viewModel.parseWhatsAppList(it) },
            onConfirmPlayers = { ids ->
                viewModel.applyImportedPresentPlayers(ids)
                showSmartPasteDialog = false
            }
        )
    }

    if (showGuestDialog) {
        QuickGuestDialog(
            onDismiss = { showGuestDialog = false },
            onSave = { name, pos, skill ->
                viewModel.addQuickGuest(name, pos, skill)
                showGuestDialog = false
            }
        )
    }

    if (showEditMatchDialog) {
        EditMatchDialog(
            match = match,
            settings = state.settings,
            onDismiss = { showEditMatchDialog = false },
            onSave = { iso, loc, maxP, cost, mode, fixed ->
                viewModel.updateMatchSettings(iso, loc, maxP, cost, mode, fixed)
                showEditMatchDialog = false
            },
            onCancelMatch = {
                viewModel.cancelCurrentMatch()
                showEditMatchDialog = false
            }
        )
    }

    if (showCloseMatchDialog) {
        CloseMatchDialog(
            match = match,
            settings = state.settings,
            bluePlayers = state.blueTeam,
            orangePlayers = state.orangeTeam,
            allPresent = state.presentPlayers,
            onDismiss = { showCloseMatchDialog = false },
            onCloseMatch = { bScore, oScore, mvpId, notes, goals, injuredIds, logExpense ->
                viewModel.closeMatch(bScore, oScore, mvpId, notes, goals, injuredIds, logExpense)
                showCloseMatchDialog = false
            }
        )
    }
}

@Composable
private fun MatchHeroCard(
    match: MatchEntity,
    presentCount: Int,
    uncertainCount: Int,
    injuredCount: Int,
    onEditClick: () -> Unit
) {
    val countdown = remember(match.dateTimeIso) {
        SaturdayScheduler.countdownLabel(match.dateTimeIso)
    }
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, StadiumOutline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(
                    text = countdown.ifBlank { "Prochain match" },
                    containerColor = PitchEmeraldContainer,
                    contentColor = PitchEmerald
                )
                IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Modifier le match",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, null, tint = TeamBlueLight, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = SaturdayScheduler.formatFrenchDateTime(match.dateTimeIso),
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, tint = TeamOrangeLight, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = match.location,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusBadge("$presentCount/${match.maxPlayers} Présents", TeamBlueContainer, TeamBlueLight)
                if (uncertainCount > 0) {
                    StatusBadge("$uncertainCount Incertains", WarningAmberContainer, WarningAmber)
                }
                if (injuredCount > 0) {
                    StatusBadge("$injuredCount Blessés", InjuryRedContainer, InjuryRed)
                }
            }
        }
    }
}

@Composable
private fun TeamsComparisonCard(
    settings: AppSettings,
    summary: BalancedTeamsResult,
    unassigned: List<PlayerEntity>,
    selectedSwapId: Long?,
    onPlayerTapForSwap: (Long) -> Unit,
    onClearSwap: () -> Unit,
    onAssignSide: (Long, TeamSide) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, StadiumOutline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Équipes générées",
                    style = MaterialTheme.typography.titleMedium
                )
                val deltaText = String.format(Locale.US, "Écart niveau : %.1f", summary.skillDelta)
                StatusBadge(
                    text = deltaText,
                    containerColor = if (summary.skillDelta <= 1.2) PitchEmeraldContainer else WarningAmberContainer,
                    contentColor = if (summary.skillDelta <= 1.2) PitchEmerald else WarningAmber
                )
            }

            if (selectedSwapId != null) {
                Surface(
                    color = WarningAmberContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SwapHoriz, null, tint = WarningAmber, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Touche un autre joueur pour échanger",
                                style = MaterialTheme.typography.bodyMedium,
                                color = WarningAmber
                            )
                        }
                        TextButton(onClick = onClearSwap) {
                            Text("Annuler")
                        }
                    }
                }
            } else {
                Text(
                    text = "Astuce : touche 2 joueurs pour les échanger manuellement.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TeamColumnBox(
                    title = settings.blueTeamName,
                    side = TeamSide.BLEU,
                    players = summary.blueTeam,
                    avgSkill = summary.blueAvgSkill,
                    ratings = summary.effectiveRatings,
                    selectedSwapId = selectedSwapId,
                    onPlayerTap = onPlayerTapForSwap,
                    modifier = Modifier.weight(1f)
                )
                TeamColumnBox(
                    title = settings.orangeTeamName,
                    side = TeamSide.ORANGE,
                    players = summary.orangeTeam,
                    avgSkill = summary.orangeAvgSkill,
                    ratings = summary.effectiveRatings,
                    selectedSwapId = selectedSwapId,
                    onPlayerTap = onPlayerTapForSwap,
                    modifier = Modifier.weight(1f)
                )
            }

            if (unassigned.isNotEmpty()) {
                HorizontalDivider(color = StadiumOutline)
                Text(
                    text = "Présents non encore assignés :",
                    style = MaterialTheme.typography.labelMedium,
                    color = WarningAmber
                )
                unassigned.forEach { p ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(p.displayName, style = MaterialTheme.typography.bodyLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { onAssignSide(p.id, TeamSide.BLEU) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("→ Bleu", color = TeamBlueLight)
                            }
                            OutlinedButton(
                                onClick = { onAssignSide(p.id, TeamSide.ORANGE) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("→ Orange", color = TeamOrangeLight)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamColumnBox(
    title: String,
    side: TeamSide,
    players: List<PlayerEntity>,
    avgSkill: Double,
    ratings: Map<Long, Double>,
    selectedSwapId: Long?,
    onPlayerTap: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val solid = teamSolidColor(side)
    val accent = teamAccentColor(side)
    val bg = teamContainerColor(side)

    Surface(
        modifier = modifier,
        color = bg.copy(alpha = 0.55f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, solid.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = String.format(Locale.US, "%.1f", avgSkill),
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
            }
            HorizontalDivider(color = solid.copy(alpha = 0.35f))

            players.forEach { p ->
                val isSelected = p.id == selectedSwapId
                Surface(
                    color = if (isSelected) WarningAmberContainer else StadiumCardAlt,
                    shape = RoundedCornerShape(8.dp),
                    border = if (isSelected) BorderStroke(1.5.dp, WarningAmber) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlayerTap(p.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = p.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = p.position.shortLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = String.format(Locale.US, "%.1f", ratings[p.id] ?: p.skillRating),
                            style = MaterialTheme.typography.labelMedium,
                            color = accent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendancePlayerRow(
    player: PlayerEntity,
    attendance: AttendanceStatus,
    assignedSide: TeamSide?,
    recoveryFactor: Double,
    onSelectAttendance: (AttendanceStatus) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            when (assignedSide) {
                TeamSide.BLEU -> TeamBlue.copy(alpha = 0.5f)
                TeamSide.ORANGE -> TeamOrange.copy(alpha = 0.5f)
                null -> StadiumOutline
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (assignedSide != null) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(teamSolidColor(assignedSide), RoundedCornerShape(50))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Column {
                        Text(
                            text = player.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = buildString {
                                append(player.position.label)
                                if (player.status == PlayerStatus.EN_REPRISE) append(" • Reprise")
                                if (player.isGuest) append(" • Invité")
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                SkillBadge(
                    rating = player.effectiveRating(recoveryFactor),
                    isRecovering = player.status == PlayerStatus.EN_REPRISE
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AttendanceToggleChip(
                    label = "Présent",
                    selected = attendance == AttendanceStatus.PRESENT,
                    selectedBg = PitchEmeraldContainer,
                    selectedColor = PitchEmerald,
                    onClick = { onSelectAttendance(AttendanceStatus.PRESENT) },
                    modifier = Modifier.weight(1f)
                )
                AttendanceToggleChip(
                    label = "Incertain",
                    selected = attendance == AttendanceStatus.UNCERTAIN,
                    selectedBg = WarningAmberContainer,
                    selectedColor = WarningAmber,
                    onClick = { onSelectAttendance(AttendanceStatus.UNCERTAIN) },
                    modifier = Modifier.weight(1f)
                )
                AttendanceToggleChip(
                    label = "Absent",
                    selected = attendance == AttendanceStatus.ABSENT,
                    selectedBg = StadiumCardAlt,
                    selectedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { onSelectAttendance(AttendanceStatus.ABSENT) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AttendanceToggleChip(
    label: String,
    selected: Boolean,
    selectedBg: Color,
    selectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        label = {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = selectedBg,
            selectedLabelColor = selectedColor
        )
    )
}

@Composable
private fun SmartPasteWhatsAppDialog(
    onDismiss: () -> Unit,
    onParse: (String) -> List<PlayerEntity>,
    onConfirmPlayers: (List<Long>) -> Unit
) {
    var rawText by remember { mutableStateOf("") }
    val matched = remember(rawText) {
        if (rawText.isBlank()) emptyList() else onParse(rawText)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Importer depuis WhatsApp") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Colle la liste copiée depuis votre groupe WhatsApp. Les joueurs reconnus seront cochés Présents.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    label = { Text("Message WhatsApp") },
                    placeholder = { Text("Ex: 1. Karim\n2. Badr\n3. Yassine...") },
                    minLines = 4,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
                if (rawText.isNotBlank()) {
                    Text(
                        text = if (matched.isEmpty()) {
                            "Aucun joueur reconnu pour l'instant."
                        } else {
                            "Reconnus (${matched.size}) : ${matched.joinToString(", ") { it.displayName }}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (matched.isEmpty()) WarningAmber else PitchEmerald
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmPlayers(matched.map { it.id }) },
                enabled = matched.isNotEmpty()
            ) {
                Text("Marquer présents (${matched.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
private fun QuickGuestDialog(
    onDismiss: () -> Unit,
    onSave: (String, PlayerPosition, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var position by remember { mutableStateOf(PlayerPosition.POLYVALENT) }
    var skill by remember { mutableStateOf(6f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter un invité présent") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Prénom / Surnom") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Poste : ${position.label}", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PlayerPosition.entries.forEach { pos ->
                        FilterChip(
                            selected = position == pos,
                            onClick = { position = pos },
                            label = { Text(pos.shortLabel) }
                        )
                    }
                }
                Text(
                    text = "Niveau estimé : ${String.format(Locale.US, "%.1f", skill)} / 10",
                    style = MaterialTheme.typography.labelMedium
                )
                Slider(
                    value = skill,
                    onValueChange = { skill = (it * 2f).roundToInt() / 2f },
                    valueRange = 1f..10f
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, position, skill.toDouble()) },
                enabled = name.isNotBlank()
            ) {
                Text("Ajouter & Présent")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
private fun EditMatchDialog(
    match: MatchEntity,
    settings: AppSettings,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, Double, FeeMode, Double) -> Unit,
    onCancelMatch: () -> Unit
) {
    val parsedDt = remember(match.dateTimeIso) {
        SaturdayScheduler.parseIso(match.dateTimeIso) ?: LocalDateTime.now()
    }
    var dateText by remember { mutableStateOf(parsedDt.toLocalDate().toString()) }
    var timeText by remember {
        mutableStateOf(
            String.format(Locale.US, "%02d:%02d", parsedDt.hour, parsedDt.minute)
        )
    }
    var location by remember { mutableStateOf(match.location) }
    var maxPlayersText by remember { mutableStateOf(match.maxPlayers.toString()) }
    var pitchCostText by remember { mutableStateOf(match.pitchCost.toInt().toString()) }
    var fixedFeeText by remember { mutableStateOf(match.fixedFeePerPlayer.toInt().toString()) }
    var feeMode by remember { mutableStateOf(match.feeMode) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier ce match") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dateText,
                        onValueChange = { dateText = it },
                        label = { Text("Date (AAAA-MM-JJ)") },
                        singleLine = true,
                        modifier = Modifier.weight(1.3f)
                    )
                    OutlinedTextField(
                        value = timeText,
                        onValueChange = { timeText = it },
                        label = { Text("Heure (HH:MM)") },
                        singleLine = true,
                        modifier = Modifier.weight(0.9f)
                    )
                }
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Terrain / Lieu") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = maxPlayersText,
                    onValueChange = { maxPlayersText = it.filter(Char::isDigit) },
                    label = { Text("Places max") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Mode de cotisation", style = MaterialTheme.typography.labelMedium)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FeeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = feeMode == mode,
                            onClick = { feeMode = mode },
                            label = { Text(mode.label) }
                        )
                    }
                }
                if (feeMode == FeeMode.SPLIT_PITCH) {
                    OutlinedTextField(
                        value = pitchCostText,
                        onValueChange = { pitchCostText = it },
                        label = { Text("Coût du terrain (${settings.currency})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (feeMode == FeeMode.FIXED_PER_MATCH) {
                    OutlinedTextField(
                        value = fixedFeeText,
                        onValueChange = { fixedFeeText = it },
                        label = { Text("Tarif par joueur (${settings.currency})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                TextButton(
                    onClick = onCancelMatch,
                    colors = ButtonDefaults.textButtonColors(contentColor = InjuryRed)
                ) {
                    Text("Annuler ce samedi et passer au suivant")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val safeIso = runCatching {
                        val parts = timeText.split(":")
                        val h = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 22
                        val m = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
                        val d = java.time.LocalDate.parse(dateText.trim())
                        SaturdayScheduler.toIso(d.atTime(h, m))
                    }.getOrDefault(match.dateTimeIso)
                    onSave(
                        safeIso,
                        location,
                        maxPlayersText.toIntOrNull() ?: match.maxPlayers,
                        pitchCostText.toDoubleOrNull() ?: match.pitchCost,
                        feeMode,
                        fixedFeeText.toDoubleOrNull() ?: match.fixedFeePerPlayer
                    )
                }
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        }
    )
}

@Composable
private fun CloseMatchDialog(
    match: MatchEntity,
    settings: AppSettings,
    bluePlayers: List<PlayerEntity>,
    orangePlayers: List<PlayerEntity>,
    allPresent: List<PlayerEntity>,
    onDismiss: () -> Unit,
    onCloseMatch: (Int, Int, Long?, String, Map<Long, Int>, Set<Long>, Boolean) -> Unit
) {
    var blueScore by remember { mutableIntStateOf(0) }
    var orangeScore by remember { mutableIntStateOf(0) }
    var mvpId by remember { mutableStateOf<Long?>(null) }
    var notes by remember { mutableStateOf("") }
    var logPitchExpense by remember { mutableStateOf(match.pitchCost > 0.0) }
    val goalsMap = remember { mutableStateMapOf<Long, Int>() }
    val injuredIds = remember { mutableStateMapOf<Long, Boolean>() }

    val participants = remember(bluePlayers, orangePlayers, allPresent) {
        (bluePlayers + orangePlayers).ifEmpty { allPresent }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clôturer & Enregistrer le score") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Score Steppers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ScoreStepper(
                        label = settings.blueTeamName,
                        color = TeamBlueLight,
                        value = blueScore,
                        onChange = { blueScore = it }
                    )
                    Text("-", style = MaterialTheme.typography.headlineSmall)
                    ScoreStepper(
                        label = settings.orangeTeamName,
                        color = TeamOrangeLight,
                        value = orangeScore,
                        onChange = { orangeScore = it }
                    )
                }

                HorizontalDivider(color = StadiumOutline)

                Text("Buteurs, MVP & Blessures (optionnel)", style = MaterialTheme.typography.labelMedium)
                participants.forEach { p ->
                    val g = goalsMap[p.id] ?: 0
                    val isMvp = mvpId == p.id
                    val isInjured = injuredIds[p.id] == true
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = p.displayName,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = { goalsMap[p.id] = g + 1 },
                                contentPadding = PaddingValues(horizontal = 6.dp)
                            ) {
                                Text(if (g > 0) "⚽ $g" else "+But")
                            }
                            FilterChip(
                                selected = isMvp,
                                onClick = { mvpId = if (isMvp) null else p.id },
                                label = { Text("MVP") }
                            )
                            IconButton(
                                onClick = { injuredIds[p.id] = !isInjured },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalHospital,
                                    contentDescription = "Blessé pendant le match",
                                    tint = if (isInjured) InjuryRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Note du match (optionnel)") },
                    placeholder = { Text("Ex: Match très serré, super ambiance") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (match.pitchCost > 0.0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { logPitchExpense = !logPitchExpense }
                    ) {
                        Checkbox(
                            checked = logPitchExpense,
                            onCheckedChange = { logPitchExpense = it }
                        )
                        Text(
                            text = "Enregistrer dépense terrain (-${FeeCalculator.formatMoney(match.pitchCost, settings.currency)})",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val injuredSet = injuredIds.filterValues { it }.keys
                    onCloseMatch(
                        blueScore,
                        orangeScore,
                        mvpId,
                        notes,
                        goalsMap.toMap(),
                        injuredSet,
                        logPitchExpense
                    )
                }
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Valider & Archiver")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
private fun ScoreStepper(
    label: String,
    color: Color,
    value: Int,
    onChange: (Int) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            maxLines = 1
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (value > 0) onChange(value - 1) }) {
                Text("-", style = MaterialTheme.typography.headlineSmall)
            }
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { onChange(value + 1) }) {
                Icon(Icons.Default.Add, contentDescription = "+1")
            }
        }
    }
}

package com.foot22.samedi.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.foot22.samedi.data.local.AppSettings
import com.foot22.samedi.data.local.MatchParticipationEntity
import com.foot22.samedi.data.local.PlayerEntity
import com.foot22.samedi.data.model.MatchStatus
import com.foot22.samedi.domain.MatchOutcome
import com.foot22.samedi.domain.PlayerSeasonStats
import com.foot22.samedi.domain.SaturdayScheduler
import com.foot22.samedi.domain.WhatsAppFormatter
import com.foot22.samedi.ui.components.StatusBadge
import com.foot22.samedi.ui.theme.InjuryRed
import com.foot22.samedi.ui.theme.PitchEmerald
import com.foot22.samedi.ui.theme.PitchEmeraldContainer
import com.foot22.samedi.ui.theme.StadiumOutline
import com.foot22.samedi.ui.theme.TeamBlueContainer
import com.foot22.samedi.ui.theme.TeamBlueLight
import com.foot22.samedi.ui.theme.TeamOrangeContainer
import com.foot22.samedi.ui.theme.TeamOrangeLight
import com.foot22.samedi.ui.theme.WarningAmber
import com.foot22.samedi.ui.theme.WarningAmberContainer
import com.foot22.samedi.util.MatchCardImageGenerator
import com.foot22.samedi.util.WhatsAppShareHelper
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var editingItem by remember { mutableStateOf<HistoricalMatchItem?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Matchs (${state.matches.size})") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Classement") }
            )
        }

        if (selectedTab == 0) {
            MatchesHistoryTab(
                items = state.matches,
                settings = state.settings,
                onEditMatch = { editingItem = it }
            )
        } else {
            LeaderboardTab(stats = state.leaderboard)
        }
    }

    editingItem?.let { item ->
        EditPastMatchDialog(
            item = item,
            settings = state.settings,
            onDismiss = { editingItem = null },
            onSave = { bScore, oScore, mvpId, notes, goals ->
                viewModel.updatePastMatchScore(
                    matchId = item.match.id,
                    blueScore = bScore,
                    orangeScore = oScore,
                    mvpPlayerId = mvpId,
                    notes = notes,
                    goalsByPlayer = goals
                )
                editingItem = null
            }
        )
    }
}

@Composable
private fun MatchesHistoryTab(
    items: List<HistoricalMatchItem>,
    settings: AppSettings,
    onEditMatch: (HistoricalMatchItem) -> Unit
) {
    val context = LocalContext.current

    if (items.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Aucun match clôturé pour le moment.\nDès que tu clôtures un samedi soir, le score et les 2 équipes restent archivés ici.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items, key = { it.match.id }) { item ->
            var expanded by remember { mutableStateOf(false) }
            var showShareMenu by remember { mutableStateOf(false) }
            val match = item.match
            val isCancelled = match.status == MatchStatus.CANCELLED
            val bScore = match.blueScore ?: 0
            val oScore = match.orangeScore ?: 0

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, StadiumOutline),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { if (!isCancelled) expanded = !expanded }
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = SaturdayScheduler.formatFrenchDateTime(match.dateTimeIso),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = match.location,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!isCancelled) {
                            Row {
                                Box {
                                    IconButton(onClick = { showShareMenu = true }) {
                                        Icon(Icons.Default.Share, contentDescription = "Partager résultat")
                                    }
                                    DropdownMenu(
                                        expanded = showShareMenu,
                                        onDismissRequest = { showShareMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Résultat sur WhatsApp (texte)") },
                                            leadingIcon = { Icon(Icons.Default.Share, null) },
                                            onClick = {
                                                showShareMenu = false
                                                val msg = WhatsAppFormatter.formatMatchResult(
                                                    match = match,
                                                    bluePlayers = item.bluePlayers,
                                                    orangePlayers = item.orangePlayers,
                                                    mvp = item.mvp,
                                                    settings = settings
                                                )
                                                WhatsAppShareHelper.shareTextToWhatsApp(context, msg)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Carte résultat (image PNG)") },
                                            leadingIcon = { Icon(Icons.Default.Image, null) },
                                            onClick = {
                                                showShareMenu = false
                                                val uri = MatchCardImageGenerator.generateLineupOrScoreCard(
                                                    context = context,
                                                    match = match,
                                                    bluePlayers = item.bluePlayers.map { it.first },
                                                    orangePlayers = item.orangePlayers.map { it.first },
                                                    settings = settings,
                                                    blueAvg = item.blueAvgSnapshot,
                                                    orangeAvg = item.orangeAvgSnapshot
                                                )
                                                val caption = "🏁 ${settings.blueTeamName} $bScore - $oScore ${settings.orangeTeamName}"
                                                WhatsAppShareHelper.shareImageAndTextToWhatsApp(context, uri, caption)
                                            }
                                        )
                                    }
                                }
                                IconButton(onClick = { onEditMatch(item) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Corriger score")
                                }
                            }
                        }
                    }

                    if (isCancelled) {
                        StatusBadge("Match annulé", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        // Score Pill Banner
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = TeamBlueContainer,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "${settings.blueTeamName}  $bScore",
                                    color = TeamBlueLight,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = " - ",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                            Surface(
                                color = TeamOrangeContainer,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "$oScore  ${settings.orangeTeamName}",
                                    color = TeamOrangeLight,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Voir les équipes",
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }

                        if (item.mvp != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.EmojiEvents, null, tint = WarningAmber, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Homme du match : ${item.mvp.displayName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = WarningAmber
                                )
                            }
                        }

                        if (expanded) {
                            HorizontalDivider(color = StadiumOutline)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                FrozenTeamRoster(
                                    title = settings.blueTeamName,
                                    avg = item.blueAvgSnapshot,
                                    accent = TeamBlueLight,
                                    players = item.bluePlayers,
                                    modifier = Modifier.weight(1f)
                                )
                                FrozenTeamRoster(
                                    title = settings.orangeTeamName,
                                    avg = item.orangeAvgSnapshot,
                                    accent = TeamOrangeLight,
                                    players = item.orangePlayers,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (match.notes.isNotBlank()) {
                                Text(
                                    text = "Note : ${match.notes}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FrozenTeamRoster(
    title: String,
    avg: Double,
    accent: Color,
    players: List<Pair<PlayerEntity, MatchParticipationEntity>>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "$title (${String.format(Locale.US, "%.1f", avg)})",
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            fontWeight = FontWeight.Bold
        )
        players.forEach { (player, part) ->
            val tags = buildString {
                if (part.goals > 0) append(" ⚽${part.goals}")
                if (part.injuredInMatch) append(" 🩹")
            }
            Text(
                text = "• ${player.displayName}$tags",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LeaderboardTab(stats: List<PlayerSeasonStats>) {
    if (stats.isEmpty() || stats.all { it.matchesPlayed == 0 }) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Les statistiques apparaîtront après le premier match terminé.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(stats.filter { it.matchesPlayed > 0 }, key = { _, s -> s.player.id }) { index, row ->
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, StadiumOutline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "#${index + 1}",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (index < 3) WarningAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(34.dp)
                        )
                        Column {
                            Text(row.player.displayName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${row.matchesPlayed}M • ${row.wins}V ${row.draws}N ${row.losses}D" +
                                    (if (row.goals > 0) " • ⚽ ${row.goals}" else "") +
                                    (if (row.mvpCount > 0) " • ⭐ ${row.mvpCount}" else ""),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        StatusBadge(
                            text = "${row.winRatePercent}% V",
                            containerColor = if (row.winRatePercent >= 50) PitchEmeraldContainer else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (row.winRatePercent >= 50) PitchEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            row.recentOutcomes.forEach { outcome ->
                                val c = when (outcome) {
                                    MatchOutcome.WIN -> PitchEmerald
                                    MatchOutcome.DRAW -> WarningAmber
                                    MatchOutcome.LOSS -> InjuryRed
                                }
                                Surface(
                                    color = c,
                                    shape = CircleShape,
                                    modifier = Modifier.size(7.dp)
                                ) {}
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditPastMatchDialog(
    item: HistoricalMatchItem,
    settings: AppSettings,
    onDismiss: () -> Unit,
    onSave: (Int, Int, Long?, String, Map<Long, Int>) -> Unit
) {
    var blueScore by remember { mutableIntStateOf(item.match.blueScore ?: 0) }
    var orangeScore by remember { mutableIntStateOf(item.match.orangeScore ?: 0) }
    var mvpId by remember { mutableStateOf(item.match.mvpPlayerId) }
    var notes by remember { mutableStateOf(item.match.notes) }
    val goalsMap = remember {
        mutableStateMapOf<Long, Int>().apply {
            (item.bluePlayers + item.orangePlayers).forEach { (p, part) ->
                put(p.id, part.goals)
            }
        }
    }

    val allParticipants = item.bluePlayers + item.orangePlayers

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier le match") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(settings.blueTeamName, color = TeamBlueLight, style = MaterialTheme.typography.labelMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (blueScore > 0) blueScore-- }) { Text("-") }
                            Text(blueScore.toString(), style = MaterialTheme.typography.titleLarge, color = TeamBlueLight)
                            IconButton(onClick = { blueScore++ }) { Icon(Icons.Default.Add, null) }
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(settings.orangeTeamName, color = TeamOrangeLight, style = MaterialTheme.typography.labelMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (orangeScore > 0) orangeScore-- }) { Text("-") }
                            Text(orangeScore.toString(), style = MaterialTheme.typography.titleLarge, color = TeamOrangeLight)
                            IconButton(onClick = { orangeScore++ }) { Icon(Icons.Default.Add, null) }
                        }
                    }
                }

                allParticipants.forEach { (p, _) ->
                    val g = goalsMap[p.id] ?: 0
                    val isMvp = mvpId == p.id
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(p.displayName, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { goalsMap[p.id] = g + 1 }) {
                                Text(if (g > 0) "⚽ $g" else "+But")
                            }
                            if (g > 0) {
                                TextButton(onClick = { goalsMap[p.id] = (g - 1).coerceAtLeast(0) }) {
                                    Text("-1")
                                }
                            }
                            FilterChip(
                                selected = isMvp,
                                onClick = { mvpId = if (isMvp) null else p.id },
                                label = { Text("MVP") }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Note du match") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(blueScore, orangeScore, mvpId, notes, goalsMap.toMap()) }) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

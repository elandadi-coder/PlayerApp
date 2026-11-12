package com.foot22.samedi.ui.players

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.foot22.samedi.data.local.PlayerEntity
import com.foot22.samedi.data.model.PlayerPosition
import com.foot22.samedi.data.model.PlayerStatus
import com.foot22.samedi.domain.WhatsAppFormatter
import com.foot22.samedi.ui.components.PlayerStatusBadge
import com.foot22.samedi.ui.components.SkillBadge
import com.foot22.samedi.ui.components.SummaryMetricCard
import com.foot22.samedi.ui.theme.InjuryRed
import com.foot22.samedi.ui.theme.PitchEmerald
import com.foot22.samedi.ui.theme.StadiumOutline
import com.foot22.samedi.ui.theme.TeamBlueLight
import com.foot22.samedi.ui.theme.WarningAmber
import com.foot22.samedi.util.WhatsAppShareHelper
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun PlayersScreen(
    viewModel: PlayersViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var editingPlayer by remember { mutableStateOf<PlayerEntity?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingPlayer = null
                    showEditDialog = true
                },
                containerColor = PitchEmerald,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nouveau joueur")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryMetricCard(
                        title = "Actifs",
                        value = state.activeCount.toString(),
                        accentColor = PitchEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryMetricCard(
                        title = "En reprise",
                        value = state.recoveringCount.toString(),
                        accentColor = WarningAmber,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryMetricCard(
                        title = "Infirmerie",
                        value = state.injuredCount.toString(),
                        accentColor = InjuryRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.setSearch(it) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    placeholder = { Text("Rechercher un joueur ou un poste...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlayerFilter.entries.forEach { f ->
                        FilterChip(
                            selected = state.filter == f,
                            onClick = { viewModel.setFilter(f) },
                            label = { Text(f.label) }
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    PlayerSort.entries.forEach { s ->
                        FilterChip(
                            selected = state.sort == s,
                            onClick = { viewModel.setSort(s) },
                            label = { Text(s.label) }
                        )
                    }
                }
            }

            if (state.players.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, StadiumOutline)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Aucun joueur dans le groupe", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Appuie sur + pour créer tes joueurs ou charge une liste de démonstration.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(onClick = { viewModel.seedDemoPlayers() }) {
                                Text("Charger 14 joueurs d'exemple")
                            }
                        }
                    }
                }
            } else if (state.filteredPlayers.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Aucun joueur ne correspond à ce filtre.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(state.filteredPlayers, key = { it.id }) { player ->
                PlayerProfileCard(
                    player = player,
                    recoveryFactor = state.settings.recoveryRatingFactor,
                    onClick = {
                        editingPlayer = player
                        showEditDialog = true
                    },
                    onWhatsAppClick = {
                        val msg = if (player.status == PlayerStatus.BLESSE) {
                            WhatsAppFormatter.formatInjuryCheckInMessage(player)
                        } else {
                            "Salut ${player.displayName}, rdv samedi à 22h00 pour le foot ! ⚽"
                        }
                        WhatsAppShareHelper.openPrivateWhatsAppChat(context, player.phone, msg)
                    }
                )
            }
        }
    }

    if (showEditDialog) {
        PlayerEditorDialog(
            initial = editingPlayer,
            onDismiss = { showEditDialog = false },
            onSave = { saved ->
                viewModel.savePlayer(saved)
                showEditDialog = false
            },
            onDelete = editingPlayer?.let { target ->
                {
                    viewModel.deletePlayer(target)
                    showEditDialog = false
                }
            }
        )
    }
}

@Composable
private fun PlayerProfileCard(
    player: PlayerEntity,
    recoveryFactor: Double,
    onClick: () -> Unit,
    onWhatsAppClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            if (player.status == PlayerStatus.BLESSE) InjuryRed.copy(alpha = 0.45f) else StadiumOutline
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = player.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    PlayerStatusBadge(player.status)
                }

                Text(
                    text = buildString {
                        append(player.position.label)
                        if (player.useDetailedRating) {
                            append(" • Tech ${player.technique} / Car ${player.stamina} / Déf ${player.defense} / Fin ${player.finishing}")
                        }
                        if (player.isGuest) append(" • Invité")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (player.status == PlayerStatus.BLESSE || player.status == PlayerStatus.EN_REPRISE) {
                    val info = listOf(player.injuryNote, player.expectedReturnText)
                        .filter { it.isNotBlank() }
                        .joinToString(" • Retour : ")
                    if (info.isNotBlank()) {
                        Text(
                            text = info,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (player.status == PlayerStatus.BLESSE) InjuryRed else WarningAmber
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                SkillBadge(
                    rating = player.effectiveRating(recoveryFactor),
                    isRecovering = player.status == PlayerStatus.EN_REPRISE
                )
                IconButton(onClick = onWhatsAppClick) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "Contacter sur WhatsApp",
                        tint = TeamBlueLight,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerEditorDialog(
    initial: PlayerEntity?,
    onDismiss: () -> Unit,
    onSave: (PlayerEntity) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var nickname by remember { mutableStateOf(initial?.nickname ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    var position by remember { mutableStateOf(initial?.position ?: PlayerPosition.POLYVALENT) }
    var useDetailed by remember { mutableStateOf(initial?.useDetailedRating ?: false) }
    var simpleSkill by remember { mutableFloatStateOf((initial?.skillRating ?: 6.0).toFloat()) }
    var technique by remember { mutableIntStateOf(initial?.technique ?: 6) }
    var stamina by remember { mutableIntStateOf(initial?.stamina ?: 6) }
    var defense by remember { mutableIntStateOf(initial?.defense ?: 6) }
    var finishing by remember { mutableIntStateOf(initial?.finishing ?: 6) }
    var status by remember { mutableStateOf(initial?.status ?: PlayerStatus.ACTIF) }
    var injuryNote by remember { mutableStateOf(initial?.injuryNote ?: "") }
    var expectedReturn by remember { mutableStateOf(initial?.expectedReturnText ?: "") }
    var isGuest by remember { mutableStateOf(initial?.isGuest ?: false) }

    val computedSkill = if (useDetailed) {
        (technique + stamina + defense + finishing) / 4.0
    } else {
        simpleSkill.toDouble()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nouveau joueur" else "Modifier ${initial.displayName}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom complet *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("Surnom (affiché sur les compos)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Téléphone WhatsApp (ex: 2126...)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Poste principal", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PlayerPosition.entries.forEach { pos ->
                        FilterChip(
                            selected = position == pos,
                            onClick = { position = pos },
                            label = { Text(pos.label) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Niveau : ${String.format(Locale.US, "%.1f", computedSkill)} / 10",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("4 critères", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(checked = useDetailed, onCheckedChange = { useDetailed = it })
                    }
                }

                if (!useDetailed) {
                    Slider(
                        value = simpleSkill,
                        onValueChange = { simpleSkill = (it * 2f).roundToInt() / 2f },
                        valueRange = 1f..10f
                    )
                } else {
                    CriterionSlider("Technique", technique) { technique = it }
                    CriterionSlider("Cardio / Physique", stamina) { stamina = it }
                    CriterionSlider("Défense", defense) { defense = it }
                    CriterionSlider("Finition / Tir", finishing) { finishing = it }
                }

                Text("Statut / Infirmerie", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PlayerStatus.entries.forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(st.label) }
                        )
                    }
                }

                if (status == PlayerStatus.BLESSE || status == PlayerStatus.EN_REPRISE) {
                    OutlinedTextField(
                        value = injuryNote,
                        onValueChange = { injuryNote = it },
                        label = { Text("Motif / Zone blessée") },
                        placeholder = { Text("Ex: Entorse cheville droite") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = expectedReturn,
                        onValueChange = { expectedReturn = it },
                        label = { Text("Retour estimé") },
                        placeholder = { Text("Ex: Samedi prochain / 2 semaines") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { isGuest = !isGuest }
                ) {
                    Checkbox(checked = isGuest, onCheckedChange = { isGuest = it })
                    Text("Joueur invité ponctuel", style = MaterialTheme.typography.bodyMedium)
                }

                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = InjuryRed)
                    ) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Supprimer ce joueur")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val entity = PlayerEntity(
                        id = initial?.id ?: 0L,
                        name = name.trim(),
                        nickname = nickname.trim(),
                        phone = phone.trim(),
                        position = position,
                        skillRating = computedSkill,
                        technique = technique,
                        stamina = stamina,
                        defense = defense,
                        finishing = finishing,
                        useDetailedRating = useDetailed,
                        status = status,
                        injuryNote = injuryNote.trim(),
                        injuryDateEpoch = initial?.injuryDateEpoch,
                        expectedReturnText = expectedReturn.trim(),
                        isGuest = isGuest,
                        createdAtEpoch = initial?.createdAtEpoch ?: System.currentTimeMillis()
                    )
                    onSave(entity)
                },
                enabled = name.isNotBlank()
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
private fun CriterionSlider(label: String, value: Int, onChange: (Int) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text("$value / 10", style = MaterialTheme.typography.labelMedium)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt().coerceIn(1, 10)) },
            valueRange = 1f..10f,
            steps = 8
        )
    }
}

package com.foot22.samedi.ui.fees

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.foot22.samedi.data.local.CashTransactionEntity
import com.foot22.samedi.data.model.FeeMode
import com.foot22.samedi.data.model.PaymentMethod
import com.foot22.samedi.data.model.TransactionType
import com.foot22.samedi.domain.FeeCalculator
import com.foot22.samedi.domain.SaturdayScheduler
import com.foot22.samedi.domain.WhatsAppFormatter
import com.foot22.samedi.ui.components.StatusBadge
import com.foot22.samedi.ui.components.SummaryMetricCard
import com.foot22.samedi.ui.theme.InjuryRed
import com.foot22.samedi.ui.theme.InjuryRedContainer
import com.foot22.samedi.ui.theme.PitchEmerald
import com.foot22.samedi.ui.theme.PitchEmeraldContainer
import com.foot22.samedi.ui.theme.StadiumOutline
import com.foot22.samedi.ui.theme.TeamBlueLight
import com.foot22.samedi.ui.theme.WarningAmber
import com.foot22.samedi.ui.theme.WarningAmberContainer
import com.foot22.samedi.util.WhatsAppShareHelper
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun FeesScreen(
    viewModel: FeesViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val currency = state.settings.currency

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddTxDialog by remember { mutableStateOf(false) }
    var editingFeeRow by remember { mutableStateOf<MatchFeeParticipantRow?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Treasury Summary
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryMetricCard(
                title = "Solde Caisse",
                value = FeeCalculator.formatMoney(state.treasury.balance, currency),
                accentColor = if (state.treasury.balance >= 0) PitchEmerald else InjuryRed,
                modifier = Modifier.weight(1f)
            )
            SummaryMetricCard(
                title = "À encaisser",
                value = FeeCalculator.formatMoney(state.treasury.totalUnpaidAcrossPlayers, currency),
                accentColor = if (state.treasury.totalUnpaidAcrossPlayers > 0) WarningAmber else PitchEmerald,
                modifier = Modifier.weight(1f)
            )
        }

        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Match actuel") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Dettes (${state.treasury.playerDebts.size})") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Caisse") })
        }

        when (selectedTab) {
            0 -> CurrentMatchFeesTab(
                state = state,
                onTogglePaid = { matchId, playerId ->
                    viewModel.togglePaid(matchId, playerId, PaymentMethod.CASH)
                },
                onEditRow = { editingFeeRow = it }
            )
            1 -> DebtsTab(
                state = state,
                onShareGroupDebts = {
                    val msg = WhatsAppFormatter.formatDebtsReminder(
                        debts = state.treasury.playerDebts,
                        currency = currency
                    )
                    WhatsAppShareHelper.shareTextToWhatsApp(context, msg)
                },
                onPrivateReminder = { debt ->
                    val msg = WhatsAppFormatter.formatPrivatePaymentReminder(
                        player = debt.player,
                        amountDue = debt.unpaidBalance,
                        currency = currency
                    )
                    WhatsAppShareHelper.openPrivateWhatsAppChat(context, debt.player.phone, msg)
                }
            )
            2 -> CashLedgerTab(
                transactions = state.treasury,
                ledger = state.transactions,
                currency = currency,
                onAddClick = { showAddTxDialog = true },
                onDeleteTx = { viewModel.deleteTransaction(it) }
            )
        }
    }

    if (showAddTxDialog) {
        AddTransactionDialog(
            currency = currency,
            onDismiss = { showAddTxDialog = false },
            onSave = { type, amount, label, method ->
                viewModel.addManualTransaction(type, amount, label, method)
                showAddTxDialog = false
            }
        )
    }

    editingFeeRow?.let { row ->
        val matchId = state.upcomingMatch?.id
        if (matchId != null) {
            EditPlayerFeeDialog(
                row = row,
                currency = currency,
                onDismiss = { editingFeeRow = null },
                onTogglePaidWithMethod = { method ->
                    viewModel.togglePaid(matchId, row.player.id, method)
                    editingFeeRow = null
                },
                onToggleExempt = {
                    viewModel.toggleExempt(matchId, row.player.id, row.participation.feeExempt)
                    editingFeeRow = null
                },
                onCustomAmount = { custom ->
                    viewModel.overridePlayerFee(matchId, row.player.id, custom)
                    editingFeeRow = null
                }
            )
        }
    }
}

@Composable
private fun CurrentMatchFeesTab(
    state: FeesUiState,
    onTogglePaid: (Long, Long) -> Unit,
    onEditRow: (MatchFeeParticipantRow) -> Unit
) {
    val match = state.upcomingMatch
    val currency = state.settings.currency

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (match != null) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, StadiumOutline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = SaturdayScheduler.formatFrenchDateTime(match.dateTimeIso),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Mode : ${match.feeMode.label}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (match.feeMode == FeeMode.SPLIT_PITCH) {
                            Text(
                                text = "Terrain : ${FeeCalculator.formatMoney(match.pitchCost, currency)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TeamBlueLight
                            )
                        } else if (match.feeMode == FeeMode.MONTHLY) {
                            Text(
                                text = "Forfait mensuel : ${FeeCalculator.formatMoney(state.settings.monthlyFeeAmount, currency)} / mois (encaisser via Journal Caisse)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TeamBlueLight
                            )
                        }
                    }
                }
            }
        }

        if (state.monthlyFreezePlayers.isNotEmpty() && match?.feeMode == FeeMode.MONTHLY) {
            item {
                Surface(
                    color = InjuryRedContainer.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, InjuryRed.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocalHospital, null, tint = InjuryRed, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cotisation gelée (blessure) : ${state.monthlyFreezePlayers.joinToString(", ") { it.displayName }}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        if (state.currentMatchRows.isEmpty()) {
            item {
                Text(
                    text = "Aucun joueur marqué Présent sur le prochain match.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (match != null) {
            items(state.currentMatchRows, key = { it.player.id }) { row ->
                val part = row.participation
                val amountLabel = when {
                    part.feeExempt -> "Exonéré"
                    part.feeDue <= 0.0 -> "0 $currency"
                    else -> FeeCalculator.formatMoney(part.feeDue, currency)
                }

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        1.dp,
                        if (part.isPaid) PitchEmerald.copy(alpha = 0.45f) else StadiumOutline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditRow(row) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(row.player.displayName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "$amountLabel${if (part.isPaid) " • ${part.paymentMethod.label}" else ""}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!part.feeExempt && part.feeDue > 0.0) {
                            FilterChip(
                                selected = part.isPaid,
                                onClick = { onTogglePaid(match.id, row.player.id) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (part.isPaid) Icons.Default.CheckCircle else Icons.Default.HourglassBottom,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text(if (part.isPaid) "Payé" else "À payer") }
                            )
                        } else {
                            StatusBadge(
                                text = if (part.feeExempt) "Exonéré" else "Mensuel",
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DebtsTab(
    state: FeesUiState,
    onShareGroupDebts: () -> Unit,
    onPrivateReminder: (com.foot22.samedi.domain.PlayerDebtSummary) -> Unit
) {
    val debts = state.treasury.playerDebts
    val currency = state.settings.currency

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OutlinedButton(
                onClick = onShareGroupDebts,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Partager le rappel des cotisations sur WhatsApp")
            }
        }

        if (debts.isEmpty()) {
            item {
                Surface(
                    color = PitchEmeraldContainer.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "✅ Aucune dette en cours ! Tous les joueurs sont à jour.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        color = PitchEmerald
                    )
                }
            }
        } else {
            items(debts, key = { it.player.id }) { debt ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(debt.player.displayName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${debt.unpaidMatchCount} match${if (debt.unpaidMatchCount > 1) "s" else ""} non réglé${if (debt.unpaidMatchCount > 1) "s" else ""}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusBadge(
                                text = FeeCalculator.formatMoney(debt.unpaidBalance, currency),
                                containerColor = WarningAmberContainer,
                                contentColor = WarningAmber
                            )
                            IconButton(onClick = { onPrivateReminder(debt) }) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = "Rappel WhatsApp privé",
                                    tint = TeamBlueLight
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
private fun CashLedgerTab(
    transactions: com.foot22.samedi.domain.TreasurySummary,
    ledger: List<CashTransactionEntity>,
    currency: String,
    onAddClick: () -> Unit,
    onDeleteTx: (CashTransactionEntity) -> Unit
) {
    val dateFmt = remember {
        DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryMetricCard(
                    title = "Total Entrées",
                    value = FeeCalculator.formatMoney(transactions.totalIncome, currency),
                    accentColor = PitchEmerald,
                    modifier = Modifier.weight(1f)
                )
                SummaryMetricCard(
                    title = "Total Dépenses",
                    value = FeeCalculator.formatMoney(transactions.totalExpense, currency),
                    accentColor = InjuryRed,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Button(
                onClick = onAddClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ajouter entrée / dépense (terrain, ballon, cotisation)")
            }
        }

        if (ledger.isEmpty()) {
            item {
                Text(
                    text = "Aucune écriture dans le journal de caisse.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(ledger, key = { it.id }) { tx ->
                val isIncome = tx.type == TransactionType.INCOME
                val dateLabel = remember(tx.dateEpoch) {
                    Instant.ofEpochMilli(tx.dateEpoch)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                        .format(dateFmt)
                }
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, StadiumOutline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tx.label,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$dateLabel • ${tx.paymentMethod.label}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${if (isIncome) "+" else "-"}${FeeCalculator.formatMoney(tx.amount, currency)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isIncome) PitchEmerald else InjuryRed
                            )
                            IconButton(onClick = { onDeleteTx(tx) }, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Supprimer",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(17.dp)
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
private fun AddTransactionDialog(
    currency: String,
    onDismiss: () -> Unit,
    onSave: (TransactionType, Double, String, PaymentMethod) -> Unit
) {
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var label by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var method by remember { mutableStateOf(PaymentMethod.CASH) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvelle opération Caisse") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TransactionType.entries.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t.label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Motif") },
                    placeholder = { Text(if (type == TransactionType.EXPENSE) "Ex: Ballon / Eau / Terrain" else "Ex: Cotisation mensuelle Karim") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Montant ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PaymentMethod.entries.forEach { m ->
                        FilterChip(
                            selected = method == m,
                            onClick = { method = m },
                            label = { Text(m.label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            val amount = amountText.replace(",", ".").toDoubleOrNull() ?: 0.0
            Button(
                onClick = { onSave(type, amount, label, method) },
                enabled = label.isNotBlank() && amount > 0.0
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
private fun EditPlayerFeeDialog(
    row: MatchFeeParticipantRow,
    currency: String,
    onDismiss: () -> Unit,
    onTogglePaidWithMethod: (PaymentMethod) -> Unit,
    onToggleExempt: () -> Unit,
    onCustomAmount: (Double) -> Unit
) {
    var customText by remember { mutableStateOf(row.participation.feeDue.toString()) }
    var method by remember { mutableStateOf(row.participation.paymentMethod) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cotisation • ${row.player.displayName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = customText,
                    onValueChange = { customText = it },
                    label = { Text("Montant dû pour ce match ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        customText.replace(",", ".").toDoubleOrNull()?.let(onCustomAmount)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Appliquer ce montant")
                }

                Text("Moyen de paiement :", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PaymentMethod.entries.forEach { m ->
                        FilterChip(
                            selected = method == m,
                            onClick = { method = m },
                            label = { Text(m.label) }
                        )
                    }
                }

                OutlinedButton(
                    onClick = { onTogglePaidWithMethod(method) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (row.participation.isPaid) "Marquer Non Payé" else "Marquer Payé (${method.label})")
                }

                TextButton(
                    onClick = onToggleExempt,
                    colors = ButtonDefaults.textButtonColors(contentColor = WarningAmber)
                ) {
                    Text(if (row.participation.feeExempt) "Retirer l'exonération" else "Exonérer pour ce match")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        }
    )
}

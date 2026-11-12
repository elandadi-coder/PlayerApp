package com.foot22.samedi.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.foot22.samedi.data.local.PlayerEntity
import com.foot22.samedi.data.model.PlayerStatus
import com.foot22.samedi.data.model.TeamSide
import com.foot22.samedi.ui.theme.InjuryRed
import com.foot22.samedi.ui.theme.InjuryRedContainer
import com.foot22.samedi.ui.theme.PitchEmerald
import com.foot22.samedi.ui.theme.PitchEmeraldContainer
import com.foot22.samedi.ui.theme.StadiumOutline
import com.foot22.samedi.ui.theme.TeamBlue
import com.foot22.samedi.ui.theme.TeamBlueContainer
import com.foot22.samedi.ui.theme.TeamBlueLight
import com.foot22.samedi.ui.theme.TeamOrange
import com.foot22.samedi.ui.theme.TeamOrangeContainer
import com.foot22.samedi.ui.theme.TeamOrangeLight
import com.foot22.samedi.ui.theme.WarningAmber
import com.foot22.samedi.ui.theme.WarningAmberContainer
import java.util.Locale

@Composable
fun StatusBadge(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun PlayerStatusBadge(status: PlayerStatus) {
    when (status) {
        PlayerStatus.ACTIF -> StatusBadge("Actif", PitchEmeraldContainer, PitchEmerald)
        PlayerStatus.BLESSE -> Surface(
            color = InjuryRedContainer,
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalHospital,
                    contentDescription = "Blessé",
                    tint = InjuryRed,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Blessé",
                    color = InjuryRed,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
        PlayerStatus.EN_REPRISE -> StatusBadge("En reprise", WarningAmberContainer, WarningAmber)
        PlayerStatus.INACTIF -> StatusBadge("Inactif", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SkillBadge(rating: Double, isRecovering: Boolean = false) {
    val bg = if (isRecovering) WarningAmberContainer else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (isRecovering) WarningAmber else MaterialTheme.colorScheme.onSurface
    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, StadiumOutline)
    ) {
        Text(
            text = String.format(Locale.US, "%.1f", rating),
            color = fg,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun TeamColorDot(side: TeamSide, sizeDp: Int = 10) {
    val color = if (side == TeamSide.BLEU) TeamBlue else TeamOrange
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(color)
    )
}

fun teamAccentColor(side: TeamSide): Color =
    if (side == TeamSide.BLEU) TeamBlueLight else TeamOrangeLight

fun teamSolidColor(side: TeamSide): Color =
    if (side == TeamSide.BLEU) TeamBlue else TeamOrange

fun teamContainerColor(side: TeamSide): Color =
    if (side == TeamSide.BLEU) TeamBlueContainer else TeamOrangeContainer

@Composable
fun SummaryMetricCard(
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, StadiumOutline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = accentColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

fun formatPlayerCompactMeta(player: PlayerEntity, recoveryFactor: Double): String {
    val rating = String.format(Locale.US, "%.1f", player.effectiveRating(recoveryFactor))
    return "${player.displayName} • ${player.position.shortLabel} • $rating"
}

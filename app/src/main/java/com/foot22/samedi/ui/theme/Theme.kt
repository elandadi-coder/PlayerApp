package com.foot22.samedi.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SamediDarkColorScheme = darkColorScheme(
    primary = PitchEmerald,
    onPrimary = Color(0xFF042218),
    primaryContainer = PitchEmeraldContainer,
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = TeamBlueLight,
    onSecondary = Color(0xFF071B3E),
    secondaryContainer = TeamBlueContainer,
    onSecondaryContainer = Color(0xFFDBEAFE),
    tertiary = TeamOrangeLight,
    onTertiary = Color(0xFF331503),
    tertiaryContainer = TeamOrangeContainer,
    onTertiaryContainer = Color(0xFFFFEDD5),
    background = StadiumDarkBg,
    onBackground = TextPrimaryDark,
    surface = StadiumCardBg,
    onSurface = TextPrimaryDark,
    surfaceVariant = StadiumCardAlt,
    onSurfaceVariant = TextSecondaryDark,
    outline = StadiumOutline,
    error = InjuryRed,
    onError = Color.White,
    errorContainer = InjuryRedContainer,
    onErrorContainer = Color(0xFFFECACA)
)

@Composable
fun Samedi22Theme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SamediDarkColorScheme,
        typography = AppTypography,
        content = content
    )
}

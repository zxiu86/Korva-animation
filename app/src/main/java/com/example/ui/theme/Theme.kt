package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val KorvaDarkColorScheme = darkColorScheme(
    primary = KorvaVioletPrimary,
    onPrimary = Color.White,
    primaryContainer = KorvaVioletDark,
    onPrimaryContainer = KorvaVioletLight,
    secondary = KorvaVioletLight,
    onSecondary = StudioObsidianDark,
    secondaryContainer = StudioSurfaceVariant,
    onSecondaryContainer = KorvaVioletLight,
    tertiary = StudioCyan,
    onTertiary = StudioObsidianDark,
    background = StudioObsidianDark,
    onBackground = TextPrimary,
    surface = StudioPanelDark,
    onSurface = TextPrimary,
    surfaceVariant = StudioSurfaceDark,
    onSurfaceVariant = TextSecondary,
    outline = StudioBorder,
    outlineVariant = StudioBorderLight,
    error = StudioRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = KorvaDarkColorScheme,
        typography = Typography,
        content = content
    )
}

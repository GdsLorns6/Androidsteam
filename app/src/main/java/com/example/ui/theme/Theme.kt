package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SteamDarkColorScheme = darkColorScheme(
    primary = SteamCyan,
    onPrimary = Color(0xFF003544),
    primaryContainer = SteamBlue,
    onPrimaryContainer = SteamCyan,
    secondary = SteamTeal,
    onSecondary = Color(0xFF00344D),
    secondaryContainer = SteamCardElevated,
    onSecondaryContainer = SteamTextPrimary,
    background = SteamBackground,
    onBackground = SteamTextPrimary,
    surface = SteamSurface,
    onSurface = SteamTextPrimary,
    surfaceVariant = SteamCard,
    onSurfaceVariant = SteamTextSecondary,
    outline = SteamBorder,
    error = SteamError,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SteamDarkColorScheme,
        typography = Typography,
        content = content
    )
}

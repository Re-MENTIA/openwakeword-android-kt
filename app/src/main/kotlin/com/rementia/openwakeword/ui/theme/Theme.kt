package com.rementia.openwakeword.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AccentGreen,
    onPrimary = Black,
    primaryContainer = AccentGreenDim,
    onPrimaryContainer = TextPrimary,
    
    secondary = LightGray,
    onSecondary = TextPrimary,
    secondaryContainer = MediumGray,
    onSecondaryContainer = TextPrimary,
    
    background = Black,
    onBackground = TextPrimary,
    
    surface = SurfaceBlack,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = TextSecondary,
    
    error = InactiveRed,
    onError = TextPrimary,
    
    outline = MediumGray,
    outlineVariant = DarkGray
)

@Composable
fun OpenWakeWordTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
package com.funtime.sciai.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    background = SciAIDark,
    surface = SciAISurface,
    surfaceVariant = SciAISurfaceAlt,
    primary = SciAICyan,
    secondary = SciAIAmber,
    tertiary = SciAIPurple,
    error = SciAIRed,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = SciAIText,
    onSurface = SciAIText,
    onSurfaceVariant = SciAISubtext,
    outline = SciAIBorder,
    outlineVariant = SciAIBorderLight
)

@Composable
fun SciAITheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
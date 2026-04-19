package com.funtime.sciai.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── Light Theme Colors (Premium Ivory) ──────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    background = Color(0xFFFBFBF9),   // Soft Ivory background for reading comfort
    surface = Color.White,
    surfaceVariant = Color(0xFFF1F4F9),
    primary = Color(0xFF0F172A),       // Deep Slate text/primary for sharpness
    secondary = Color(0xFFD97706),
    tertiary = Color(0xFF9333EA),
    error = Color(0xFFDC2626),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF0F172A),  // High contrast text
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFE2E8F0),
    outlineVariant = Color(0xFFF1F5F9)
)

// ── Dark Theme Colors (Deep Slate) ───────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    background = Color(0xFF020617),    // Deeper Dark (Slate-950)
    surface = Color(0xFF0F172A),       // Surface (Slate-900)
    surfaceVariant = Color(0xFF1E293B), // Surface Alt (Slate-800)
    primary = Color(0xFF38BDF8),
    secondary = Color(0xFFFBBF24),
    tertiary = Color(0xFFA855F7),
    error = Color(0xFFEF4444),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color(0xFFF1F5F9),  // Crisp Slate-100 text
    onSurface = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155),
    outlineVariant = Color(0x33FFFFFF)
)

@Composable
fun SciAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
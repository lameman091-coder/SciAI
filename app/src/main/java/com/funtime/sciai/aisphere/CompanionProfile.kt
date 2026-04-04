package com.funtime.sciai.aisphere

import androidx.compose.ui.graphics.Color

/**
 * Complete companion identity & appearance profile.
 * Persisted locally via SharedPreferences.
 *
 * Future-ready: structured for unlockable accessories,
 * premium customization, voice mapping, and AI memory.
 */
data class CompanionProfile(
    val name: String = "Orbi",
    val gender: CompanionGender = CompanionGender.NEUTRAL,
    val colorTheme: CompanionColorTheme = CompanionColorTheme.OCEAN,
    val accessory: CompanionAccessory = CompanionAccessory.NONE,
    val hasGlow: Boolean = true
)

/**
 * Gender affects future voice/personality mapping.
 */
enum class CompanionGender(val displayName: String, val emoji: String) {
    MALE("Male", "♂"),
    FEMALE("Female", "♀"),
    NEUTRAL("Neutral", "⚡")
}

/**
 * Color themes that tint the sphere's base gradient.
 * Each theme provides three harmonious colors plus a glow color.
 */
enum class CompanionColorTheme(
    val displayName: String,
    val primary: Color,
    val secondary: Color,
    val accent: Color,
    val glow: Color
) {
    OCEAN(
        "Ocean",
        Color(0xFF38BDF8), Color(0xFF6366F1), Color(0xFF22D3EE), Color(0xFF7DD3FC)
    ),
    SAKURA(
        "Sakura",
        Color(0xFFF472B6), Color(0xFFEC4899), Color(0xFFFDA4AF), Color(0xFFFBCFE8)
    ),
    FOREST(
        "Forest",
        Color(0xFF34D399), Color(0xFF059669), Color(0xFF6EE7B7), Color(0xFFA7F3D0)
    ),
    SUNSET(
        "Sunset",
        Color(0xFFF97316), Color(0xFFEF4444), Color(0xFFFBBF24), Color(0xFFFDE68A)
    ),
    COSMIC(
        "Cosmic",
        Color(0xFFA855F7), Color(0xFF7C3AED), Color(0xFFC084FC), Color(0xFFE9D5FF)
    )
}

/**
 * Lightweight accessories drawn on the Canvas layer above the sphere.
 * Kept minimal — this is a soft companion, not a game avatar.
 */
enum class CompanionAccessory(val displayName: String, val emoji: String) {
    NONE("None", ""),
    HAT("Hat", "🎩"),
    RIBBON("Ribbon", "🎀"),
    GLASSES("Glasses", "👓"),
    CROWN("Crown", "👑"),
    STAR("Star", "⭐")
}

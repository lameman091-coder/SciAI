package com.funtime.sciai.aisphere

import androidx.compose.ui.graphics.Color

/**
 * Emotion states for the AI Sphere Companion.
 * Each state defines a complete visual + behavioral profile.
 * 9 states total — covering the full emotional spectrum of a tiny digital companion.
 */
enum class EmotionState {
    HAPPY, IDLE, SAD, ANGRY, EXCITED, SLEEP, LOVE, SHY, CONFUSED, PLAYFUL_EVIL, JUGGLING, CONCERNED
}

/**
 * Visual configuration for each emotion state.
 * Multi-layer gradient system with kaomoji expression text.
 */
data class EmotionVisuals(
    val primaryColor: Color,
    val secondaryColor: Color,
    val accentColor: Color,
    val tertiaryColor: Color,       // Third gradient layer for depth
    val glowColor: Color,
    val animationSpeed: Float,
    val glowIntensity: Float,
    val breathingScale: Float,
    val expression: ExpressionType,
    val kaomoji: String             // Display text for the expression
)

enum class ExpressionType {
    NEUTRAL, SMILE, SAD_FACE, ANGRY_FACE, EXCITED_FACE, SLEEPING,
    LOVE_FACE, SHY_FACE, CONFUSED_FACE, EVIL_FACE, JUGGLING_FACE, CONCERNED_FACE
}

/**
 * Personality modes that affect message tone and behavior.
 * PLAYFUL = default bubbly tone
 * CALM = gentle, warm tone
 * SILENT = no messages
 * STUDY = minimal, academic hints only
 */
enum class PersonalityMode(val displayName: String) {
    PLAYFUL("Playful"),
    CALM("Calm"),
    SILENT("Silent"),
    STUDY("Study")
}

/**
 * Maps each EmotionState to its visual configuration.
 * Uses rich, multi-layer gradients for each emotion.
 */
object EmotionPalette {

    fun getVisuals(state: EmotionState): EmotionVisuals = when (state) {
        EmotionState.HAPPY -> EmotionVisuals(
            primaryColor = Color(0xFFFBBF24),      // Warm amber
            secondaryColor = Color(0xFFFB7185),     // Soft pink
            accentColor = Color(0xFFF97316),        // Orange
            tertiaryColor = Color(0xFFFDE68A),      // Light amber
            glowColor = Color(0xFFFDE68A),          // Amber glow
            animationSpeed = 1.3f,
            glowIntensity = 0.7f,
            breathingScale = 0.06f,
            expression = ExpressionType.SMILE,
            kaomoji = "^_^"
        )

        EmotionState.IDLE -> EmotionVisuals(
            primaryColor = Color(0xFF64748B),       // Slate
            secondaryColor = Color(0xFF6366F1),     // Indigo
            accentColor = Color(0xFF22D3EE),        // Cyan
            tertiaryColor = Color(0xFF818CF8),      // Indigo light
            glowColor = Color(0xFF818CF8),          // Indigo glow
            animationSpeed = 1.0f,
            glowIntensity = 0.4f,
            breathingScale = 0.04f,
            expression = ExpressionType.NEUTRAL,
            kaomoji = "·_·"
        )

        EmotionState.SAD -> EmotionVisuals(
            primaryColor = Color(0xFF3B82F6),       // Blue
            secondaryColor = Color(0xFF4338CA),     // Indigo deep
            accentColor = Color(0xFF6366F1),        // Indigo
            tertiaryColor = Color(0xFF1E3A5F),      // Deep blue
            glowColor = Color(0xFF93C5FD),          // Blue glow
            animationSpeed = 0.5f,
            glowIntensity = 0.25f,
            breathingScale = 0.02f,
            expression = ExpressionType.SAD_FACE,
            kaomoji = ";_;"
        )

        EmotionState.ANGRY -> EmotionVisuals(
            primaryColor = Color(0xFFEF4444),       // Red
            secondaryColor = Color(0xFF991B1B),     // Dark crimson
            accentColor = Color(0xFFF97316),        // Orange
            tertiaryColor = Color(0xFFDC2626),      // Red 600
            glowColor = Color(0xFFFCA5A5),          // Red glow
            animationSpeed = 2.0f,
            glowIntensity = 0.95f,
            breathingScale = 0.03f,
            expression = ExpressionType.ANGRY_FACE,
            kaomoji = ">:("
        )

        EmotionState.EXCITED -> EmotionVisuals(
            primaryColor = Color(0xFFEC4899),       // Pink
            secondaryColor = Color(0xFFF59E0B),     // Amber
            accentColor = Color(0xFF06B6D4),        // Cyan
            tertiaryColor = Color(0xFFA855F7),      // Purple
            glowColor = Color(0xFFF9A8D4),          // Pink glow
            animationSpeed = 2.2f,
            glowIntensity = 1.0f,
            breathingScale = 0.09f,
            expression = ExpressionType.EXCITED_FACE,
            kaomoji = "★‿★"
        )

        EmotionState.SLEEP -> EmotionVisuals(
            primaryColor = Color(0xFF1E293B),       // Dark slate
            secondaryColor = Color(0xFF0F172A),     // Near-black
            accentColor = Color(0xFF334155),        // Slate 700
            tertiaryColor = Color(0xFF020617),      // Black
            glowColor = Color(0xFF475569),          // Slate dim glow
            animationSpeed = 0.25f,
            glowIntensity = 0.12f,
            breathingScale = 0.015f,
            expression = ExpressionType.SLEEPING,
            kaomoji = "-_- zZ"
        )

        EmotionState.LOVE -> EmotionVisuals(
            primaryColor = Color(0xFFEC4899),       // Pink
            secondaryColor = Color(0xFFDB2777),     // Magenta
            accentColor = Color(0xFFA855F7),        // Purple
            tertiaryColor = Color(0xFFF472B6),      // Pink light
            glowColor = Color(0xFFFBCFE8),          // Warm pink glow
            animationSpeed = 1.0f,
            glowIntensity = 0.85f,
            breathingScale = 0.05f,
            expression = ExpressionType.LOVE_FACE,
            kaomoji = "♡‿♡"
        )

        EmotionState.SHY -> EmotionVisuals(
            primaryColor = Color(0xFFFDA4AF),       // Peach
            secondaryColor = Color(0xFFFBCFE8),     // Soft pink
            accentColor = Color(0xFFFB7185),        // Rose
            tertiaryColor = Color(0xFFFFE4E6),      // Very light pink
            glowColor = Color(0xFFFDA4AF),          // Peach glow
            animationSpeed = 0.8f,
            glowIntensity = 0.6f,
            breathingScale = 0.03f,
            expression = ExpressionType.SHY_FACE,
            kaomoji = ">~<"
        )

        EmotionState.CONFUSED -> EmotionVisuals(
            primaryColor = Color(0xFF2DD4BF),       // Teal
            secondaryColor = Color(0xFF8B5CF6),     // Purple
            accentColor = Color(0xFF22D3EE),        // Cyan
            tertiaryColor = Color(0xFF6366F1),      // Indigo
            glowColor = Color(0xFF5EEAD4),          // Teal glow
            animationSpeed = 1.1f,
            glowIntensity = 0.55f,
            breathingScale = 0.04f,
            expression = ExpressionType.CONFUSED_FACE,
            kaomoji = "o_O"
        )

        EmotionState.PLAYFUL_EVIL -> EmotionVisuals(
            primaryColor = Color(0xFF0A0A0A),       // Jet black
            secondaryColor = Color(0xFF1A0000),     // Dark crimson-black
            accentColor = Color(0xFFDC2626),        // Blood red accent
            tertiaryColor = Color(0xFF0F0F0F),      // Near-void
            glowColor = Color(0xFFFF0000),          // Sinister red glow
            animationSpeed = 2.5f,
            glowIntensity = 1.2f,
            breathingScale = 0.12f,                 // Big pulsing
            expression = ExpressionType.EVIL_FACE,
            kaomoji = "ψ(｀∇´)ψ"
        )

        EmotionState.JUGGLING -> EmotionVisuals(
            primaryColor = Color(0xFFFBBF24),       // Gold
            secondaryColor = Color(0xFF06B6D4),     // Cyan
            accentColor = Color(0xFFA855F7),        // Purple
            tertiaryColor = Color(0xFFEC4899),      // Pink
            glowColor = Color(0xFFFDE68A),          // Warm glow
            animationSpeed = 3.0f,                  // Very fast
            glowIntensity = 1.0f,
            breathingScale = 0.10f,
            expression = ExpressionType.JUGGLING_FACE,
            kaomoji = "◎‿◎"
        )

        EmotionState.CONCERNED -> EmotionVisuals(
            primaryColor = Color(0xFFF59E0B),       // Warm amber
            secondaryColor = Color(0xFFD97706),     // Darker amber
            accentColor = Color(0xFFFBBF24),        // Gold
            tertiaryColor = Color(0xFFB45309),      // Deep amber
            glowColor = Color(0xFFFDE68A),          // Soft amber glow
            animationSpeed = 0.7f,                  // Slow, thoughtful
            glowIntensity = 0.5f,
            breathingScale = 0.03f,
            expression = ExpressionType.CONCERNED_FACE,
            kaomoji = "(._. )"
        )
    }
}

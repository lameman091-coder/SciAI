package com.funtime.sciai.aisphere

import androidx.compose.runtime.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp as colorLerp

/**
 * Emotion Blending System — smooth transitions between emotions.
 * Instead of instant-switching, emotions crossfade with spring physics.
 *
 * Provides interpolated [EmotionVisuals] that blend colors, glow,
 * speed, and breathing between the previous and current emotion.
 */
object EmotionBlender {

    /**
     * Interpolate between two EmotionVisuals based on progress (0..1).
     * At progress=0, returns [from]. At progress=1, returns [to].
     */
    fun blend(from: EmotionVisuals, to: EmotionVisuals, progress: Float): EmotionVisuals {
        val p = progress.coerceIn(0f, 1f)
        return EmotionVisuals(
            primaryColor = colorLerp(from.primaryColor, to.primaryColor, p),
            secondaryColor = colorLerp(from.secondaryColor, to.secondaryColor, p),
            accentColor = colorLerp(from.accentColor, to.accentColor, p),
            tertiaryColor = colorLerp(from.tertiaryColor, to.tertiaryColor, p),
            glowColor = colorLerp(from.glowColor, to.glowColor, p),
            animationSpeed = lerp(from.animationSpeed, to.animationSpeed, p),
            glowIntensity = lerp(from.glowIntensity, to.glowIntensity, p),
            breathingScale = lerp(from.breathingScale, to.breathingScale, p),
            // Expression switches at the halfway point for clean crossfade
            expression = if (p < 0.5f) from.expression else to.expression,
            kaomoji = if (p < 0.5f) from.kaomoji else to.kaomoji
        )
    }

    /**
     * Returns the alpha for the "outgoing" expression during crossfade.
     * Fades from 1→0 during 0→0.5 progress.
     */
    fun outgoingExpressionAlpha(progress: Float): Float {
        return if (progress < 0.5f) 1f - (progress * 2f) else 0f
    }

    /**
     * Returns the alpha for the "incoming" expression during crossfade.
     * Fades from 0→1 during 0.5→1 progress.
     */
    fun incomingExpressionAlpha(progress: Float): Float {
        return if (progress >= 0.5f) (progress - 0.5f) * 2f else 0f
    }

    private fun lerp(start: Float, end: Float, fraction: Float): Float {
        return start + (end - start) * fraction
    }
}

/**
 * State holder for emotion blending within a Composable.
 * Tracks previous emotion and animates transition progress.
 */
@Composable
fun rememberEmotionBlendState(
    currentEmotion: EmotionState
): EmotionBlendState {
    var previousEmotion by remember { mutableStateOf(currentEmotion) }
    var targetEmotion by remember { mutableStateOf(currentEmotion) }

    // Detect emotion changes
    LaunchedEffect(currentEmotion) {
        if (currentEmotion != targetEmotion) {
            previousEmotion = targetEmotion
            targetEmotion = currentEmotion
        }
    }

    // Animate transition progress
    val progress by animateFloatAsState(
        targetValue = if (targetEmotion == currentEmotion) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = 200f
        ),
        label = "emotionBlend"
    )

    val fromVisuals = EmotionPalette.getVisuals(previousEmotion)
    val toVisuals = EmotionPalette.getVisuals(targetEmotion)
    val blendedVisuals = EmotionBlender.blend(fromVisuals, toVisuals, progress)

    return remember(previousEmotion, targetEmotion, progress) {
        EmotionBlendState(
            previousEmotion = previousEmotion,
            currentEmotion = targetEmotion,
            progress = progress,
            blendedVisuals = blendedVisuals,
            fromVisuals = fromVisuals,
            toVisuals = toVisuals
        )
    }
}

/**
 * Immutable snapshot of the current emotion blend state.
 */
data class EmotionBlendState(
    val previousEmotion: EmotionState,
    val currentEmotion: EmotionState,
    val progress: Float,
    val blendedVisuals: EmotionVisuals,
    val fromVisuals: EmotionVisuals,
    val toVisuals: EmotionVisuals
) {
    val isTransitioning: Boolean get() = progress < 0.95f
    val outgoingAlpha: Float get() = EmotionBlender.outgoingExpressionAlpha(progress)
    val incomingAlpha: Float get() = EmotionBlender.incomingExpressionAlpha(progress)
}

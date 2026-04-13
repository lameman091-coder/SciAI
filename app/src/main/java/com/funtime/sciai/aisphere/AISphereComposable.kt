package com.funtime.sciai.aisphere

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.*

/**
 * The AI Sphere Composable — a living, expressive digital companion.
 *
 * Features:
 * - Emotion-blended multi-layer gradients with smooth transitions
 * - Anime-style Canvas-drawn expressions (^_^, ♡‿♡, >~<, etc.)
 * - Micro-animations: blink, blush, hearts, ripples, droop, compress, tilt
 * - Spark particles, sleep Z's, and emotion-specific effects
 * - Inner glow + outer aura for depth
 */
@Composable
fun AISphereComposable(
    emotionState: EmotionState,
    bounceScale: Float = 1f,
    shakeOffset: Float = 0f,
    compressScale: Float = 1f,
    droopOffset: Float = 0f,
    tiltAngle: Float = 0f,
    reduceAnimations: Boolean = false,
    companionProfile: CompanionProfile = CompanionProfile(),
    size: Dp = 64.dp,
    modifier: Modifier = Modifier
) {
    // ── Emotion blending ────────────────────────────────────────────
    val blendState = rememberEmotionBlendState(emotionState)
    val visuals = blendState.blendedVisuals

    // ── Infinite transition for breathing + gradient rotation ───────
    val infiniteTransition = rememberInfiniteTransition(label = "sphere")
    val speed = if (reduceAnimations) visuals.animationSpeed * 0.3f else visuals.animationSpeed

    // Breathing scale
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 1f - visuals.breathingScale,
        targetValue = 1f + visuals.breathingScale,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (2000 / speed.coerceAtLeast(0.1f)).toInt(),
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    // Gradient rotation
    val gradientRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (4000 / speed.coerceAtLeast(0.1f)).toInt(),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Glow pulse
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (1500 / speed.coerceAtLeast(0.1f)).toInt(),
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    // Inner ring shimmer
    val shimmerAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (6000 / speed.coerceAtLeast(0.1f)).toInt(),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    // Expression blink
    val blinkValue by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 4000
                1f at 0
                1f at 3700
                0.1f at 3800
                1f at 3900
                1f at 4000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "blink"
    )

    // Spark/particle phase
    val sparkPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spark"
    )

    // Heart float phase (for LOVE)
    val heartPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "heart"
    )

    // Blush pulse (for SHY / LOVE)
    val blushPulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blush"
    )

    // Ripple expand (for ANGRY)
    val rippleExpand by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple"
    )

    // ── Combined transforms ─────────────────────────────────────────
    // Special burst scale for "Engulf" (Evil) state
    val burstPulse by animateFloatAsState(
        targetValue = if (emotionState == EmotionState.PLAYFUL_EVIL) 1.4f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f),
        label = "burstPulse"
    )
    val finalScale = breathingScale * bounceScale * compressScale * burstPulse
    val glowMult = if (companionProfile.hasGlow) 1.2f else 0.15f
    val theme = companionProfile.colorTheme

    // ── Render ──────────────────────────────────────────────────────
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = finalScale
                scaleY = finalScale * (if (compressScale < 1f) 1.05f else 1f) // Slight squash
                translationX = shakeOffset
                translationY = droopOffset
                rotationZ = tiltAngle
            }
            // Outer aura (low opacity)
            .drawBehind {
                val auraRadius = this.size.minDimension * 1.1f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            theme.glow.copy(alpha = visuals.glowIntensity * glowPulse * 0.4f * glowMult),
                            theme.glow.copy(alpha = visuals.glowIntensity * glowPulse * 0.15f * glowMult),
                            theme.primary.copy(alpha = visuals.glowIntensity * glowPulse * 0.05f * glowMult),
                            Color.Transparent
                        ),
                        radius = auraRadius
                    ),
                    radius = auraRadius
                )
            }
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = this.size.minDimension / 2f

            // ── Main sphere body with rotating triple-gradient ───
            rotate(gradientRotation) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            blendColors(visuals.primaryColor, theme.primary, 0.4f),
                            blendColors(visuals.secondaryColor, theme.secondary, 0.5f),
                            blendColors(visuals.tertiaryColor, theme.accent, 0.4f),
                            blendColors(visuals.accentColor, theme.primary, 0.5f),
                            blendColors(visuals.primaryColor.copy(alpha = 0.85f), theme.primary.copy(alpha = 0.85f), 0.4f),
                            blendColors(visuals.secondaryColor.copy(alpha = 0.9f), theme.secondary.copy(alpha = 0.9f), 0.5f),
                            blendColors(visuals.primaryColor, theme.primary, 0.4f)
                        )
                    ),
                    radius = radius,
                    center = center
                )
            }

            // ── Inner glow layer ────────────────────────────────
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        theme.glow.copy(alpha = visuals.glowIntensity * 0.3f * glowMult),
                        Color.Transparent,
                        theme.primary.copy(alpha = 0.15f)
                    ),
                    center = center,
                    radius = radius * 0.8f
                ),
                radius = radius * 0.8f,
                center = center
            )

            // ── Glassmorphism depth layer ───────────────────────
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.2f)
                    ),
                    center = Offset(center.x - radius * 0.25f, center.y - radius * 0.25f),
                    radius = radius * 1.2f
                ),
                radius = radius,
                center = center
            )

            // ── Shimmer ring ────────────────────────────────────
            rotate(shimmerAngle) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.0f),
                            Color.White.copy(alpha = 0.12f),
                            Color.White.copy(alpha = 0.0f),
                            Color.White.copy(alpha = 0.06f),
                            Color.White.copy(alpha = 0.0f)
                        )
                    ),
                    radius = radius * 0.85f,
                    center = center,
                    style = Stroke(width = radius * 0.15f)
                )
            }

            // ── Highlight spot (top-left) ───────────────────────
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.35f),
                        Color.Transparent
                    ),
                    center = Offset(center.x - radius * 0.3f, center.y - radius * 0.35f),
                    radius = radius * 0.4f
                ),
                radius = radius * 0.4f,
                center = Offset(center.x - radius * 0.3f, center.y - radius * 0.35f)
            )

            // ── Angry ripple ring ───────────────────────────────
            if (emotionState == EmotionState.ANGRY && !reduceAnimations) {
                val rippleRadius = radius * (1.0f + rippleExpand * 0.5f)
                val rippleAlpha = (1f - rippleExpand) * 0.4f
                drawCircle(
                    color = Color(0xFFEF4444).copy(alpha = rippleAlpha),
                    radius = rippleRadius,
                    center = center,
                    style = Stroke(width = radius * 0.04f)
                )
            }

            // ── Blush cheeks (SHY + LOVE) ───────────────────────
            if ((emotionState == EmotionState.SHY || emotionState == EmotionState.LOVE) && !reduceAnimations) {
                val blushAlpha = blushPulse
                val blushRadius = radius * 0.15f
                val blushY = center.y + radius * 0.08f
                // Left cheek
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFF6B8A).copy(alpha = blushAlpha),
                            Color.Transparent
                        ),
                        center = Offset(center.x - radius * 0.35f, blushY),
                        radius = blushRadius
                    ),
                    radius = blushRadius,
                    center = Offset(center.x - radius * 0.35f, blushY)
                )
                // Right cheek
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFF6B8A).copy(alpha = blushAlpha),
                            Color.Transparent
                        ),
                        center = Offset(center.x + radius * 0.35f, blushY),
                        radius = blushRadius
                    ),
                    radius = blushRadius,
                    center = Offset(center.x + radius * 0.35f, blushY)
                )
            }

            // ── Expression face ─────────────────────────────────
            // During transitions, crossfade between expressions
            if (blendState.isTransitioning && !reduceAnimations) {
                // Draw outgoing expression fading out
                if (blendState.outgoingAlpha > 0.01f) {
                    drawExpression(
                        expression = blendState.fromVisuals.expression,
                        center = center,
                        radius = radius,
                        blinkValue = blinkValue,
                        alpha = 0.65f * blendState.outgoingAlpha
                    )
                }
                // Draw incoming expression fading in
                if (blendState.incomingAlpha > 0.01f) {
                    drawExpression(
                        expression = blendState.toVisuals.expression,
                        center = center,
                        radius = radius,
                        blinkValue = blinkValue,
                        alpha = 0.65f * blendState.incomingAlpha
                    )
                }
            } else {
                drawExpression(
                    expression = visuals.expression,
                    center = center,
                    radius = radius,
                    blinkValue = blinkValue,
                    alpha = 0.70f
                )
            }

            // ── Spark particles (EXCITED) ───────────────────────
            if (emotionState == EmotionState.EXCITED && !reduceAnimations) {
                drawSparkParticles(center, radius, sparkPhase, visuals.accentColor)
            }

            // ── Heart particles (LOVE) ──────────────────────────
            if (emotionState == EmotionState.LOVE && !reduceAnimations) {
                drawHeartParticles(center, radius, heartPhase, Color(0xFFFF6B8A))
            }

            // ── Zzz effect (SLEEP) ──────────────────────────────
            if (emotionState == EmotionState.SLEEP) {
                drawSleepEffect(center, radius, sparkPhase, Color.White.copy(alpha = 0.3f))
            }

            // ── Confused question mark (CONFUSED) ───────────
            if (emotionState == EmotionState.CONFUSED && !reduceAnimations) {
                drawConfusedEffect(center, radius, sparkPhase, visuals.glowColor)
            }

            // ── PLAYFUL_EVIL dark aura + smoke ───────────────
            if (emotionState == EmotionState.PLAYFUL_EVIL && !reduceAnimations) {
                drawEvilAura(center, radius, rippleExpand)
                drawSmokeParticles(center, radius, sparkPhase)
            }

            // ── JUGGLING orbiting balls ─────────────────────
            if (emotionState == EmotionState.JUGGLING && !reduceAnimations) {
                drawJugglingBalls(center, radius, sparkPhase)
            }

            // ── Accessories ─────────────────────────────────────
            if (companionProfile.accessory != CompanionAccessory.NONE) {
                drawAccessory(companionProfile.accessory, center, radius)
            }
        }
    }
}

/** Utility to blend two colors nicely */
private fun blendColors(c1: Color, c2: Color, weight2: Float): Color {
    val weight1 = 1f - weight2
    return Color(
        red = c1.red * weight1 + c2.red * weight2,
        green = c1.green * weight1 + c2.green * weight2,
        blue = c1.blue * weight1 + c2.blue * weight2,
        alpha = c1.alpha * weight1 + c2.alpha * weight2
    )
}

// ═══════════════════════════════════════════════════════════════════════
// EXPRESSION DRAWING — Anime-style Canvas faces
// ═══════════════════════════════════════════════════════════════════════

private fun DrawScope.drawExpression(
    expression: ExpressionType,
    center: Offset,
    radius: Float,
    blinkValue: Float,
    alpha: Float
) {
    val eyeSpacing = radius * 0.28f
    val eyeY = center.y - radius * 0.08f
    val mouthY = center.y + radius * 0.22f
    val eyeRadius = radius * 0.055f
    val color = Color.White.copy(alpha = alpha)

    when (expression) {
        ExpressionType.NEUTRAL -> drawNeutralFace(center, eyeSpacing, eyeY, mouthY, eyeRadius, blinkValue, color, radius)
        ExpressionType.SMILE -> drawHappyFace(center, eyeSpacing, eyeY, mouthY, eyeRadius, blinkValue, color, radius, alpha)
        ExpressionType.SAD_FACE -> drawSadFace(center, eyeSpacing, eyeY, mouthY, eyeRadius, blinkValue, color, radius)
        ExpressionType.ANGRY_FACE -> drawAngryFace(center, eyeSpacing, eyeY, mouthY, eyeRadius, blinkValue, color, radius)
        ExpressionType.EXCITED_FACE -> drawExcitedFace(center, eyeSpacing, eyeY, mouthY, eyeRadius, blinkValue, color, radius, alpha)
        ExpressionType.SLEEPING -> drawSleepingFace(center, eyeSpacing, eyeY, mouthY, eyeRadius, color, radius, alpha)
        ExpressionType.LOVE_FACE -> drawLoveFace(center, eyeSpacing, eyeY, mouthY, eyeRadius, color, radius, alpha)
        ExpressionType.SHY_FACE -> drawShyFace(center, eyeSpacing, eyeY, mouthY, eyeRadius, color, radius, alpha)
        ExpressionType.CONFUSED_FACE -> drawConfusedFace(center, eyeSpacing, eyeY, mouthY, eyeRadius, blinkValue, color, radius)
        ExpressionType.EVIL_FACE -> drawEvilFace(center, eyeSpacing, eyeY, mouthY, eyeRadius, blinkValue, color, radius, alpha)
        ExpressionType.JUGGLING_FACE -> drawJugglingFace(center, eyeSpacing, eyeY, mouthY, eyeRadius, blinkValue, color, radius, alpha)
        ExpressionType.CONCERNED_FACE -> drawConcernedFace(center, eyeSpacing, eyeY, mouthY, eyeRadius, blinkValue, color, radius)
    }
}
// ── CONCERNED /_\ ───────────────────────────────────────────────────
private fun DrawScope.drawConcernedFace(
    center: Offset, eyeSpacing: Float, eyeY: Float, mouthY: Float,
    eyeRadius: Float, blinkValue: Float, color: Color, radius: Float
) {
    val eyeH = eyeRadius * 2f * blinkValue
    
    // Slanted eyebrows
    drawLine(color, Offset(center.x - eyeSpacing - eyeRadius * 1.5f, eyeY - radius * 0.08f), Offset(center.x - eyeSpacing + eyeRadius, eyeY - radius * 0.15f), strokeWidth = radius * 0.035f, cap = StrokeCap.Round)
    drawLine(color, Offset(center.x + eyeSpacing + eyeRadius * 1.5f, eyeY - radius * 0.08f), Offset(center.x + eyeSpacing - eyeRadius, eyeY - radius * 0.15f), strokeWidth = radius * 0.035f, cap = StrokeCap.Round)
    
    // Wide eyes
    drawOval(color, Offset(center.x - eyeSpacing - eyeRadius * 1.2f, eyeY - eyeH / 2f), Size(eyeRadius * 2.4f, eyeH))
    drawOval(color, Offset(center.x + eyeSpacing - eyeRadius * 1.2f, eyeY - eyeH / 2f), Size(eyeRadius * 2.4f, eyeH))
    
    // Small worried mouth
    drawArc(color, 180f, 180f, false, Offset(center.x - radius * 0.1f, mouthY), Size(radius * 0.2f, radius * 0.08f), style = Stroke(width = radius * 0.035f, cap = StrokeCap.Round))
}

// ── NEUTRAL ·_· ─────────────────────────────────────────────────────

private fun DrawScope.drawNeutralFace(
    center: Offset, eyeSpacing: Float, eyeY: Float, mouthY: Float,
    eyeRadius: Float, blinkValue: Float, color: Color, radius: Float
) {
    val eyeH = eyeRadius * 2f * blinkValue
    drawOval(color, Offset(center.x - eyeSpacing - eyeRadius, eyeY - eyeH / 2f), Size(eyeRadius * 2f, eyeH))
    drawOval(color, Offset(center.x + eyeSpacing - eyeRadius, eyeY - eyeH / 2f), Size(eyeRadius * 2f, eyeH))
    drawLine(color, Offset(center.x - radius * 0.10f, mouthY), Offset(center.x + radius * 0.10f, mouthY),
        strokeWidth = radius * 0.03f, cap = StrokeCap.Round)
}

// ── HAPPY ^_^ ───────────────────────────────────────────────────────
// Curved upward eyes (anime happy-squeeze) + smile

private fun DrawScope.drawHappyFace(
    center: Offset, eyeSpacing: Float, eyeY: Float, mouthY: Float,
    eyeRadius: Float, blinkValue: Float, color: Color, radius: Float, alpha: Float
) {
    // ^_^ style eyes — curved arcs pointing upward (happy squint)
    val eyeArcWidth = eyeRadius * 3f
    val eyeArcHeight = eyeRadius * 2f

    // Left eye — upward arc (like ^)
    drawArc(
        color = color,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(center.x - eyeSpacing - eyeArcWidth / 2f, eyeY - eyeArcHeight),
        size = Size(eyeArcWidth, eyeArcHeight * blinkValue.coerceAtLeast(0.3f)),
        style = Stroke(width = radius * 0.04f, cap = StrokeCap.Round)
    )
    // Right eye
    drawArc(
        color = color,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(center.x + eyeSpacing - eyeArcWidth / 2f, eyeY - eyeArcHeight),
        size = Size(eyeArcWidth, eyeArcHeight * blinkValue.coerceAtLeast(0.3f)),
        style = Stroke(width = radius * 0.04f, cap = StrokeCap.Round)
    )

    // Smile arc
    drawArc(
        color = color,
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(center.x - radius * 0.18f, mouthY - radius * 0.08f),
        size = Size(radius * 0.36f, radius * 0.18f),
        style = Stroke(width = radius * 0.035f, cap = StrokeCap.Round)
    )
}

// ── SAD ;_; ─────────────────────────────────────────────────────────
// Droopy eyes with tear marks

private fun DrawScope.drawSadFace(
    center: Offset, eyeSpacing: Float, eyeY: Float, mouthY: Float,
    eyeRadius: Float, blinkValue: Float, color: Color, radius: Float
) {
    val eyeH = eyeRadius * 2f * blinkValue
    val droop = radius * 0.03f

    // Sad eyes (slightly lower, droopy)
    drawOval(color, Offset(center.x - eyeSpacing - eyeRadius, eyeY - eyeH / 2f + droop), Size(eyeRadius * 2f, eyeH))
    drawOval(color, Offset(center.x + eyeSpacing - eyeRadius, eyeY - eyeH / 2f + droop), Size(eyeRadius * 2f, eyeH))

    // Tear tracks (small lines below eyes)
    val tearColor = color.copy(alpha = color.alpha * 0.4f)
    drawLine(tearColor,
        Offset(center.x - eyeSpacing, eyeY + eyeRadius + droop),
        Offset(center.x - eyeSpacing - radius * 0.02f, eyeY + eyeRadius + droop + radius * 0.08f),
        strokeWidth = radius * 0.02f, cap = StrokeCap.Round)
    drawLine(tearColor,
        Offset(center.x + eyeSpacing, eyeY + eyeRadius + droop),
        Offset(center.x + eyeSpacing + radius * 0.02f, eyeY + eyeRadius + droop + radius * 0.08f),
        strokeWidth = radius * 0.02f, cap = StrokeCap.Round)

    // Frown arc
    drawArc(
        color = color,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(center.x - radius * 0.15f, mouthY),
        size = Size(radius * 0.30f, radius * 0.14f),
        style = Stroke(width = radius * 0.035f, cap = StrokeCap.Round)
    )
}

// ── ANGRY >:( ───────────────────────────────────────────────────────
// Sharp angular eyes with angry brows

private fun DrawScope.drawAngryFace(
    center: Offset, eyeSpacing: Float, eyeY: Float, mouthY: Float,
    eyeRadius: Float, blinkValue: Float, color: Color, radius: Float
) {
    val eyeH = eyeRadius * 2f * blinkValue

    // Angry eyes (slightly narrower)
    drawOval(color, Offset(center.x - eyeSpacing - eyeRadius, eyeY - eyeH / 2f), Size(eyeRadius * 2f, eyeH * 0.8f))
    drawOval(color, Offset(center.x + eyeSpacing - eyeRadius, eyeY - eyeH / 2f), Size(eyeRadius * 2f, eyeH * 0.8f))

    // Left brow (angled sharply down-inward) — thicker for emphasis
    drawLine(color,
        Offset(center.x - eyeSpacing - eyeRadius * 2f, eyeY - radius * 0.16f),
        Offset(center.x - eyeSpacing + eyeRadius * 1.5f, eyeY - radius * 0.06f),
        strokeWidth = radius * 0.05f, cap = StrokeCap.Round)
    // Right brow
    drawLine(color,
        Offset(center.x + eyeSpacing + eyeRadius * 2f, eyeY - radius * 0.16f),
        Offset(center.x + eyeSpacing - eyeRadius * 1.5f, eyeY - radius * 0.06f),
        strokeWidth = radius * 0.05f, cap = StrokeCap.Round)

    // Tight zigzag mouth (frustrated)
    val mouthLeft = center.x - radius * 0.12f
    val mouthRight = center.x + radius * 0.12f
    val mouthMid = center.x
    drawLine(color, Offset(mouthLeft, mouthY + radius * 0.02f), Offset(mouthMid, mouthY + radius * 0.06f),
        strokeWidth = radius * 0.04f, cap = StrokeCap.Round)
    drawLine(color, Offset(mouthMid, mouthY + radius * 0.06f), Offset(mouthRight, mouthY + radius * 0.02f),
        strokeWidth = radius * 0.04f, cap = StrokeCap.Round)
}

// ── EXCITED ★‿★ ─────────────────────────────────────────────────────
// Star-sparkle eyes + wide open smile

private fun DrawScope.drawExcitedFace(
    center: Offset, eyeSpacing: Float, eyeY: Float, mouthY: Float,
    eyeRadius: Float, blinkValue: Float, color: Color, radius: Float, alpha: Float
) {
    val starSize = eyeRadius * 2.2f

    // Star eyes — draw as 4-pointed star shapes
    drawStar(center.x - eyeSpacing, eyeY, starSize * blinkValue.coerceAtLeast(0.4f), color, radius)
    drawStar(center.x + eyeSpacing, eyeY, starSize * blinkValue.coerceAtLeast(0.4f), color, radius)

    // Eye sparkle dots
    drawCircle(
        color = Color.White.copy(alpha = alpha * 0.9f),
        radius = eyeRadius * 0.3f,
        center = Offset(center.x - eyeSpacing + starSize * 0.3f, eyeY - starSize * 0.3f)
    )
    drawCircle(
        color = Color.White.copy(alpha = alpha * 0.9f),
        radius = eyeRadius * 0.3f,
        center = Offset(center.x + eyeSpacing + starSize * 0.3f, eyeY - starSize * 0.3f)
    )

    // Wide smile
    drawArc(
        color = color,
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(center.x - radius * 0.22f, mouthY - radius * 0.10f),
        size = Size(radius * 0.44f, radius * 0.24f),
        style = Stroke(width = radius * 0.04f, cap = StrokeCap.Round)
    )
}

// ── SLEEPING -_- zZ ─────────────────────────────────────────────────

private fun DrawScope.drawSleepingFace(
    center: Offset, eyeSpacing: Float, eyeY: Float, mouthY: Float,
    eyeRadius: Float, color: Color, radius: Float, alpha: Float
) {
    // Closed eyes — horizontal lines
    drawLine(color,
        Offset(center.x - eyeSpacing - eyeRadius * 1.3f, eyeY),
        Offset(center.x - eyeSpacing + eyeRadius * 1.3f, eyeY),
        strokeWidth = radius * 0.035f, cap = StrokeCap.Round)
    drawLine(color,
        Offset(center.x + eyeSpacing - eyeRadius * 1.3f, eyeY),
        Offset(center.x + eyeSpacing + eyeRadius * 1.3f, eyeY),
        strokeWidth = radius * 0.035f, cap = StrokeCap.Round)

    // Tiny content mouth
    drawArc(
        color = color.copy(alpha = alpha * 0.5f),
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(center.x - radius * 0.08f, mouthY - radius * 0.02f),
        size = Size(radius * 0.16f, radius * 0.08f),
        style = Stroke(width = radius * 0.025f, cap = StrokeCap.Round)
    )
}

// ── LOVE ♡‿♡ ────────────────────────────────────────────────────────
// Heart-shaped eyes + gentle smile

private fun DrawScope.drawLoveFace(
    center: Offset, eyeSpacing: Float, eyeY: Float, mouthY: Float,
    eyeRadius: Float, color: Color, radius: Float, alpha: Float
) {
    val heartSize = eyeRadius * 2.5f

    // Heart eyes — draw using overlapping circles and triangle
    drawHeart(center.x - eyeSpacing, eyeY, heartSize, Color(0xFFFF6B8A).copy(alpha = alpha * 0.9f), radius)
    drawHeart(center.x + eyeSpacing, eyeY, heartSize, Color(0xFFFF6B8A).copy(alpha = alpha * 0.9f), radius)

    // Gentle smile
    drawArc(
        color = color,
        startAngle = 10f,
        sweepAngle = 160f,
        useCenter = false,
        topLeft = Offset(center.x - radius * 0.16f, mouthY - radius * 0.06f),
        size = Size(radius * 0.32f, radius * 0.16f),
        style = Stroke(width = radius * 0.035f, cap = StrokeCap.Round)
    )
}

// ── SHY >~< ─────────────────────────────────────────────────────────
// Squeezed shut eyes + wavy mouth

private fun DrawScope.drawShyFace(
    center: Offset, eyeSpacing: Float, eyeY: Float, mouthY: Float,
    eyeRadius: Float, color: Color, radius: Float, alpha: Float
) {
    // Squeezed eyes — downward arcs (>~< style, squeezed shut)
    val squeezeWidth = eyeRadius * 2.8f
    val squeezeHeight = eyeRadius * 1.5f

    // Left eye — > shape (two lines meeting at center)
    drawLine(color,
        Offset(center.x - eyeSpacing - squeezeWidth / 2f, eyeY - squeezeHeight / 2f),
        Offset(center.x - eyeSpacing + squeezeWidth * 0.1f, eyeY),
        strokeWidth = radius * 0.035f, cap = StrokeCap.Round)
    drawLine(color,
        Offset(center.x - eyeSpacing + squeezeWidth * 0.1f, eyeY),
        Offset(center.x - eyeSpacing - squeezeWidth / 2f, eyeY + squeezeHeight / 2f),
        strokeWidth = radius * 0.035f, cap = StrokeCap.Round)

    // Right eye — < shape
    drawLine(color,
        Offset(center.x + eyeSpacing + squeezeWidth / 2f, eyeY - squeezeHeight / 2f),
        Offset(center.x + eyeSpacing - squeezeWidth * 0.1f, eyeY),
        strokeWidth = radius * 0.035f, cap = StrokeCap.Round)
    drawLine(color,
        Offset(center.x + eyeSpacing - squeezeWidth * 0.1f, eyeY),
        Offset(center.x + eyeSpacing + squeezeWidth / 2f, eyeY + squeezeHeight / 2f),
        strokeWidth = radius * 0.035f, cap = StrokeCap.Round)

    // ~ wavy mouth
    val waveWidth = radius * 0.24f
    val segments = 8
    val waveAmplitude = radius * 0.03f
    val startX = center.x - waveWidth / 2f
    for (i in 0 until segments) {
        val x1 = startX + (i.toFloat() / segments) * waveWidth
        val x2 = startX + ((i + 1).toFloat() / segments) * waveWidth
        val y1 = mouthY + sin(i * PI.toFloat() / 2) * waveAmplitude
        val y2 = mouthY + sin((i + 1) * PI.toFloat() / 2) * waveAmplitude
        drawLine(color, Offset(x1, y1), Offset(x2, y2),
            strokeWidth = radius * 0.03f, cap = StrokeCap.Round)
    }
}

// ── CONFUSED o_O ────────────────────────────────────────────────────
// Asymmetric eyes (one small, one large)

private fun DrawScope.drawConfusedFace(
    center: Offset, eyeSpacing: Float, eyeY: Float, mouthY: Float,
    eyeRadius: Float, blinkValue: Float, color: Color, radius: Float
) {
    // Left eye — small (o)
    val smallEyeH = eyeRadius * 1.6f * blinkValue
    drawOval(color,
        Offset(center.x - eyeSpacing - eyeRadius * 0.8f, eyeY - smallEyeH / 2f),
        Size(eyeRadius * 1.6f, smallEyeH))
    // Left pupil
    drawCircle(color.copy(alpha = color.alpha * 0.6f),
        radius = eyeRadius * 0.3f,
        center = Offset(center.x - eyeSpacing, eyeY))

    // Right eye — large (O)
    val bigEyeRadius = eyeRadius * 1.8f
    val bigEyeH = bigEyeRadius * 2f * blinkValue
    drawOval(color,
        Offset(center.x + eyeSpacing - bigEyeRadius, eyeY - bigEyeH / 2f),
        Size(bigEyeRadius * 2f, bigEyeH))
    // Right pupil
    drawCircle(color.copy(alpha = color.alpha * 0.6f),
        radius = eyeRadius * 0.5f,
        center = Offset(center.x + eyeSpacing, eyeY))

    // Tilted line eyebrow on right side
    drawLine(color,
        Offset(center.x + eyeSpacing - bigEyeRadius, eyeY - radius * 0.16f),
        Offset(center.x + eyeSpacing + bigEyeRadius, eyeY - radius * 0.12f),
        strokeWidth = radius * 0.03f, cap = StrokeCap.Round)

    // Small "o" mouth
    drawCircle(
        color = color,
        radius = radius * 0.06f,
        center = Offset(center.x, mouthY + radius * 0.02f),
        style = Stroke(width = radius * 0.03f)
    )
}

// ── EVIL ψ(｀∇´)ψ ──────────────────────────────────────────────────
private fun DrawScope.drawEvilFace(
    center: Offset, eyeSpacing: Float, eyeY: Float, mouthY: Float,
    eyeRadius: Float, blinkValue: Float, color: Color, radius: Float,
    alpha: Float = 1.0f
) {
    val eyeH = eyeRadius * 1.8f * blinkValue
    val evilRed = Color(0xFFEF4444).copy(alpha = alpha)
    
    // Slanted menacing eyes (narrow slits)
    drawOval(evilRed, Offset(center.x - eyeSpacing - eyeRadius * 1.5f, eyeY - eyeH / 2f), Size(eyeRadius * 3f, eyeH))
    drawOval(evilRed, Offset(center.x + eyeSpacing - eyeRadius * 1.5f, eyeY - eyeH / 2f), Size(eyeRadius * 3f, eyeH))
    
    // Eyebrows slanted sharply down-in
    drawLine(Color.White.copy(alpha = alpha), Offset(center.x - eyeSpacing * 2f, eyeY - radius * 0.15f), Offset(center.x - eyeSpacing * 0.5f, eyeY - radius * 0.05f), strokeWidth = radius * 0.05f, cap = StrokeCap.Round)
    drawLine(Color.White.copy(alpha = alpha), Offset(center.x + eyeSpacing * 2f, eyeY - radius * 0.15f), Offset(center.x + eyeSpacing * 0.5f, eyeY - radius * 0.05f), strokeWidth = radius * 0.05f, cap = StrokeCap.Round)

    // Sharp wicked grin
    val mouthPath = Path().apply {
        moveTo(center.x - radius * 0.25f, mouthY)
        quadraticTo(center.x, mouthY + radius * 0.25f, center.x + radius * 0.25f, mouthY)
        lineTo(center.x + radius * 0.15f, mouthY + radius * 0.05f)
        lineTo(center.x - radius * 0.15f, mouthY + radius * 0.05f)
        close()
    }
    drawPath(mouthPath, Color.White.copy(alpha = alpha))
}

// ── JUGGLING ◎‿◎ ────────────────────────────────────────────────────
private fun DrawScope.drawJugglingFace(
    center: Offset, eyeSpacing: Float, eyeY: Float, mouthY: Float,
    eyeRadius: Float, blinkValue: Float, color: Color, radius: Float,
    alpha: Float = 1.0f
) {
    // Wide starry eyes
    drawStar(center.x - eyeSpacing, eyeY, eyeRadius * 2.5f * blinkValue, Color.White.copy(alpha = alpha), radius)
    drawStar(center.x + eyeSpacing, eyeY, eyeRadius * 2.5f * blinkValue, Color.White.copy(alpha = alpha), radius)

    // Bouncing wide mouth
    val bounceMouthY = mouthY + sin(System.currentTimeMillis() / 200f) * radius * 0.05f
    drawArc(
        color = Color.White.copy(alpha = alpha),
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(center.x - radius * 0.2f, bounceMouthY - radius * 0.1f),
        size = Size(radius * 0.4f, radius * 0.25f),
        style = Stroke(width = radius * 0.04f, cap = StrokeCap.Round)
    )
}

// ═══════════════════════════════════════════════════════════════════════
// HELPER DRAWING FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════

/**
 * Draw a simple 4-pointed star at (cx, cy).
 */
private fun DrawScope.drawStar(cx: Float, cy: Float, size: Float, color: Color, radius: Float) {
    val strokeW = radius * 0.035f
    // Vertical line
    drawLine(color, Offset(cx, cy - size), Offset(cx, cy + size), strokeWidth = strokeW, cap = StrokeCap.Round)
    // Horizontal line
    drawLine(color, Offset(cx - size, cy), Offset(cx + size, cy), strokeWidth = strokeW, cap = StrokeCap.Round)
    // Diagonals (smaller)
    val diagSize = size * 0.65f
    drawLine(color, Offset(cx - diagSize, cy - diagSize), Offset(cx + diagSize, cy + diagSize), strokeWidth = strokeW * 0.7f, cap = StrokeCap.Round)
    drawLine(color, Offset(cx + diagSize, cy - diagSize), Offset(cx - diagSize, cy + diagSize), strokeWidth = strokeW * 0.7f, cap = StrokeCap.Round)
}

/**
 * Draw a simple heart shape at (cx, cy) using circles and a triangle path.
 */
private fun DrawScope.drawHeart(cx: Float, cy: Float, size: Float, color: Color, radius: Float) {
    val halfSize = size / 2f
    val topOffset = size * 0.3f

    // Two circles forming the top bumps
    drawCircle(color, radius = halfSize * 0.55f, center = Offset(cx - halfSize * 0.35f, cy - topOffset))
    drawCircle(color, radius = halfSize * 0.55f, center = Offset(cx + halfSize * 0.35f, cy - topOffset))

    // Triangle bottom (using a path)
    val path = Path().apply {
        moveTo(cx - halfSize * 0.72f, cy - topOffset + halfSize * 0.1f)
        lineTo(cx, cy + halfSize * 0.6f)
        lineTo(cx + halfSize * 0.72f, cy - topOffset + halfSize * 0.1f)
        close()
    }
    drawPath(path, color)
}

// ═══════════════════════════════════════════════════════════════════════
// PARTICLE EFFECTS
// ═══════════════════════════════════════════════════════════════════════

private fun DrawScope.drawSparkParticles(
    center: Offset, radius: Float, phase: Float, color: Color
) {
    val sparkCount = 8
    for (i in 0 until sparkCount) {
        val angle = (i * 360f / sparkCount + phase * 360f) * PI.toFloat() / 180f
        val dist = radius * (1.1f + phase * 0.5f)
        val sparkAlpha = (1f - phase).coerceIn(0f, 1f) * 0.8f
        val sparkSize = radius * 0.06f * (1f - phase * 0.5f)

        val sparkX = center.x + cos(angle) * dist
        val sparkY = center.y + sin(angle) * dist

        drawCircle(
            color = color.copy(alpha = sparkAlpha),
            radius = sparkSize,
            center = Offset(sparkX, sparkY)
        )
    }
}

/**
 * Floating heart particles rising from the sphere (LOVE state).
 */
private fun DrawScope.drawHeartParticles(
    center: Offset, radius: Float, phase: Float, color: Color
) {
    val heartCount = 3
    for (i in 0 until heartCount) {
        val offset = i.toFloat() / heartCount
        val p = (phase + offset) % 1f
        val alpha = (1f - p).coerceIn(0f, 1f) * 0.5f
        val heartSize = radius * 0.08f * (0.5f + p * 0.5f)

        // Rise upward and drift slightly sideways
        val hx = center.x + sin(p * PI.toFloat() * 2f + i * 2f) * radius * 0.4f
        val hy = center.y - radius * (0.8f + p * 1.2f)

        // Simple heart as two overlapping circles + triangle
        val heartColor = color.copy(alpha = alpha)
        drawCircle(heartColor, radius = heartSize * 0.45f, center = Offset(hx - heartSize * 0.25f, hy - heartSize * 0.15f))
        drawCircle(heartColor, radius = heartSize * 0.45f, center = Offset(hx + heartSize * 0.25f, hy - heartSize * 0.15f))
        val path = Path().apply {
            moveTo(hx - heartSize * 0.55f, hy)
            lineTo(hx, hy + heartSize * 0.6f)
            lineTo(hx + heartSize * 0.55f, hy)
            close()
        }
        drawPath(path, heartColor)
    }
}

private fun DrawScope.drawSleepEffect(
    center: Offset, radius: Float, phase: Float, color: Color
) {
    val zOffset = phase * radius * 0.8f
    val zAlpha = (1f - phase).coerceIn(0f, 1f) * 0.5f
    val zX = center.x + radius * 0.5f
    val zY = center.y - radius * 0.3f - zOffset
    val zSize = radius * 0.12f * (0.6f + phase * 0.4f)

    // Z shape
    drawLine(color.copy(alpha = zAlpha), Offset(zX - zSize, zY - zSize), Offset(zX + zSize, zY - zSize),
        strokeWidth = radius * 0.025f, cap = StrokeCap.Round)
    drawLine(color.copy(alpha = zAlpha), Offset(zX + zSize, zY - zSize), Offset(zX - zSize, zY + zSize),
        strokeWidth = radius * 0.025f, cap = StrokeCap.Round)
    drawLine(color.copy(alpha = zAlpha), Offset(zX - zSize, zY + zSize), Offset(zX + zSize, zY + zSize),
        strokeWidth = radius * 0.025f, cap = StrokeCap.Round)

    // Second smaller Z
    val z2Offset = (phase + 0.4f) % 1f
    val z2Alpha = (1f - z2Offset).coerceIn(0f, 1f) * 0.3f
    val z2X = zX + radius * 0.15f
    val z2Y = center.y - radius * 0.5f - z2Offset * radius * 0.6f
    val z2Size = zSize * 0.7f
    drawLine(color.copy(alpha = z2Alpha), Offset(z2X - z2Size, z2Y - z2Size), Offset(z2X + z2Size, z2Y - z2Size),
        strokeWidth = radius * 0.02f, cap = StrokeCap.Round)
    drawLine(color.copy(alpha = z2Alpha), Offset(z2X + z2Size, z2Y - z2Size), Offset(z2X - z2Size, z2Y + z2Size),
        strokeWidth = radius * 0.02f, cap = StrokeCap.Round)
    drawLine(color.copy(alpha = z2Alpha), Offset(z2X - z2Size, z2Y + z2Size), Offset(z2X + z2Size, z2Y + z2Size),
        strokeWidth = radius * 0.02f, cap = StrokeCap.Round)
}

/**
 * Floating question/exclamation marks for CONFUSED state.
 */
private fun DrawScope.drawConfusedEffect(
    center: Offset, radius: Float, phase: Float, color: Color
) {
    val qAlpha = (sin(phase * PI.toFloat() * 2f) * 0.5f + 0.5f) * 0.4f
    val qX = center.x + radius * 0.55f
    val qY = center.y - radius * 0.6f - sin(phase * PI.toFloat()) * radius * 0.1f
    val qSize = radius * 0.1f

    // ? mark — arc + dot
    drawArc(
        color = color.copy(alpha = qAlpha),
        startAngle = 180f,
        sweepAngle = 270f,
        useCenter = false,
        topLeft = Offset(qX - qSize, qY - qSize * 1.5f),
        size = Size(qSize * 2f, qSize * 2f),
        style = Stroke(width = radius * 0.025f, cap = StrokeCap.Round)
    )
    drawLine(
        color = color.copy(alpha = qAlpha),
        start = Offset(qX + qSize, qY + qSize * 0.5f),
        end = Offset(qX, qY + qSize * 1.2f),
        strokeWidth = radius * 0.025f,
        cap = StrokeCap.Round
    )
    drawCircle(
        color = color.copy(alpha = qAlpha),
        radius = radius * 0.02f,
        center = Offset(qX, qY + qSize * 1.6f)
    )
}

// ═══════════════════════════════════════════════════════════════════════
// ACCESSORY DRAWING
// ═══════════════════════════════════════════════════════════════════════

private fun DrawScope.drawAccessory(accessory: CompanionAccessory, center: Offset, radius: Float) {
    when (accessory) {
        CompanionAccessory.NONE -> {}
        CompanionAccessory.HAT -> drawHat(center, radius)
        CompanionAccessory.RIBBON -> drawRibbon(center, radius)
        CompanionAccessory.GLASSES -> drawGlasses(center, radius)
        CompanionAccessory.CROWN -> drawCrown(center, radius)
        CompanionAccessory.STAR -> drawAccessoryStar(center, radius)
    }
}

private fun DrawScope.drawHat(center: Offset, radius: Float) {
    val hatY = center.y - radius * 0.85f
    val hatColor = Color(0xFF1E293B)
    val bandColor = Color(0xFFEF4444)

    // Brim
    drawOval(
        color = hatColor,
        topLeft = Offset(center.x - radius * 0.5f, hatY),
        size = Size(radius, radius * 0.2f)
    )
    
    // Top
    val topPath = Path().apply {
        moveTo(center.x - radius * 0.3f, hatY + radius * 0.1f)
        lineTo(center.x - radius * 0.25f, hatY - radius * 0.4f)
        lineTo(center.x + radius * 0.25f, hatY - radius * 0.4f)
        lineTo(center.x + radius * 0.3f, hatY + radius * 0.1f)
        close()
    }
    drawPath(topPath, hatColor)

    // Band
    val bandPath = Path().apply {
        moveTo(center.x - radius * 0.29f, hatY - radius * 0.05f)
        lineTo(center.x - radius * 0.28f, hatY - radius * 0.15f)
        lineTo(center.x + radius * 0.28f, hatY - radius * 0.15f)
        lineTo(center.x + radius * 0.29f, hatY - radius * 0.05f)
        close()
    }
    drawPath(bandPath, bandColor)
}

private fun DrawScope.drawRibbon(center: Offset, radius: Float) {
    val rY = center.y - radius * 0.7f
    val rX = center.x + radius * 0.4f
    val ribbonColor = Color(0xFFEC4899)
    val ribbonSize = radius * 0.3f

    rotate(25f, Offset(rX, rY)) {
        // Left loop
        drawOval(ribbonColor, Offset(rX - ribbonSize, rY - ribbonSize * 0.4f), Size(ribbonSize, ribbonSize * 0.8f))
        // Right loop
        drawOval(ribbonColor, Offset(rX, rY - ribbonSize * 0.4f), Size(ribbonSize, ribbonSize * 0.8f))
        // Center knot
        drawCircle(ribbonColor, radius = ribbonSize * 0.3f, center = Offset(rX, rY))
    }
}

private fun DrawScope.drawGlasses(center: Offset, radius: Float) {
    val eyeSpacing = radius * 0.28f
    val eyeY = center.y - radius * 0.08f
    val glassStyle = Stroke(width = radius * 0.04f)
    val frameColor = Color(0xFF334155)

    // Left lens
    drawCircle(frameColor, radius = radius * 0.20f, center = Offset(center.x - eyeSpacing, eyeY), style = glassStyle)
    // Right lens
    drawCircle(frameColor, radius = radius * 0.20f, center = Offset(center.x + eyeSpacing, eyeY), style = glassStyle)
    // Bridge
    drawLine(frameColor, Offset(center.x - eyeSpacing + radius * 0.20f, eyeY), Offset(center.x + eyeSpacing - radius * 0.20f, eyeY), strokeWidth = radius * 0.04f)
}

private fun DrawScope.drawCrown(center: Offset, radius: Float) {
    val cY = center.y - radius * 0.9f
    val crownColor = Color(0xFFFBBF24)

    val path = Path().apply {
        moveTo(center.x - radius * 0.35f, cY + radius * 0.1f)
        lineTo(center.x - radius * 0.4f, cY - radius * 0.3f)
        lineTo(center.x - radius * 0.15f, cY - radius * 0.1f)
        lineTo(center.x, cY - radius * 0.4f)
        lineTo(center.x + radius * 0.15f, cY - radius * 0.1f)
        lineTo(center.x + radius * 0.4f, cY - radius * 0.3f)
        lineTo(center.x + radius * 0.35f, cY + radius * 0.1f)
        close()
    }
    drawPath(path, crownColor)

    // Jewels
    drawCircle(Color(0xFFEF4444), radius = radius * 0.04f, center = Offset(center.x - radius * 0.4f, cY - radius * 0.32f))
    drawCircle(Color(0xFF38BDF8), radius = radius * 0.05f, center = Offset(center.x, cY - radius * 0.43f))
    drawCircle(Color(0xFFEF4444), radius = radius * 0.04f, center = Offset(center.x + radius * 0.4f, cY - radius * 0.32f))
}

private fun DrawScope.drawAccessoryStar(center: Offset, radius: Float) {
    val sY = center.y - radius * 0.7f
    val sX = center.x - radius * 0.5f
    val starColor = Color(0xFFFBBF24)
    rotate(-15f, Offset(sX, sY)) {
        drawStar(sX, sY, radius * 0.25f, starColor, radius)
    }
}

// ═══════════════════════════════════════════════════════════════════════
// SPECIAL BURST VISUALS
// ═══════════════════════════════════════════════════════════════════════

private fun DrawScope.drawEvilAura(center: Offset, radius: Float, pulse: Float) {
    // Massive black "engulfing" circle behind the sphere
    val auraRadius = radius * (1.5f + pulse * 0.8f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.Black, Color.Black.copy(alpha = 0.8f), Color.Transparent),
            center = center,
            radius = auraRadius
        ),
        radius = auraRadius,
        center = center
    )
    
    // Sinister red ring
    drawCircle(
        color = Color(0xFFEF4444).copy(alpha = 0.5f * (1f - pulse)),
        radius = radius * (1.2f + pulse * 0.5f),
        center = center,
        style = Stroke(width = radius * 0.1f)
    )
}

private fun DrawScope.drawSmokeParticles(center: Offset, radius: Float, phase: Float) {
    val count = 6
    for (i in 0 until count) {
        val p = (phase + i.toFloat() / count) % 1f
        val angle = (i * 360f / count) * PI.toFloat() / 180f
        val dist = radius * (1f + p * 1.5f)
        val particleSize = radius * 0.2f * (1f - p)
        val alpha = (1f - p) * 0.4f
        
        drawCircle(
            color = Color.Black.copy(alpha = alpha),
            radius = particleSize,
            center = Offset(center.x + cos(angle) * dist, center.y + sin(angle) * dist)
        )
    }
}

private fun DrawScope.drawJugglingBalls(center: Offset, radius: Float, phase: Float) {
    val colors = listOf(Color(0xFFFBBF24), Color(0xFF06B6D4), Color(0xFFEC4899))
    val orbitRadius = radius * 1.4f
    
    colors.forEachIndexed { i, color ->
        val angle = (phase * 360f + i * (360f / colors.size)) * PI.toFloat() / 180f
        val x = center.x + cos(angle) * orbitRadius
        val y = center.y + sin(angle) * orbitRadius + sin(phase * PI.toFloat() * 2f + i) * radius * 0.2f
        
        // Glow for the ball
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.8f), Color.Transparent),
                center = Offset(x, y),
                radius = radius * 0.25f
            ),
            radius = radius * 0.25f,
            center = Offset(x, y)
        )
        
        // The ball itself
        drawCircle(color, radius = radius * 0.1f, center = Offset(x, y))
    }
}

package com.funtime.sciai.aisphere

/**
 * Behavior Engine — the "brain" of the AI Sphere.
 * Decides the current emotion state based on user interactions,
 * affection level, time, and context.
 *
 * Enhanced with:
 * - Affection-based emotion modifiers
 * - Rapid tap → ANGRY detection
 * - Double tap → SHY reaction
 * - Long hold → LOVE
 * - Emotion blending paths
 */
object BehaviorEngine {

    // ── Timing thresholds (milliseconds) ────────────────────────────
    private const val IDLE_TO_SAD_MS = 2 * 60 * 1000L
    private const val IDLE_TO_SLEEP_MS = 5 * 60 * 1000L
    private const val MESSAGE_COOLDOWN_MS = 2 * 60 * 1000L
    private const val IDLE_MESSAGE_MS = 30 * 1000L
    private const val SESSION_ENCOURAGE_MS = 15 * 60 * 1000L

    // ── Emotion evaluation ──────────────────────────────────────────

    /**
     * Evaluate the current emotion state based on all inputs.
     * Priority order: Override emotion > Petting > Recent gesture > Idle time > Default
     *
     * @param overrideEmotion forced emotion from rapid-tap/double-tap (nullable)
     * @param overrideAge how long ago the override was set
     */
    fun evaluateEmotion(
        petHappiness: Float,
        affectionLevel: Float,
        timeSinceLastInteraction: Long,
        lastGesture: GestureType,
        gestureAge: Long,
        overrideEmotion: EmotionState? = null,
        overrideAge: Long = Long.MAX_VALUE
    ): EmotionState {
        // Override emotions (from rapid-tap, double-tap, etc.) last ~3 seconds
        if (overrideEmotion != null && overrideAge < 3000L) {
            return overrideEmotion
        }

        // Petting overrides
        if (petHappiness > 0.8f) {
            // High affection + high happiness = LOVE instead of EXCITED
            return if (affectionLevel > 0.6f) EmotionState.LOVE else EmotionState.EXCITED
        }
        if (petHappiness > 0.4f) return EmotionState.HAPPY

        // Long press → LOVE (within 3 seconds)
        if (lastGesture == GestureType.LONG_PRESS && gestureAge < 3000L) {
            return if (affectionLevel > 0.3f) EmotionState.LOVE else EmotionState.HAPPY
        }

        // Recent tap
        if (lastGesture == GestureType.TAP && gestureAge < 3000L) {
            return EmotionState.HAPPY
        }

        // Idle-based transitions
        if (timeSinceLastInteraction > IDLE_TO_SLEEP_MS) return EmotionState.SLEEP
        if (timeSinceLastInteraction > IDLE_TO_SAD_MS) return EmotionState.SAD

        return EmotionState.IDLE
    }

    /**
     * Determine if a message should be shown and what type.
     */
    fun evaluateMessageTrigger(
        timeSinceLastInteraction: Long,
        timeSinceLastMessage: Long,
        lastGesture: GestureType,
        gestureAge: Long,
        currentScreen: String,
        sessionDuration: Long,
        isFirstVisitToScreen: Boolean,
        isMuted: Boolean,
        totalInteractions: Int
    ): MessageContext? {
        if (isMuted) return null
        if (timeSinceLastMessage < MESSAGE_COOLDOWN_MS) return null

        if (lastGesture == GestureType.PET && gestureAge < 1000L) {
            return MessageContext.PETTING
        }

        if (isFirstVisitToScreen) {
            return MessageContext.TIP
        }

        if (sessionDuration > SESSION_ENCOURAGE_MS && totalInteractions > 5) {
            return MessageContext.ENCOURAGEMENT
        }

        if (timeSinceLastInteraction > IDLE_MESSAGE_MS) {
            return when {
                currentScreen.startsWith("home") -> MessageContext.HELP
                currentScreen.startsWith("library") -> MessageContext.TIP
                currentScreen.startsWith("articles") -> MessageContext.TIP
                else -> MessageContext.IDLE
            }
        }

        return null
    }

    /**
     * Calculate happiness decay amount based on time elapsed.
     */
    fun calculateHappinessDecay(timeSinceLastPet: Long): Float {
        val decayStart = 5000L
        if (timeSinceLastPet < decayStart) return 0f
        val decayTime = timeSinceLastPet - decayStart
        return (decayTime / 1000f * 0.01f).coerceAtMost(0.05f)
    }

    /**
     * Calculate affection growth from an interaction.
     * Affection grows slowly and decays very slowly — it's a long-term bond.
     */
    fun calculateAffectionGrowth(gesture: GestureType): Float = when (gesture) {
        GestureType.PET -> 0.03f
        GestureType.TAP -> 0.005f
        GestureType.LONG_PRESS -> 0.02f
        GestureType.DRAG -> 0.001f
        GestureType.DOUBLE_TAP -> 0.01f
        GestureType.NONE -> 0f
    }

    /**
     * Affection decays very slowly — 0.001 per minute.
     */
    fun calculateAffectionDecay(timeSinceLastInteraction: Long): Float {
        val decayStartMs = 10 * 60 * 1000L  // Start decaying after 10 minutes
        if (timeSinceLastInteraction < decayStartMs) return 0f
        val decayMinutes = (timeSinceLastInteraction - decayStartMs) / 60000f
        return (decayMinutes * 0.001f).coerceAtMost(0.01f)
    }
}

/**
 * Types of gestures the sphere can receive.
 */
enum class GestureType {
    NONE, TAP, LONG_PRESS, DRAG, PET, DOUBLE_TAP
}

package com.funtime.sciai.aisphere

import android.content.Context
import android.content.SharedPreferences

/**
 * Persistence layer for AI Sphere settings and interaction history.
 * Uses SharedPreferences for lightweight local storage.
 *
 * Handles migration from FUNNY → PLAYFUL personality mode.
 */
class AISpherePreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ai_sphere_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ENABLED = "sphere_enabled"
        private const val KEY_MUTED = "sphere_muted"
        private const val KEY_SOUND_ENABLED = "sphere_sound_enabled"
        private const val KEY_REDUCE_ANIMATIONS = "sphere_reduce_animations"
        private const val KEY_POSITION_X = "sphere_pos_x"
        private const val KEY_POSITION_Y = "sphere_pos_y"
        private const val KEY_PERSONALITY = "sphere_personality"
        private const val KEY_TOTAL_TAPS = "sphere_total_taps"
        private const val KEY_TOTAL_PETS = "sphere_total_pets"
        private const val KEY_TOTAL_INTERACTIONS = "sphere_total_interactions"
        private const val KEY_LAST_INTERACTION_TIME = "sphere_last_interaction"
        private const val KEY_HAPPINESS_LEVEL = "sphere_happiness"
        private const val KEY_AFFECTION_LEVEL = "sphere_affection"
        private const val KEY_FIRST_LAUNCH = "sphere_first_launch"
        private const val KEY_LAST_VISIT_TIME = "sphere_last_visit"
    }

    // ── Master toggles ──────────────────────────────────────────────

    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    var isMuted: Boolean
        get() = prefs.getBoolean(KEY_MUTED, false)
        set(value) = prefs.edit().putBoolean(KEY_MUTED, value).apply()

    var isSoundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()

    var reduceAnimations: Boolean
        get() = prefs.getBoolean(KEY_REDUCE_ANIMATIONS, false)
        set(value) = prefs.edit().putBoolean(KEY_REDUCE_ANIMATIONS, value).apply()

    // ── Position persistence ────────────────────────────────────────

    var positionX: Float
        get() = prefs.getFloat(KEY_POSITION_X, -1f)
        set(value) = prefs.edit().putFloat(KEY_POSITION_X, value).apply()

    var positionY: Float
        get() = prefs.getFloat(KEY_POSITION_Y, -1f)
        set(value) = prefs.edit().putFloat(KEY_POSITION_Y, value).apply()

    // ── Personality ─────────────────────────────────────────────────

    var personalityMode: PersonalityMode
        get() {
            val name = prefs.getString(KEY_PERSONALITY, PersonalityMode.PLAYFUL.name)
            return try {
                // Migration: FUNNY → PLAYFUL
                when (name) {
                    "FUNNY" -> PersonalityMode.PLAYFUL
                    else -> PersonalityMode.valueOf(name ?: PersonalityMode.PLAYFUL.name)
                }
            } catch (_: Exception) {
                PersonalityMode.PLAYFUL
            }
        }
        set(value) = prefs.edit().putString(KEY_PERSONALITY, value.name).apply()

    // ── Interaction history ─────────────────────────────────────────

    var totalTaps: Int
        get() = prefs.getInt(KEY_TOTAL_TAPS, 0)
        set(value) = prefs.edit().putInt(KEY_TOTAL_TAPS, value).apply()

    var totalPets: Int
        get() = prefs.getInt(KEY_TOTAL_PETS, 0)
        set(value) = prefs.edit().putInt(KEY_TOTAL_PETS, value).apply()

    var totalInteractions: Int
        get() = prefs.getInt(KEY_TOTAL_INTERACTIONS, 0)
        set(value) = prefs.edit().putInt(KEY_TOTAL_INTERACTIONS, value).apply()

    var lastInteractionTime: Long
        get() = prefs.getLong(KEY_LAST_INTERACTION_TIME, System.currentTimeMillis())
        set(value) = prefs.edit().putLong(KEY_LAST_INTERACTION_TIME, value).apply()

    var happinessLevel: Float
        get() = prefs.getFloat(KEY_HAPPINESS_LEVEL, 0.3f)
        set(value) = prefs.edit().putFloat(KEY_HAPPINESS_LEVEL, value.coerceIn(0f, 1f)).apply()

    var affectionLevel: Float
        get() = prefs.getFloat(KEY_AFFECTION_LEVEL, 0.1f)
        set(value) = prefs.edit().putFloat(KEY_AFFECTION_LEVEL, value.coerceIn(0f, 1f)).apply()

    var isFirstLaunch: Boolean
        get() = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
        set(value) = prefs.edit().putBoolean(KEY_FIRST_LAUNCH, value).apply()

    var lastVisitTime: Long
        get() = prefs.getLong(KEY_LAST_VISIT_TIME, System.currentTimeMillis())
        set(value) = prefs.edit().putLong(KEY_LAST_VISIT_TIME, value).apply()

    // ── Utility ─────────────────────────────────────────────────────

    fun recordTap() {
        totalTaps++
        totalInteractions++
        lastInteractionTime = System.currentTimeMillis()
    }

    fun recordPet() {
        totalPets++
        totalInteractions++
        lastInteractionTime = System.currentTimeMillis()
        happinessLevel = (happinessLevel + 0.15f).coerceAtMost(1f)
    }

    fun recordInteraction() {
        totalInteractions++
        lastInteractionTime = System.currentTimeMillis()
    }

    fun decayHappiness() {
        happinessLevel = (happinessLevel - 0.01f).coerceAtLeast(0f)
    }

    /**
     * Update affection level based on gesture type.
     */
    fun recordAffection(gesture: GestureType) {
        val growth = BehaviorEngine.calculateAffectionGrowth(gesture)
        affectionLevel = (affectionLevel + growth).coerceAtMost(1f)
    }
}

package com.funtime.sciai.aisphere

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
/**
 * Sound Effect Manager for the AI Sphere.
 * Plays cute emotional vocalizations when the sphere reacts to interactions.
 *
 * Uses SoundPool for low-latency playback of short audio clips.
 * Respects mute setting and adjusts volume based on emotion intensity.
 *
 * Expected raw resources:
 * - res/raw/sfx_happy.ogg
 * - res/raw/sfx_love.ogg
 * - res/raw/sfx_shy.ogg
 * - res/raw/sfx_sad.ogg
 * - res/raw/sfx_angry.ogg
 * - res/raw/sfx_sleep.ogg
 * - res/raw/sfx_excited.ogg
 */
class SoundEffectManager(private val context: Context) {

    private var soundPool: SoundPool? = null
    private val soundIds = mutableMapOf<EmotionState, Int>()
    private var isLoaded = false
    private var isSoundEnabled = true

    companion object {
        private const val TAG = "SoundEffectManager"
        private const val MAX_STREAMS = 3

        // Minimum interval between sounds to avoid spam (ms)
        private const val MIN_SOUND_INTERVAL_MS = 1500L
    }

    private var lastSoundTime = 0L

    /**
     * Initialize the SoundPool and load all emotion sounds.
     * Call this once during app startup.
     */
    fun initialize() {
        if (soundPool != null) return

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(MAX_STREAMS)
            .setAudioAttributes(attributes)
            .build()
            .also { pool ->
                pool.setOnLoadCompleteListener { _, _, status ->
                    if (status == 0) {
                        isLoaded = true
                    }
                }
            }

        // Load sounds — gracefully handle missing resources
        loadSound(EmotionState.HAPPY, "sfx_happy")
        loadSound(EmotionState.LOVE, "sfx_love")
        loadSound(EmotionState.SHY, "sfx_shy")
        loadSound(EmotionState.SAD, "sfx_sad")
        loadSound(EmotionState.ANGRY, "sfx_angry")
        loadSound(EmotionState.SLEEP, "sfx_sleep")
        loadSound(EmotionState.EXCITED, "sfx_excited")
    }

    private fun loadSound(emotion: EmotionState, resourceName: String) {
        try {
            val resId = context.resources.getIdentifier(resourceName, "raw", context.packageName)
            if (resId != 0) {
                val soundId = soundPool?.load(context, resId, 1) ?: return
                soundIds[emotion] = soundId
            } else {
                Log.d(TAG, "Sound resource not found: $resourceName — will play silently for $emotion")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load sound for $emotion: ${e.message}")
        }
    }

    /**
     * Play the sound for the given emotion state.
     * Respects mute setting and minimum interval.
     *
     * @param emotion The emotion to play a sound for
     * @param volume Volume multiplier (0..1), based on emotion intensity
     */
    fun playEmotionSound(emotion: EmotionState, volume: Float = 0.7f) {
        if (!isSoundEnabled || !isLoaded) return

        val now = System.currentTimeMillis()
        if (now - lastSoundTime < MIN_SOUND_INTERVAL_MS) return
        lastSoundTime = now

        val soundId = soundIds[emotion] ?: return
        val clampedVolume = volume.coerceIn(0.1f, 1.0f)

        // Slight pitch variation for naturalness (not robotic)
        val pitch = 0.95f + (Math.random().toFloat() * 0.1f) // 0.95–1.05

        soundPool?.play(
            soundId,
            clampedVolume,       // left volume
            clampedVolume,       // right volume
            1,                   // priority
            0,                   // loop (0 = no loop)
            pitch                // playback rate
        )
    }

    /**
     * Play sound on emotion change — only plays for "reactive" emotions
     * (not IDLE or transitions that would feel spammy).
     */
    fun onEmotionChanged(newEmotion: EmotionState) {
        when (newEmotion) {
            EmotionState.HAPPY -> playEmotionSound(EmotionState.HAPPY, 0.6f)
            EmotionState.LOVE -> playEmotionSound(EmotionState.LOVE, 0.7f)
            EmotionState.SHY -> playEmotionSound(EmotionState.SHY, 0.5f)
            EmotionState.SAD -> playEmotionSound(EmotionState.SAD, 0.4f)
            EmotionState.ANGRY -> playEmotionSound(EmotionState.ANGRY, 0.8f)
            EmotionState.SLEEP -> playEmotionSound(EmotionState.SLEEP, 0.3f)
            EmotionState.EXCITED -> playEmotionSound(EmotionState.EXCITED, 0.9f)
            EmotionState.CONFUSED -> {} // No specific sound for confused
            EmotionState.IDLE -> {} // Don't play sound for idle
            // ✅ Add these
            EmotionState.PLAYFUL_EVIL -> playEmotionSound(EmotionState.PLAYFUL_EVIL, volume = 0.7f)
            EmotionState.JUGGLING -> playEmotionSound(EmotionState.JUGGLING, volume = 0.6f)
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        isSoundEnabled = enabled
    }

    /**
     * Release all resources. Call in ViewModel.onCleared().
     */
    fun release() {
        soundPool?.release()
        soundPool = null
        soundIds.clear()
        isLoaded = false
    }
}

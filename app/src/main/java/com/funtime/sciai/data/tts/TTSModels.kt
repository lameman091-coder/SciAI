package com.funtime.sciai.data.tts

import com.google.gson.annotations.SerializedName

/**
 * TTS request sent to backend /api/v1/tts endpoint.
 */
data class TTSRequest(
    val text: String,
    @SerializedName("voice_style") val voiceStyle: String = "professor",
    val mode: String = "Concept",
    val speed: Float = 1.0f,
    @SerializedName("use_ssml") val useSsml: Boolean = false
)

/**
 * TTS fallback response when backend engines are unavailable (503).
 * Contains optimized text for Android TTS fallback.
 */
data class TTSFallbackResponse(
    val detail: String = "",
    val fallback: String = "",
    @SerializedName("optimized_text") val optimizedText: String = ""
)

/**
 * TTS engine status response from /api/v1/tts/status
 */
data class TTSStatusResponse(
    val kyutai: TTSProviderStatus = TTSProviderStatus(),
    val huggingface: TTSProviderStatus = TTSProviderStatus()
)

data class TTSProviderStatus(
    val loaded: Boolean = false,
    val voices: List<String> = emptyList()
)

/**
 * Voice style options for TTS.
 */
enum class VoiceStyle(val id: String, val displayName: String, val gender: String, val emoji: String, val description: String) {
    PROFESSOR("professor", "Academic Professor", "Male", "🎓", "Calm & authoritative"),
    ENERGETIC("energetic", "Energetic Guide", "Female", "⚡", "Lively & motivating"),
    STORYTELLER("storyteller", "Storyteller", "Female", "📖", "Warm & narrative"),
    FORMAL_MALE("formal_male", "News Anchor", "Male", "👔", "Crisp & professional"),
    CASUAL_FEMALE("casual_female", "Friendly Assistant", "Female", "👋", "Upbeat & casual"),
    DEEP_MALE("deep_male", "Deep Narrator", "Male", "🎙️", "Resonant & dramatic"),
    WARM_FEMALE("warm_female", "Warm Mentor", "Female", "🌸", "Patient & encouraging");

    companion object {
        fun fromId(id: String): VoiceStyle = entries.find { it.id == id } ?: PROFESSOR
    }
}

/**
 * Represents a saved/downloaded TTS track for the playlist.
 */
data class Track(
    val id: String,              // Unique ID (often answerId)
    val title: String,           // The question/query
    val text: String = "",        // The full answer content
    val mode: String,            // Exam/Concept/Expert
    val voiceStyleId: String,    // Voice personality used
    val filePath: String = "",    // Local filesystem path
    val timestamp: Long,         // When it was saved
    val duration: Long = 0,       // Track length in ms
    val isDownloaded: Boolean = false
)

/**
 * TTS playback state machine.
 */
enum class TTSState {
    IDLE,       // No audio loaded
    LOADING,    // Fetching from backend
    PLAYING,    // Audio playing
    PAUSED,     // Playback paused
    ERROR       // Something failed
}

package com.funtime.sciai.data

import android.content.Context

class UserManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("sciai_prefs", Context.MODE_PRIVATE)

    /**
     * Returns a persistent device-specific user ID.
     * Generated once via UUID and stored in SharedPreferences.
     */
    fun getUserId(): String {
        val existing = prefs.getString("USER_ID", null)
        if (existing != null) return existing
        val newId = java.util.UUID.randomUUID().toString()
        prefs.edit().putString("USER_ID", newId).apply()
        return newId
    }

    fun saveName(name: String) {
        prefs.edit().putString("username", name).apply()
    }

    fun getName(): String? {
        return prefs.getString("username", null)
    }

    fun incrementTotal() {
        val current = getTotal()
        prefs.edit().putInt("total", current + 1).apply()
    }

    fun getTotal(): Int {
        return prefs.getInt("total", 0)
    }

    fun resetTotal() {
        prefs.edit().putInt("total", 0).apply()
    }

    // ── Theme Persistence ──

    fun setDarkMode(isDark: Boolean) {
        prefs.edit().putBoolean("THEME_DARK", isDark).apply()
    }

    fun isDarkMode(): Boolean {
        // Default to true (Dark Mode) for premium feel if not set
        return prefs.getBoolean("THEME_DARK", true)
    }

    // ── Gaming / Progress Persistence ──

    fun getXP(): Int = prefs.getInt("XP", 0)
    
    fun addXP(amount: Int) {
        val newXP = getXP() + amount
        prefs.edit().putInt("XP", newXP).apply()
    }

    fun getLevel(): Int {
        val xp = getXP()
        // Simple level logic: 0-99=1, 100-199=2, ..., 900+=10
        val lvl = (xp / 100) + 1
        return lvl.coerceIn(1, 10)
    }

    fun getTitle(): String {
        return when (getLevel()) {
            1 -> "Novice"
            2 -> "Student"
            3 -> "Smart"
            4 -> "Researcher"
            5 -> "Expert"
            6 -> "Professional"
            7 -> "Elite"
            8 -> "Legend"
            9 -> "Conqueror"
            10 -> "GOAT"
            else -> "Scholar"
        }
    }

    // ── Intelligence & Gamification Accessors ──

    private var _intelligenceManager: IntelligenceManager? = null
    private var _gamificationManager: GamificationManager? = null

    fun getIntelligenceManager(): IntelligenceManager {
        if (_intelligenceManager == null) {
            _intelligenceManager = IntelligenceManager(context)
        }
        return _intelligenceManager!!
    }

    fun getGamificationManager(): GamificationManager {
        if (_gamificationManager == null) {
            _gamificationManager = GamificationManager(context)
        }
        return _gamificationManager!!
    }

    // ── TTS Preferences ──

    fun setTTSEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("TTS_ENABLED", enabled).apply()
    }

    fun isTTSEnabled(): Boolean {
        return prefs.getBoolean("TTS_ENABLED", false)
    }

    fun setTTSVoiceStyle(style: String) {
        prefs.edit().putString("TTS_VOICE_STYLE", style).apply()
    }

    fun getTTSVoiceStyle(): String {
        return prefs.getString("TTS_VOICE_STYLE", "professor") ?: "professor"
    }

    fun setTTSSpeed(speed: Float) {
        prefs.edit().putFloat("TTS_SPEED", speed).apply()
    }

    fun getTTSSpeed(): Float {
        return prefs.getFloat("TTS_SPEED", 1.0f)
    }

    fun setTTSAutoPlay(autoPlay: Boolean) {
        prefs.edit().putBoolean("TTS_AUTO_PLAY", autoPlay).apply()
    }

    fun isTTSAutoPlay(): Boolean {
        return prefs.getBoolean("TTS_AUTO_PLAY", false)
    }

    // ── Premium App Settings ──

    fun setAutoDomainDetection(enabled: Boolean) {
        prefs.edit().putBoolean("AUTO_DOMAIN", enabled).apply()
    }
    fun isAutoDomainDetection(): Boolean = prefs.getBoolean("AUTO_DOMAIN", true)

    fun setSaveHistory(enabled: Boolean) {
        prefs.edit().putBoolean("SAVE_HISTORY", enabled).apply()
    }
    fun isSaveHistory(): Boolean = prefs.getBoolean("SAVE_HISTORY", true)

    fun setFastMode(enabled: Boolean) {
        prefs.edit().putBoolean("FAST_MODE", enabled).apply()
    }
    fun isFastMode(): Boolean = prefs.getBoolean("FAST_MODE", false)

    fun setExpertModeDefault(enabled: Boolean) {
        prefs.edit().putBoolean("EXPERT_DEFAULT", enabled).apply()
    }
    fun isExpertModeDefault(): Boolean = prefs.getBoolean("EXPERT_DEFAULT", false)

    fun setPrivacyMode(enabled: Boolean) {
        prefs.edit().putBoolean("PRIVACY_MODE", enabled).apply()
    }
    fun isPrivacyMode(): Boolean = prefs.getBoolean("PRIVACY_MODE", false)

    fun setHybridDefault(enabled: Boolean) {
        prefs.edit().putBoolean("HYBRID_DEFAULT", enabled).apply()
    }
    fun isHybridDefault(): Boolean = prefs.getBoolean("HYBRID_DEFAULT", false)

    fun setThemeSelection(theme: String) { // "system", "dark", "light"
        prefs.edit().putString("THEME_SELECTION", theme).apply()
    }
    fun getThemeSelection(): String = prefs.getString("THEME_SELECTION", "system") ?: "system"
}
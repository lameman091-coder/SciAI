package com.funtime.sciai.data

import android.content.Context

class UserManager(context: Context) {

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
}
package com.funtime.sciai.data



import android.content.Context

class UserManager(context: Context) {

    private val prefs = context.getSharedPreferences("sciai_prefs", Context.MODE_PRIVATE)

    /**
     * Returns a persistent device-specific user ID.
     * Generated once via UUID and stored in SharedPreferences.
     * Future-proof: Replace with FirebaseAuth UID when auth is added.
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
}
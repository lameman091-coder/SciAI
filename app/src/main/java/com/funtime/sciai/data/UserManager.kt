package com.funtime.sciai.data



import android.content.Context

class UserManager(context: Context) {

    private val prefs = context.getSharedPreferences("sciai_prefs", Context.MODE_PRIVATE)

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
package com.funtime.sciai.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object OfflineCache {
    private const val PREFS_NAME = "sciai_offline_cache"
    private const val KEY_ASK_CACHE = "ask_cache"
    private const val KEY_ARTICLES_CACHE = "articles_cache"
    private const val KEY_LAST_SYNC = "last_sync"
    private const val CACHE_EXPIRY_MS = 24 * 60 * 60 * 1000L // 24 hours

    private var prefs: SharedPreferences? = null
    private var context: Context? = null
    private val gson = Gson()

    fun init(ctx: Context) {
        context = ctx.applicationContext
        prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getPrefs(): SharedPreferences {
        return prefs ?: throw IllegalStateException("OfflineCache not initialized. Call init() first.")
    }

    data class CachedAnswer(
        val question: String,
        val answer: String,
        val type: String,
        val sources: List<String>,
        val timestamp: Long
    )

    fun cacheAskResponse(question: String, response: CachedAnswer) {
        val cache = getAskCache().toMutableMap()
        cache[question] = response
        getPrefs().edit().putString(KEY_ASK_CACHE, gson.toJson(cache)).apply()
    }

    fun getCachedAskResponse(question: String): CachedAnswer? {
        val cache = getAskCache()
        val cached = cache[question] ?: return null
        return if (System.currentTimeMillis() - cached.timestamp < CACHE_EXPIRY_MS) cached else null
    }

    private fun getAskCache(): Map<String, CachedAnswer> {
        val json = getPrefs().getString(KEY_ASK_CACHE, null) ?: return emptyMap()
        val type = object : TypeToken<Map<String, CachedAnswer>>() {}.type
        return try { gson.fromJson(json, type) } catch (e: Exception) { emptyMap() }
    }

    data class CachedArticle(
        val id: String,
        val title: String,
        val summary: String,
        val source: String,
        val link: String,
        val authors: String,
        val journal: String,
        val date: String,
        val score: Float,
        val tier: String,
        val timestamp: Long
    )

    fun cacheArticles(articles: List<CachedArticle>) {
        val cache = getArticlesCache().toMutableMap()
        articles.forEach { cache[it.id] = it }
        getPrefs().edit().putString(KEY_ARTICLES_CACHE, gson.toJson(cache)).apply()
        getPrefs().edit().putLong(KEY_LAST_SYNC, System.currentTimeMillis()).apply()
    }

    fun getCachedArticles(): List<CachedArticle> {
        val cache = getArticlesCache()
        return cache.values.filter { System.currentTimeMillis() - it.timestamp < CACHE_EXPIRY_MS }
            .sortedByDescending { it.timestamp }
    }

    private fun getArticlesCache(): Map<String, CachedArticle> {
        val json = getPrefs().getString(KEY_ARTICLES_CACHE, null) ?: return emptyMap()
        val type = object : TypeToken<Map<String, CachedArticle>>() {}.type
        return try { gson.fromJson(json, type) } catch (e: Exception) { emptyMap() }
    }

    fun isOnline(): Boolean {
        val connectivity = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        return connectivity?.activeNetworkInfo?.isConnected == true
    }

    fun clearExpiredCache() {
        val now = System.currentTimeMillis()
        val askCache = getAskCache().filterValues { now - it.timestamp < CACHE_EXPIRY_MS }
        val articlesCache = getArticlesCache().filterValues { now - it.timestamp < CACHE_EXPIRY_MS }
        getPrefs().edit()
            .putString(KEY_ASK_CACHE, gson.toJson(askCache))
            .putString(KEY_ARTICLES_CACHE, gson.toJson(articlesCache))
            .apply()
    }
}
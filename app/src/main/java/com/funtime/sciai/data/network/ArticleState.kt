package com.funtime.sciai.data.network

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Singleton state holder for the Articles screen.
 * Hoisted outside of composition so state survives navigation
 * (e.g., going to article_detail and pressing back).
 */
object ArticleState {
    var selectedArticle by mutableStateOf<Article?>(null)

    // ── Persisted screen state (survives navigation recomposition) ──
    var trendingArticles by mutableStateOf<List<Article>>(emptyList())
    var searchResultArticles by mutableStateOf<List<Article>>(emptyList())
    var screenMode by mutableStateOf("TRENDING") // "TRENDING" or "RESULTS"
    var lastSearchQuery by mutableStateOf("")
    var searchTotalCount by mutableStateOf(0)
    var searchCurrentPage by mutableStateOf(1)
    var hasLoadedTrending by mutableStateOf(false)

    // Filter state
    var selectedSort by mutableStateOf("pub+date")
    var selectedDomain by mutableStateOf("All")
    var selectedType by mutableStateOf("All")
    var selectedSource by mutableStateOf("All")
    var selectedDate by mutableStateOf("Latest")

    fun resetToTrending() {
        screenMode = "TRENDING"
        searchResultArticles = emptyList()
        searchCurrentPage = 1
        searchTotalCount = 0
        lastSearchQuery = ""
    }
}

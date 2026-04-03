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
    var trendingArticles by mutableStateOf<List<Article>>(emptyList())
    var searchResultArticles by mutableStateOf<List<Article>>(emptyList())
    var hasLoadedTrending by mutableStateOf(false)
    var screenMode by mutableStateOf("TRENDING") // "TRENDING" or "RESULTS"
    var searchTotalCount by mutableStateOf(0)
    var searchCurrentPage by mutableStateOf(1)
    var lastSearchQuery by mutableStateOf("")

    // ── Filter State ──
    var selectedSort by mutableStateOf("Relevance")
    var selectedSource by mutableStateOf("All")
    var selectedDomain by mutableStateOf("All")
    var selectedDate by mutableStateOf("All Time")
    var selectedType by mutableStateOf("All")

    var selectedArticle by mutableStateOf<Article?>(null)

    fun resetToTrending() {
        screenMode = "TRENDING"
        searchResultArticles = emptyList()
        searchTotalCount = 0
        searchCurrentPage = 1
        lastSearchQuery = ""
        selectedSort = "Relevance"
        selectedSource = "All"
        selectedDomain = "All"
        selectedDate = "All Time"
        selectedType = "All"
    }

    /**
     * Ensures an article has a valid ID and non-null fields before UI storage.
     * Generates a synthetic ID if the original is blank.
     */
    fun safeArticle(it: Article): Article {
        val finalId = it.id.takeIf { id -> id.isNotBlank() }
            ?: "syn_${it.title.hashCode().toString(16)}_${it.link.hashCode().toString(16)}_${(1000..9999).random()}"
        return it.copy(
            id = finalId,
            title = it.title ?: "",
            summary = it.summary ?: "",
            source = it.source ?: "Unknown",
            authors = it.authors ?: "Various Authors",
            journal = it.journal ?: "Research",
            date = it.date ?: ""
        )
    }
}

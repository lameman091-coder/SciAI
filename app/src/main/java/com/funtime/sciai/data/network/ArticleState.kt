package com.funtime.sciai.data.network

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Shared state to avoid passing large strings through navigation routes.
 */
object ArticleState {
    var selectedArticle by mutableStateOf<Article?>(null)
}

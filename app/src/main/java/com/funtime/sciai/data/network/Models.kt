package com.funtime.sciai.data.network

import com.google.gson.annotations.SerializedName

data class AskRequest(
    val question: String,
    val mode: String,
    val domain: String,
    @SerializedName("book_id") val bookId: String? = null,
    val hybrid: Boolean = false,
    @SerializedName("user_id") val userId: String = "guest"
)

data class AskResponse(
    val answer: String = "",
    val type: String = "LLM_ONLY",
    val sources: List<String> = emptyList()
)

data class Book(
    val id: String = "",
    @SerializedName("user_id") val userId: String = "",
    val title: String = "",
    val domain: String = "",
    val preview: String = ""
)

data class Article(
    val id: String = "",
    val title: String = "",
    val summary: String = "",
    val source: String = "",
    val link: String = "",
    val score: Double = 0.0,
    val authors: String = "Various Authors",
    val journal: String = "Research Journal",
    val date: String = "Unknown Date",
    val tier: String = "peer_reviewed"  // "peer_reviewed", "preprint", "background"
)

data class SaveArticleRequest(
    @SerializedName("user_id") val userId: String,
    val id: String,
    val title: String,
    val summary: String,
    val source: String,
    val link: String,
    val score: Double = 0.0,
    val authors: String = "Various Authors",
    val journal: String = "Research Journal",
    val date: String = "Unknown Date",
    val tier: String = "peer_reviewed"
)

data class ArticlesResponse(
    val articles: List<Article> = emptyList(),
    @SerializedName("total_count") val totalCount: Int = 0,
    val page: Int = 1
)

data class UploadResponse(
    val status: String,
    @SerializedName("chunks_processed") val chunksProcessed: Int,
    val source: String,
    @SerializedName("book_id") val bookId: String
)

data class GenerateQuestionsRequest(
    val mode: String,
    val topic: String,
    val domain: String,
    val level: Int,
    val count: Int = 4,
    @SerializedName("context_chunks") val contextChunks: List<String> = emptyList(),
    @SerializedName("previous_questions") val previousQuestions: List<Map<String, String>> = emptyList()
)

data class Question(
    val id: String,
    val type: String,
    val question: String,
    val options: List<String>?,
    val answer: String,
    val explanation: String,
    @SerializedName("difficulty_tag") val difficultyTag: String,
    val concepts: List<String> = emptyList(),
    @SerializedName("variation_tag") val variationTag: String
)

data class GenerateQuestionsResponse(
    val mode: String,
    val topic: String,
    val domain: String,
    val level: Int,
    val questions: List<Question> = emptyList(),
    val error: String? = null
)

data class EvaluateAnswerRequest(
    val question: String,
    val user_answer: String,
    val correct_answer: String
)

data class EvaluateAnswerResponse(
    val feedback: String
)


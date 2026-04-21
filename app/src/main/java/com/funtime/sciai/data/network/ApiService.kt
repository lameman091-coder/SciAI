package com.funtime.sciai.data.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*
import com.funtime.sciai.data.tts.TTSRequest

interface ApiService {

    @POST("ask")
    fun ask(@Body request: AskRequest): Call<AskResponse>

    @POST("analyze-image")
    fun analyzeImage(@Body request: ImageAnalysisRequest): Call<ImageAnalysisResponse>

    @GET("books")
    fun getBooks(@Query("user_id") userId: String): Call<List<Book>>

    @Multipart
    @POST("upload-book")
    fun uploadBook(
        @Part file: MultipartBody.Part,
        @Part("domain") domain: RequestBody,
        @Part("source_type") sourceType: RequestBody,
        @Part("user_id") userId: RequestBody
    ): Call<UploadResponse>

    @GET("articles")
    fun getArticles(
        @Query("query") query: String?,
        @Query("sort") sort: String? = "pub+date",
        @Query("page") page: Int? = 1,
        @Query("limit") limit: Int? = 10,
        @Query("source") source: String? = "all",
        @Query("domain") domain: String? = "all",
        @Query("date_range") dateRange: String? = "all",
        @Query("type") type: String? = "all"
    ): Call<ArticlesResponse>

    @GET("articles/trending")
    fun getTrending(
        @Query("limit") limit: Int? = 25
    ): Call<ArticlesResponse>

    @POST("save-article")
    fun saveArticle(@Body request: SaveArticleRequest): Call<Map<String, String>>

    @POST("generate-questions")
    fun generateQuestions(@Body request: GenerateQuestionsRequest): Call<GenerateQuestionsResponse>

    @GET("saved-articles")
    fun getSavedArticles(@Query("user_id") userId: String): Call<List<Article>>

    @POST("evaluate-answer")
    fun evaluateAnswer(@Body request: EvaluateAnswerRequest): Call<EvaluateAnswerResponse>

    @POST("evaluate-answer-detailed")
    fun evaluateAnswerDetailed(@Body request: EvaluateAnswerDetailedRequest): Call<EvaluateAnswerDetailedResponse>

    @DELETE("saved-articles/{user_id}/{article_id}")
    fun unsaveArticle(@Path("user_id") userId: String, @Path("article_id") articleId: String): Call<Map<String, String>>

    @DELETE("books/{book_id}")
    fun deleteBook(@Path("book_id") bookId: String, @Query("user_id") userId: String): Call<Map<String, String>>

    // ── Controller / Companion Route ─────────────────────────────────────────
    @POST("route")
    fun routeQuery(@Body request: RouteRequest): Call<RouteResponse>

    @POST("companion/chat")
    fun companionChat(@Body request: CompanionChatRequest): Call<CompanionChatResponse>

    // ── TTS (Text-to-Speech) ─────────────────────────────────────────────────
    @Streaming
    @POST("tts")
    fun textToSpeech(@Body request: TTSRequest): Call<ResponseBody>
}

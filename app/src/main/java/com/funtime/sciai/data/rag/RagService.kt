package com.funtime.sciai.data.rag

import com.funtime.sciai.data.network.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

object RagService {

    fun fetchBooks(userId: String, callback: (List<Book>?) -> Unit) {
        NetworkClient.apiService.getBooks(userId).enqueue(object : Callback<List<Book>> {
            override fun onResponse(call: Call<List<Book>>, response: Response<List<Book>>) {
                callback(if (response.isSuccessful) response.body() else null)
            }
            override fun onFailure(call: Call<List<Book>>, t: Throwable) {
                callback(null)
            }
        })
    }

    fun fetchArticles(
        query: String?, 
        sort: String = "pub+date", 
        page: Int = 1, 
        limit: Int = 10,
        source: String = "all",
        domain: String = "all",
        dateRange: String = "all",
        type: String = "all",
        callback: (List<Article>?, Int) -> Unit
    ) {
        NetworkClient.apiService.getArticles(query, sort, page, limit, source, domain, dateRange, type)
            .enqueue(object : Callback<ArticlesResponse> {
                override fun onResponse(call: Call<ArticlesResponse>, response: Response<ArticlesResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        callback(body.articles, body.totalCount)
                    } else {
                        callback(null, 0)
                    }
                }
                override fun onFailure(call: Call<ArticlesResponse>, t: Throwable) {
                    println("[SciAI] fetchArticles error: ${t.message}")
                    callback(null, 0)
                }
            })
    }

    fun fetchTrending(
        limit: Int = 25,
        callback: (List<Article>?, Int) -> Unit
    ) {
        NetworkClient.apiService.getTrending(limit)
            .enqueue(object : Callback<ArticlesResponse> {
                override fun onResponse(call: Call<ArticlesResponse>, response: Response<ArticlesResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        callback(body.articles, body.totalCount)
                    } else {
                        callback(null, 0)
                    }
                }
                override fun onFailure(call: Call<ArticlesResponse>, t: Throwable) {
                    println("[SciAI] fetchTrending error: ${t.message}")
                    callback(null, 0)
                }
            })
    }

    fun ask(
        question: String,
        mode: String,
        domain: String,
        bookId: String? = null,
        hybrid: Boolean = false,
        userId: String = "guest",
        callback: (AskResponse?) -> Unit
    ) {
        val request = AskRequest(question, mode, domain, bookId, hybrid, userId)
        NetworkClient.apiService.ask(request).enqueue(object : Callback<AskResponse> {
            override fun onResponse(call: Call<AskResponse>, response: Response<AskResponse>) {
                println("API Response [/ask]: ${response.code()} body: ${response.body()}")
                if (response.isSuccessful) {
                    callback(response.body())
                } else {
                    callback(null)
                }
            }

            override fun onFailure(call: Call<AskResponse>, t: Throwable) {
                callback(null)
            }
        })
    }

    fun uploadBook(
        file: File,
        domain: String,
        userId: String,
        sourceType: String = "PDF",
        callback: (Boolean, String) -> Unit
    ) {
        val filePart = MultipartBody.Part.createFormData(
            "file", 
            file.name, 
            file.asRequestBody("application/pdf".toMediaTypeOrNull())
        )
        val domainPart = domain.toRequestBody("text/plain".toMediaTypeOrNull())
        val sourcePart = sourceType.toRequestBody("text/plain".toMediaTypeOrNull())
        val userIdPart = userId.toRequestBody("text/plain".toMediaTypeOrNull())

        NetworkClient.apiService.uploadBook(filePart, domainPart, sourcePart, userIdPart)
            .enqueue(object : Callback<UploadResponse> {
                override fun onResponse(call: Call<UploadResponse>, response: Response<UploadResponse>) {
                    if (response.isSuccessful) {
                        callback(true, "Successfully uploaded: ${file.name}")
                    } else {
                        callback(false, "Upload failed: ${response.errorBody()?.string()}")
                    }
                }
                override fun onFailure(call: Call<UploadResponse>, t: Throwable) {
                    callback(false, "Network error: ${t.message}")
                }
            })
    }

    fun saveArticle(userId: String, article: Article, callback: (Boolean) -> Unit) {
        val request = SaveArticleRequest(
            userId = userId,
            id = article.id,
            title = article.title,
            summary = article.summary,
            source = article.source,
            link = article.link,
            score = article.score,
            authors = article.authors,
            journal = article.journal,
            date = article.date,
            tier = article.tier
        )
        NetworkClient.apiService.saveArticle(request).enqueue(object : Callback<Map<String, String>> {
            override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                callback(response.isSuccessful)
            }
            override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                callback(false)
            }
        })
    }

    fun fetchSavedArticles(userId: String, callback: (List<Article>?) -> Unit) {
        NetworkClient.apiService.getSavedArticles(userId).enqueue(object : Callback<List<Article>> {
            override fun onResponse(call: Call<List<Article>>, response: Response<List<Article>>) {
                callback(if (response.isSuccessful) response.body() else null)
            }
            override fun onFailure(call: Call<List<Article>>, t: Throwable) {
                callback(null)
            }
        })
    }

    fun deleteBook(bookId: String, userId: String, callback: (Boolean, String) -> Unit) {
        NetworkClient.apiService.deleteBook(bookId, userId).enqueue(object : Callback<Map<String, String>> {
            override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                if (response.isSuccessful) {
                    callback(true, "PDF deleted successfully")
                } else {
                    callback(false, "Delete failed: ${response.code()}")
                }
            }
            override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                callback(false, "Network error: ${t.message}")
            }
        })
    }
}

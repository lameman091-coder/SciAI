package com.funtime.sciai.data.groq


import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import com.funtime.sciai.BuildConfig

object GroqService {

    private val API_KEY = BuildConfig.GROQ_API_KEY

    fun ask(
        question: String,
        mode: String,
        level: String = "Academic",
        domain: String = "Biology",
        callback: (String) -> Unit
    ) {

        val client = OkHttpClient()
        val model = when (mode) {
            "Exam" -> "openai/gpt-oss-20b"
            "Concept" -> "openai/gpt-oss-20b"
            "Expert" -> "qwen/qwen3-32b"
            else -> "openai/gpt-oss-20b"
        }

        val prompt = buildPrompt(question, mode, level, domain)

        val json = JSONObject().apply {
            put("model", model)

            val messagesArray = org.json.JSONArray().apply {
                put(
                    JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    }
                )
            }

            put("messages", messagesArray)
        }

        val body = json.toString().toRequestBody(
            ("application" +
                    "/json").toMediaTypeOrNull()
        )

        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/chat/completions")
            .addHeader("Authorization", "Bearer $API_KEY")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                callback("Error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val res = response.body?.string() ?: ""
                println("GROQ RESPONSE: $res")

                try {
                    val jsonObj = JSONObject(res)

                    if (jsonObj.has("choices")) {
                        val answer = jsonObj
                            .getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")

                        callback(answer)
                    } else if (jsonObj.has("error")) {
                        val errorMsg = jsonObj
                            .getJSONObject("error")
                            .getString("message")

                        callback("API Error: $errorMsg")
                    } else {
                        callback("Unexpected response format")
                    }

                } catch (e: Exception) {
                    callback("Parsing error: ${e.message}")
                }
            }
        })
    }

    private fun buildPrompt(
        question: String,
        mode: String,
        level: String,
        domain: String
    ): String {

        return when (mode) {

            "Exam" -> """
You are an exam-focused assistant.


FORMAT:
FINAL ANSWER:
- Direct answer (2-4 lines)

IMPORTANT POINTS:
- Bullet points

MEMORY TRICK:
- Quick recall trick

Question:
$question
""".trimIndent()

            "Concept" -> """
You are a conceptual teacher.

FORMAT:
CORE IDEA:
- Simple explanation

KEY COMPONENTS:
- Bullet points

WORKING:
- Step-by-step explanation

ANALOGY:
- Real-life example

Question:
$question
""".trimIndent()

            "Expert" -> """
You are an advanced scientific expert.

Level: $level

FORMAT:
DEFINITION:
- Technical definition

DEEP EXPLANATION:
- Detailed concept

MECHANISM:
- Stepwise explanation

CRITICAL INSIGHT:
- Why important

ADVANCED PERSPECTIVE:
- Research insights

Question:
$question
""".trimIndent()

            else -> question
        }
    }
}
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

        val body = json.toString().toRequestBody(("application" +
                "/json").toMediaTypeOrNull())

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

        return """
You are a scientific assistant specialized in $domain.

Mode: $mode
Level: $level

Question:
$question

STRICT FORMAT RULES:
- Do NOT use *, **, or markdown
- Do NOT use HTML tags like <br>
- Use plain text only
- Use bullet points with "-"
- Keep sections clearly separated

STRUCTURE:
SUMMARY:
- short answer

KEY POINTS:
- point 1
- point 2

EXPLANATION:
- detailed explanation

""".trimIndent()
    }
}
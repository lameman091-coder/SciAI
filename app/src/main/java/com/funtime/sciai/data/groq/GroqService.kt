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
            "Concept" -> "openai/gpt-oss-120b"
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
        val structureRules = """
Structure the answer into clear sections.

Rules:
- Each section must start with a short heading
- Followed by explanation
- Keep headings concise (3–6 words)
- Do NOT use symbols like ### or **
- Do NOT include <think> or hidden reasoning

IMPORTANT FOR EQUATIONS:
- Do NOT use LaTeX (no \frac, $$, \, etc.)
- Write equations in readable plain format

Example:
Correct → K = ([C]^c × [D]^d) / ([A]^a × [B]^b)
Wrong → K = \frac{[C]^c [D]^d}{[A]^a [B]^b}
""".trimIndent()
        val domainPrompt = when (domain) {
            "Biology" -> """
Focus on biological processes, flow, and real-life examples.
Use clear terminology and stepwise explanation where needed.
""".trimIndent()

            "Physics" -> """
Include formulas, variables, units, and real-world applications.
Explain meaning of each variable clearly.
""".trimIndent()

            "Chemistry" -> """
Include chemical equations, reaction mechanisms, and symbolic representation.
Focus on clarity in reactions and equilibrium.
""".trimIndent()

            else -> ""
        }

        val modePrompt = when (mode) {

            "Exam" -> """
You are an exam-focused assistant.

Give concise, high-yield answers suitable for exams.
Use bullet-style clarity where helpful.
""".trimIndent()

            "Concept" -> """
You are a conceptual teacher.

Explain in a simple, intuitive way.
Focus on understanding rather than memorization.
""".trimIndent()

            "Expert" -> """
You are an advanced scientific expert.

Level: $level

Provide deep, analytical, and research-level explanation.
Include insights, reasoning, and critical understanding.
""".trimIndent()

            else -> ""
        }

        return """
$modePrompt

$domainPrompt

$structureRules

Question:
$question
""".trimIndent()
    }
    }

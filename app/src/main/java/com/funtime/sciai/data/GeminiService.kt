package com.funtime.sciai.data

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiService {

    private const val API_KEY = "AIzaSyAJnHthLZ1sKMQQB8qPJ4JFu5NflrBrkTk"

    suspend fun analyzeImage(
        context: Context,
        uri: Uri
    ): String {

        return withContext(Dispatchers.IO) {

            try {
                val model = GenerativeModel(
                    modelName = "gemini-3.1-flash-lite-preview",
                    apiKey = API_KEY
                )

                val bitmap = ImageUtils.decodeSampledBitmapFromUri(
                    context,
                    uri,
                    1024,
                    1024
                ) ?: throw IllegalArgumentException("Failed to decode image")

                val inputContent = content {
                    image(bitmap)

                    text("""
                    Analyze this image carefully.

                    If it contains a scientific question:
                    - Extract the question
                    - Answer in CLEAN format (NO symbols like **, ##,++)
                    - Answer in structured format:
                      Definition
                      Explanation
                      Key Points
                      Do NOT use markdown symbols.

                    If unclear, describe what you see.
                  """.trimIndent())
                }

                val response = model.generateContent(inputContent)

                response.text ?: "No response"

            } catch (e: Exception) {
                "Error: ${e.message}"
            }
        }
    }
}

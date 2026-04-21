package com.funtime.sciai.data

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiService {

    suspend fun analyzeImage(
        context: Context,
        uri: Uri
    ): String {

        return withContext(Dispatchers.IO) {

            try {
                val bitmap = ImageUtils.decodeSampledBitmapFromUri(
                    context,
                    uri,
                    1024,
                    1024
                ) ?: throw IllegalArgumentException("Failed to decode image")

                // Convert to Base64
                val stream = java.io.ByteArrayOutputStream()
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, stream)
                val bytes = stream.toByteArray()
                val imageB64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)

                val request = com.funtime.sciai.data.network.ImageAnalysisRequest(
                    imageB64 = imageB64,
                    question = "Analyze this scientific image/question concisely."
                )

                val response = com.funtime.sciai.data.network.NetworkClient.apiService.analyzeImage(request).execute()

                if (response.isSuccessful) {
                    response.body()?.answer ?: "No response from AI engine."
                } else {
                    "Error: Backend failed with code ${response.code()}"
                }

            } catch (e: Exception) {
                "Error: ${e.message}"
            }
        }
    }
}

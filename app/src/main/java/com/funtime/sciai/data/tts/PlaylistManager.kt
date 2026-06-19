package com.funtime.sciai.data.tts

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.funtime.sciai.data.network.NetworkClient.BASE_URL

class PlaylistManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("SciAI_Playlist", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val TAG = "PlaylistManager"
    private val client = OkHttpClient()

    fun getTracks(): List<Track> {
        val json = prefs.getString("tracks", "[]")
        val type = object : TypeToken<List<Track>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveTrack(track: Track) {
        val tracks = getTracks().toMutableList()
        val existing = tracks.indexOfFirst { it.id == track.id }
        if (existing != -1) {
            tracks[existing] = track
        } else {
            tracks.add(track)
        }
        prefs.edit().putString("tracks", gson.toJson(tracks)).apply()
    }

    fun removeTrack(trackId: String) {
        val tracks = getTracks().toMutableList()
        val track = tracks.find { it.id == trackId }
        track?.let {
            val file = File(it.filePath)
            if (file.exists()) file.delete()
            tracks.remove(it)
            prefs.edit().putString("tracks", gson.toJson(tracks)).apply()
        }
    }

    fun isDownloaded(trackId: String): Boolean {
        return getTracks().any { it.id == trackId && it.isDownloaded }
    }

    fun saveMetadata(answerId: String, title: String, text: String, mode: String, voiceStyle: VoiceStyle) {
        val tracks = getTracks().toMutableList()
        val existing = tracks.find { it.id == answerId }
        
        if (existing == null) {
            val newTrack = Track(
                id = answerId,
                title = title,
                text = text,
                mode = mode,
                voiceStyleId = voiceStyle.id,
                timestamp = System.currentTimeMillis(),
                isDownloaded = false
            )
            tracks.add(newTrack)
            prefs.edit().putString("tracks", gson.toJson(tracks)).apply()
            Log.i(TAG, "Metadata saved for $answerId")
        }
    }

    fun downloadTrack(
        answerId: String,
        text: String,
        mode: String,
        voiceStyle: VoiceStyle,
        onProgress: (Float) -> Unit,
        onComplete: (Track?) -> Unit
    ) {
        Thread {
            try {
                val escapedText = text.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t")
                val json = """{"text":"$escapedText","voice_style":"${voiceStyle.id}","mode":"$mode","speed":1.0}"""
                val body = json.toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("${BASE_URL}tts")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Download failed: ${response.code}")
                        onComplete(null)
                        return@Thread
                    }

                    val responseBody = response.body ?: return@Thread
                    val contentLength = responseBody.contentLength()
                    val inputStream = responseBody.byteStream()
                    
                    val outputDir = File(context.getExternalFilesDir(null), "SciAI_Tracks").apply { mkdirs() }
                    val outputFile = File(outputDir, "track_$answerId.wav")
                    
                    val outputStream = FileOutputStream(outputFile)
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (contentLength > 0) {
                            onProgress(totalRead.toFloat() / contentLength)
                        }
                    }

                    outputStream.flush()
                    outputStream.close()
                    inputStream.close()

                    val tracks = getTracks()
                    val existing = tracks.find { it.id == answerId }

                    val newTrack = Track(
                        id = answerId,
                        title = existing?.title ?: text.take(60),
                        text = text,
                        mode = mode,
                        voiceStyleId = voiceStyle.id,
                        filePath = outputFile.absolutePath,
                        timestamp = existing?.timestamp ?: System.currentTimeMillis(),
                        isDownloaded = true
                    )
                    saveTrack(newTrack)
                    onComplete(newTrack)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Download error: ${e.message}")
                onComplete(null)
            }
        }.start()
    }
}

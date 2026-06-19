package com.funtime.sciai.data.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.core.content.ContextCompat
import com.funtime.sciai.data.network.NetworkClient.BASE_URL
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * SciAI TTS Manager
 * ─────────────────
 * Central Text-to-Speech controller with tiered fallback:
 *   Tier 1+2: Backend (Kyutai / HuggingFace) → streams WAV via /tts
 *   Tier 3:   Android TTS (offline fallback)
 *
 * State Machine: IDLE → LOADING → PLAYING ⇄ PAUSED → IDLE
 */
class TTSManager(private val context: Context) {

    companion object {
        private const val TAG = "TTSManager"
    }

    // ── State ───────────────────────────────────────────────────────────────
    private var currentState: TTSState = TTSState.IDLE
    private var stateListener: ((TTSState) -> Unit)? = null
    private var progressListener: ((Float) -> Unit)? = null

    // ── ExoPlayer (Backend Streaming) ───────────────────────────────────────
    private var exoPlayer: ExoPlayer? = null
    private var exoPlayerInitialized = false
    private var currentAudioFile: File? = null
    private var isUsingAndroidTts = false
    private var activeStreamingCall: okhttp3.Call? = null

    // ── Android TTS (Tier 3 Offline Fallback) ───────────────────────────────
    private var androidTts: TextToSpeech? = null
    private var androidTtsReady = false
    private var androidTtsInitStarted = false
    
    private val playlistManager = PlaylistManager(context)

    // NOTE: init block is intentionally empty.
    // Both ExoPlayer and Android TTS are lazily initialized to prevent
    // blocking the main thread during Compose remember{} construction.
    // TextToSpeech() blocks on media.audio_policy service which causes ANR.

    /**
     * Ensures ExoPlayer is initialized (must be called on the main thread).
     */
    private fun ensureExoPlayer() {
        if (exoPlayerInitialized) return
        exoPlayerInitialized = true
        
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.CONTENT_TYPE_SPEECH)
            .build()
            
        exoPlayer = ExoPlayer.Builder(context).build().apply {
            setAudioAttributes(audioAttributes, true)
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    when (state) {
                        Player.STATE_READY -> {
                            if (playWhenReady) {
                                updateState(TTSState.PLAYING)
                                Log.d(TAG, "ExoPlayer: Playing")
                            }
                        }

                        Player.STATE_BUFFERING -> {
                            updateState(TTSState.LOADING)
                        }

                        Player.STATE_ENDED -> {
                            updateState(TTSState.IDLE)
                            progressListener?.invoke(1f)
                        }
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        updateState(TTSState.PLAYING)
                    } else if (playbackState == Player.STATE_READY) {
                        updateState(TTSState.PAUSED)
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.e(TAG, "ExoPlayer Error: ${error.message}", error)
                    updateState(TTSState.ERROR)
                }
            })
        }
        Log.i(TAG, "ExoPlayer: Lazily initialized")
    }

    /**
     * Ensures Android TTS is initialized on a background thread.
     * The TextToSpeech constructor blocks waiting for media.audio_policy,
     * so it MUST NOT run on the main thread.
     */
    private fun ensureAndroidTts() {
        if (androidTtsInitStarted) return
        androidTtsInitStarted = true
        
        Thread {
            try {
                initAndroidTts()
            } catch (e: Exception) {
                Log.e(TAG, "Android TTS background init failed: ${e.message}")
            }
        }.start()
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Start speaking the given text.
     * Tries local cache first, then backend streaming, falls back to Android TTS.
     */
    fun speak(
        text: String,
        answerId: String = "default",
        voiceStyle: VoiceStyle = VoiceStyle.PROFESSOR,
        mode: String = "Concept",
        speed: Float = 1.0f,
        onStateChange: ((TTSState) -> Unit)? = null
    ) {
        ensureExoPlayer()
        ensureAndroidTts()
        stop()
        
        // Clean up old temp files before starting new session
        val oldTempDir = File(context.cacheDir, "tts_temp_$answerId")
        if (oldTempDir.exists()) {
            oldTempDir.deleteRecursively()
        }
        
        exoPlayer?.clearMediaItems()
        stateListener = onStateChange
        updateState(TTSState.LOADING)
        isUsingAndroidTts = false

        // 1. Check local playlist (downloaded tracks)
        val savedTrack = playlistManager.getTracks().find { it.id == answerId && it.isDownloaded }
        if (savedTrack != null) {
            val file = File(savedTrack.filePath)
            if (file.exists()) {
                Log.i(TAG, "Playlist hit for $answerId. Playing saved track.")
                playLocalFile(file, speed)
                return
            }
        }

        // 2. Check local cache
        val cacheFile = File(context.cacheDir, "tts_$answerId.wav")
        if (cacheFile.exists() && cacheFile.length() > 100) {
            Log.i(TAG, "Cache hit for $answerId. Playing temp file.")
            playLocalFile(cacheFile, speed)
            return
        }

        // 3. Try backend streaming
        Log.i(TAG, "Cache miss. Streaming TTS from backend for $answerId")
        streamFromBackend(text, answerId, voiceStyle, mode, speed)
    }

    fun downloadTrack(
        answerId: String,
        text: String,
        mode: String,
        voiceStyle: VoiceStyle,
        onProgress: (Float) -> Unit,
        onComplete: (Boolean) -> Unit
    ) {
        playlistManager.downloadTrack(answerId, text, mode, voiceStyle, onProgress) { track ->
            onComplete(track != null)
        }
    }

    fun saveMetadata(answerId: String, title: String, text: String, mode: String, voiceStyle: VoiceStyle) {
        playlistManager.saveMetadata(answerId, title, text, mode, voiceStyle)
    }
    
    fun isDownloaded(answerId: String): Boolean = playlistManager.isDownloaded(answerId)
    
    fun isSaved(answerId: String): Boolean = playlistManager.getTracks().any { it.id == answerId }

    private fun streamFromBackend(
        text: String,
        answerId: String,
        voiceStyle: VoiceStyle,
        mode: String,
        speed: Float
    ) {
        val tempDir = File(context.cacheDir, "tts_temp_$answerId").apply { mkdirs() }
        var chunkIndex = 0

        Thread {
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.MINUTES)
                    .build()
                
                val escapedText = text.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t")

                val json =
                    """{"text":"$escapedText","voice_style":"${voiceStyle.id}","mode":"$mode","speed":$speed}"""

                val body = okhttp3.RequestBody.create(
                    "application/json".toMediaType(),
                    json
                )
                val request = Request.Builder()
                    .url("${BASE_URL}tts")
                    .post(body)
                    .build()

                activeStreamingCall = client.newCall(request)
                activeStreamingCall?.execute()?.use { response ->
                    if (!response.isSuccessful) {
                        Log.w(TAG, "Backend TTS failed: ${response.code}. Falling back.")
                        if (!activeStreamingCall!!.isCanceled()) {
                            ContextCompat.getMainExecutor(context).execute { fallbackToAndroidTts(text, speed) }
                        }
                        return@Thread
                    }

                    val inputStream =
                        response.body?.byteStream() ?: throw Exception("Empty response body")
                    val chunkFiles = mutableListOf<File>()

                    try {
                        val headerBuffer = ByteArray(44)
                        
                        while (true) {
                            // 1. Read the 44-byte WAV header
                            if (!readFully(inputStream, headerBuffer)) break
                            
                            // Check if it's a "RIFF" header
                            if (headerBuffer[0] != 'R'.toByte() || headerBuffer[1] != 'I'.toByte()) {
                                Log.w(TAG, "Missing RIFF header in stream, skipping byte")
                                continue 
                            }

                            // 2. Parse the data size (offset 40, 4 bytes, little-endian)
                            val dataSize = ByteBuffer.wrap(headerBuffer, 40, 4)
                                .order(ByteOrder.LITTLE_ENDIAN)
                                .int
                            
                            Log.d(TAG, "Incoming chunk detected: header 44b + data $dataSize bytes")

                            // 3. Create chunk file
                            val newChunkFile = File(tempDir, "chunk_${chunkIndex++}.wav")
                            newChunkFile.outputStream().use { out ->
                                out.write(headerBuffer)
                                
                                // Read the data payload
                                val dataBuffer = ByteArray(8192)
                                var remaining = dataSize
                                while (remaining > 0) {
                                    val toRead = remaining.coerceAtMost(dataBuffer.size)
                                    val read = inputStream.read(dataBuffer, 0, toRead)
                                    if (read == -1) break
                                    out.write(dataBuffer, 0, read)
                                    remaining -= read
                                }
                            }
                            
                            // 4. Immediately hand off to player (LOW LATENCY)
                            chunkFiles.add(newChunkFile)
                            ContextCompat.getMainExecutor(context).execute {
                                addChunkToPlayer(newChunkFile, speed)
                            }
                        }
                        
                        // 5. Create the combined file for caching (Properly joined)
                        if (chunkFiles.isNotEmpty()) {
                            val cacheFile = File(context.cacheDir, "tts_$answerId.wav")
                            concatWavFiles(chunkFiles, cacheFile)
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "Stream processing error: ${e.message}")
                    }

                    Log.i(TAG, "Streaming completed for $answerId. Chunks: ${chunkFiles.size}")
                }
            } catch (e: Exception) {
                if (activeStreamingCall?.isCanceled() == true) {
                    Log.i(TAG, "Streaming call was explicitly cancelled.")
                } else {
                    Log.e(TAG, "Streaming failed: ${e.message}")
                    ContextCompat.getMainExecutor(context).execute { fallbackToAndroidTts(text, speed) }
                }
            } finally {
                activeStreamingCall = null
            }
        }.start()
    }

    private fun readFully(inputStream: java.io.InputStream, buffer: ByteArray): Boolean {
        var offset = 0
        while (offset < buffer.size) {
            val read = inputStream.read(buffer, offset, buffer.size - offset)
            if (read == -1) return false
            offset += read
        }
        return true
    }

    private fun concatWavFiles(chunks: List<File>, target: File) {
        if (chunks.isEmpty()) return
        
        try {
            val firstChunk = chunks[0]
            firstChunk.copyTo(target, overwrite = true)
            
            val raf = RandomAccessFile(target, "rw")
            var totalDataSize = (target.length() - 44)
            
            // Append data from other chunks
            for (i in 1 until chunks.size) {
                val data = chunks[i].readBytes().sliceArray(44 until chunks[i].length().toInt())
                raf.seek(target.length())
                raf.write(data)
                totalDataSize += data.size
            }
            
            // Update Headers
            // 1. ChunkSize (offset 4): TotalLength - 8
            raf.seek(4)
            val totalLength = 8 + 36 + totalDataSize // RIFF (4) + format (36) + data (totalDataSize)
            val chunkSizeBuf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt((totalLength - 8).toInt())
            raf.write(chunkSizeBuf.array())
            
            // 2. Subchunk2Size (offset 40): totalDataSize
            raf.seek(40)
            val dataSizeBuf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(totalDataSize.toInt())
            raf.write(dataSizeBuf.array())
            
            raf.close()
            Log.i(TAG, "Persistent cache created: ${target.absolutePath} (${target.length()} bytes)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to concatenate WAVs: ${e.message}")
        }
    }

    private fun addChunkToPlayer(file: File, speed: Float) {
        ensureExoPlayer()
        exoPlayer?.apply {
            val mediaItem = MediaItem.fromUri(Uri.fromFile(file))
            addMediaItem(mediaItem)
            setPlaybackSpeed(speed.coerceIn(0.5f, 2.0f))

            when (playbackState) {
                Player.STATE_IDLE -> {
                    prepare()
                    play()
                }
                Player.STATE_ENDED -> {
                    // Start playing the newly added item
                    seekTo(mediaItemCount - 1, 0)
                    prepare()
                    play()
                }
                Player.STATE_READY -> {
                    if (!isPlaying) play()
                }
            }
        }
    }

    private fun playLocalFile(file: File, speed: Float) {
        ensureExoPlayer()
        currentAudioFile = file
        exoPlayer?.apply {
            clearMediaItems()
            setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            setPlaybackSpeed(speed.coerceIn(0.5f, 2.0f))
            prepare()
            play()
        }
    }

    fun pause() {
        ensureExoPlayer()
        exoPlayer?.pause()
        if (isUsingAndroidTts) androidTts?.stop()
        updateState(TTSState.PAUSED)
    }

    fun resume() {
        ensureExoPlayer()
        exoPlayer?.play()
        updateState(TTSState.PLAYING)
    }

    fun stop() {
        ensureExoPlayer()
        activeStreamingCall?.cancel()
        activeStreamingCall = null
        exoPlayer?.stop()
        androidTts?.stop()
        updateState(TTSState.IDLE)
    }

    fun seekForward() {
        ensureExoPlayer()
        exoPlayer?.let {
            it.seekTo(it.currentPosition + 10000)
        }
    }

    fun seekBackward() {
        ensureExoPlayer()
        exoPlayer?.let {
            it.seekTo(it.currentPosition - 10000)
        }
    }

    fun getDuration(): Long {
        ensureExoPlayer()
        return exoPlayer?.duration ?: 0L
    }

    fun getCurrentPosition(): Long {
        ensureExoPlayer()
        return exoPlayer?.currentPosition ?: 0L
    }

    fun seekTo(position: Long) {
        ensureExoPlayer()
        exoPlayer?.seekTo(position)
    }

    fun isPlaying(): Boolean = currentState == TTSState.PLAYING
    fun isPaused(): Boolean = currentState == TTSState.PAUSED
    fun isLoading(): Boolean = currentState == TTSState.LOADING
    fun getState(): TTSState = currentState

    fun setOnProgressListener(listener: ((Float) -> Unit)?) {
        progressListener = listener
    }

    fun release() {
        stop()
        exoPlayer?.release()
        exoPlayer = null
        androidTts?.shutdown()
        androidTts = null
    }

    // ═════════════════════════════════════════════════════════════════════════
    // TIER 3: ANDROID TTS (OFFLINE FALLBACK)
    // ═════════════════════════════════════════════════════════════════════════

    private fun initAndroidTts() {
        androidTts = TextToSpeech(context) { status ->
            androidTtsReady = status == TextToSpeech.SUCCESS
            if (androidTtsReady) {
                androidTts?.language = Locale.US
                Log.i(TAG, "Android TTS engine initialized")
            } else {
                Log.w(TAG, "Android TTS initialization failed")
            }
        }
    }

    private fun fallbackToAndroidTts(text: String, speed: Float) {
        isUsingAndroidTts = true
        speakWithAndroidTts(text, speed)
    }

    private fun speakWithAndroidTts(text: String, speed: Float) {
        ensureAndroidTts()
        androidTts?.let { tts ->
            tts.setSpeechRate(speed.coerceIn(0.5f, 2.0f))
            tts.setPitch(1.0f)

            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    updateState(TTSState.PLAYING)
                }

                override fun onDone(utteranceId: String?) {
                    updateState(TTSState.IDLE)
                }

                override fun onError(utteranceId: String?) {
                    updateState(TTSState.ERROR)
                }
            })

            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "sciai_tts")
            updateState(TTSState.PLAYING)
        }
    }

    private fun updateState(newState: TTSState) {
        currentState = newState
        stateListener?.invoke(newState)
    }
}

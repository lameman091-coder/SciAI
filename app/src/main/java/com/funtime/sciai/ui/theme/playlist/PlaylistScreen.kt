package com.funtime.sciai.ui.theme.playlist

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.funtime.sciai.components.AppScaffold
import com.funtime.sciai.data.tts.*
import com.funtime.sciai.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(navController: NavController, drawerState: androidx.compose.material3.DrawerState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val playlistManager = remember { PlaylistManager(context) }
    val ttsManager = remember { TTSManager(context) }
    
    var tracks by remember { mutableStateOf(playlistManager.getTracks().sortedByDescending { it.timestamp }) }
    var currentTrackId by remember { mutableStateOf<String?>(null) }
    var ttsState by remember { mutableStateOf(TTSState.IDLE) }
    var isRepeating by remember { mutableStateOf(false) }
    var isShuffle by remember { mutableStateOf(false) }
    


    LaunchedEffect(Unit) {
        ttsManager.setOnProgressListener { /* Optional: update track progress */ }
    }

    DisposableEffect(Unit) {
        onDispose {
            ttsManager.release()
        }
    }

    AppScaffold(
        title = "SciAI Playlist",
        navController = navController,
        showBack = false,
        onMenuClick = { scope.launch { drawerState.open() } },
        onBack = { navController.popBackStack() },
        actions = {
            IconButton(onClick = { isShuffle = !isShuffle }) {
                Icon(Icons.Default.Shuffle, null, tint = if (isShuffle) SciAICyan else Color.White)
            }
            IconButton(onClick = { isRepeating = !isRepeating }) {
                Icon(Icons.Default.Repeat, null, tint = if (isRepeating) SciAICyan else Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(SciAISurface)
        ) {
            if (tracks.isEmpty()) {
                EmptyPlaylistView()
            } else {
                // Currently Playing Track Header
                val currentTrack = tracks.find { it.id == currentTrackId }
                if (currentTrack != null) {
                    CurrentTrackHeader(currentTrack, ttsState) {
                        if (ttsState == TTSState.PLAYING) ttsManager.pause()
                        else if (ttsState == TTSState.PAUSED) ttsManager.resume()
                        else playTrack(currentTrack, ttsManager) { ttsState = it }
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "Your Saved Tracks",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    itemsIndexed(tracks) { index, track ->
                        val isPlaying = currentTrackId == track.id
                        TrackItem(
                            track = track,
                            isPlaying = isPlaying,
                            ttsState = ttsState,
                            onClick = {
                                if (isPlaying) {
                                    if (ttsState == TTSState.PLAYING) ttsManager.pause()
                                    else ttsManager.resume()
                                } else {
                                    currentTrackId = track.id
                                    playTrack(track, ttsManager) { ttsState = it }
                                }
                            },
                            onDelete = {
                                playlistManager.removeTrack(track.id)
                                tracks = tracks.filter { it.id != track.id }
                                if (currentTrackId == track.id) {
                                    ttsManager.stop()
                                    currentTrackId = null
                                }
                            }
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

private fun playTrack(track: Track, ttsManager: TTSManager, onStateChange: (TTSState) -> Unit) {
    ttsManager.stop()
    ttsManager.speak(
        text = if (track.text.isNotBlank()) track.text else track.title,
        answerId = track.id,
        voiceStyle = VoiceStyle.fromId(track.voiceStyleId),
        mode = track.mode,
        onStateChange = onStateChange
    )
}

@Composable
fun TrackItem(
    track: Track,
    isPlaying: Boolean,
    ttsState: TTSState,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(track.timestamp) {
        SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(track.timestamp))
    }

    Surface(
        onClick = onClick,
        color = if (isPlaying) Color.White.copy(alpha = 0.05f) else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        border = if (isPlaying) BorderStroke(1.dp, SciAICyan.copy(alpha = 0.3f)) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play Icon / Animation
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isPlaying) SciAICyan else Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying && ttsState == TTSState.PLAYING) {
                    Icon(Icons.Default.Pause, null, tint = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.Default.PlayArrow, null, tint = if (isPlaying) Color.White else Color.Gray, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = if (isPlaying) Color.White else Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(track.mode, color = SciAICyan.copy(alpha = 0.7f), fontSize = 11.sp)
                    Text(" • ", color = Color.Gray, fontSize = 11.sp)
                    Text(dateStr, color = Color.Gray, fontSize = 11.sp)
                    if (track.isDownloaded) {
                        Text(" • ", color = Color.Gray, fontSize = 11.sp)
                        Icon(Icons.Default.CloudDone, null, tint = SciAICyan.copy(alpha = 0.5f), modifier = Modifier.size(10.dp))
                    }
                }
            }



            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, null, tint = Color.Gray.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun CurrentTrackHeader(track: Track, ttsState: TTSState, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(SciAICyan.copy(alpha = 0.2f), Color.Transparent)))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Now Playing", color = SciAICyan.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(track.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            
            FloatingActionButton(
                onClick = onToggle,
                containerColor = SciAICyan,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (ttsState == TTSState.PLAYING) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
fun EmptyPlaylistView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.QueueMusic, null, tint = Color.White.copy(alpha = 0.1f), modifier = Modifier.size(100.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Your playlist is empty", color = Color.Gray, fontSize = 16.sp)
            Text("Save tracks from answers to see them here", color = Color.Gray.copy(alpha = 0.5f), fontSize = 12.sp)
        }
    }
}

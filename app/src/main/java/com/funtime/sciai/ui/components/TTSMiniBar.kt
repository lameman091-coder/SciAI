package com.funtime.sciai.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.funtime.sciai.data.tts.TTSState
import com.funtime.sciai.data.tts.VoiceStyle

// ── DESIGN TOKENS ────────────────────────────────────────────────────────────
private val TTSAccent = Color(0xFF7C3AED)
private val TTSSurface = Color(0xFF0F172A)

@Composable
fun TTSMiniBar(
    ttsState: TTSState,
    isEnabled: Boolean,
    selectedStyle: VoiceStyle,
    progress: Float,
    isSaved: Boolean = false,
    isDownloaded: Boolean = false,
    downloadProgress: Float = 0f,
    onToggle: () -> Unit,
    onPlayPause: () -> Unit,
    onSave: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isEnabled) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .shadow(8.dp, RoundedCornerShape(12.dp))
            .clickable { onToggle() },
        color = TTSSurface.copy(alpha = 0.95f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            0.5.dp,
            Brush.horizontalGradient(listOf(TTSAccent.copy(alpha = 0.4f), Color.Transparent))
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Voice Style Icon & Info
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(TTSAccent.copy(alpha = 0.15f))
                            .border(1.dp, TTSAccent.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = selectedStyle.emoji, fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = selectedStyle.displayName,
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when(ttsState) {
                                TTSState.PLAYING -> "Speaking..."
                                TTSState.PAUSED -> "Paused"
                                TTSState.LOADING -> "Generating..."
                                else -> "Ready"
                            },
                            color = Color.White.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                // Controls Group
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Save Button
                    IconButton(
                        onClick = onSave,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Default.BookmarkAdded else Icons.Default.BookmarkAdd,
                            contentDescription = "Save",
                            tint = if (isSaved) TTSAccent else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Download Button
                    IconButton(
                        onClick = onDownload,
                        enabled = !isDownloaded,
                        modifier = Modifier.size(32.dp)
                    ) {
                        if (downloadProgress > 0f && downloadProgress < 1f) {
                            CircularProgressIndicator(
                                progress = downloadProgress,
                                modifier = Modifier.size(16.dp),
                                color = TTSAccent,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (isDownloaded) Icons.Default.CloudDone else Icons.Default.CloudDownload,
                                contentDescription = "Download",
                                tint = if (isDownloaded) Color(0xFF10B981) else Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Play/Pause Mini Control
                    IconButton(
                        onClick = { onPlayPause() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (ttsState == TTSState.PLAYING) TTSAccent else Color.White.copy(alpha = 0.1f))
                    ) {
                        if (ttsState == TTSState.LOADING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (ttsState == TTSState.PLAYING) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Progress Line (Animated)
            val animatedProgress by animateFloatAsState(
                targetValue = progress.coerceIn(0f, 1f),
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "progress"
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(animatedProgress)
                    .height(3.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(TTSAccent, Color(0xFFA78BFA))
                        )
                    )
            )
        }
    }
}

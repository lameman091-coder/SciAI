package com.funtime.sciai.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.funtime.sciai.data.tts.TTSState
import com.funtime.sciai.data.tts.VoiceStyle
import com.funtime.sciai.ui.theme.*
import kotlinx.coroutines.delay

// ── DESIGN TOKENS ────────────────────────────────────────────────────────────
private val TTSAccent = Color(0xFF7C3AED)       // Vivid purple for TTS
private val TTSAccentLight = Color(0xFFA78BFA)   // Lighter purple
private val TTSPlayGreen = Color(0xFF10B981)     // Emerald green
private val TTSSurface = Color(0xFF0F172A)

// ═══════════════════════════════════════════════════════════════════════════════
// MAIN TTS TOGGLE BUTTON (Floating Action Style)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun TTSFloatingToggle(
    isEnabled: Boolean,
    ttsState: TTSState,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val infiniteTransition = rememberInfiniteTransition(label = "ttsGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    val scale by animateFloatAsState(
        targetValue = if (ttsState == TTSState.PLAYING) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "scaleAnim"
    )

    Box(
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .size(44.dp)
            .shadow(
                elevation = if (isEnabled) 12.dp else 4.dp,
                shape = CircleShape,
                ambientColor = if (ttsState == TTSState.PLAYING) TTSAccent.copy(alpha = glowAlpha) else Color.Transparent,
                spotColor = if (ttsState == TTSState.PLAYING) TTSAccent.copy(alpha = glowAlpha) else Color.Transparent
            )
            .clip(CircleShape)
            .background(
                brush = if (isEnabled) {
                    Brush.radialGradient(
                        colors = listOf(TTSAccent, Color(0xFF5B21B6))
                    )
                } else {
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                    )
                }
            )
            .border(
                width = 1.5.dp,
                brush = if (ttsState == TTSState.PLAYING) {
                    Brush.sweepGradient(listOf(TTSAccentLight, TTSPlayGreen, TTSAccentLight))
                } else if (isEnabled) {
                    Brush.linearGradient(listOf(TTSAccentLight.copy(alpha = 0.6f), TTSAccent.copy(alpha = 0.3f)))
                } else {
                    Brush.linearGradient(listOf(Color.White.copy(alpha = 0.1f), Color.White.copy(alpha = 0.05f)))
                },
                shape = CircleShape
            )
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggle()
            },
        contentAlignment = Alignment.Center
    ) {
        Crossfade(targetState = ttsState, label = "ttsIcon") { state ->
            when (state) {
                TTSState.LOADING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                }
                TTSState.PLAYING -> {
                    WaveformIcon(color = Color.White)
                }
                else -> {
                    Icon(
                        imageVector = if (isEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = "TTS Toggle",
                        tint = if (isEnabled) Color.White else Color.Gray,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun WaveformIcon(color: Color, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")

    val heights = (0 until 4).map { index ->
        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 400 + (index * 120),
                    easing = EaseInOutSine
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar$index"
        )
    }

    Row(
        modifier = modifier.height(20.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        heights.forEach { heightFraction ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(heightFraction.value)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(color)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TTS CONTROL PANEL (Advanced Playback Controls)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun TTSControlPanel(
    isVisible: Boolean,
    ttsState: TTSState,
    selectedStyle: VoiceStyle,
    speed: Float,
    currentPosition: Long,
    duration: Long,
    onStyleSelect: (VoiceStyle) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onSeek: (Long) -> Unit,
    onRewind: () -> Unit,
    onForward: () -> Unit,
    onSave: () -> Unit,
    isSaved: Boolean = false,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(300)) + slideInVertically(tween(400)) { it },
        exit = fadeOut(tween(200)) + slideOutVertically(tween(300)) { it },
        modifier = modifier
    ) {
        Surface(
            color = TTSSurface.copy(alpha = 0.97f),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            border = BorderStroke(
                1.dp,
                Brush.verticalGradient(
                    listOf(TTSAccentLight.copy(alpha = 0.4f), TTSAccent.copy(alpha = 0.1f))
                )
            ),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Handle bar
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ── Progress & Seek Bar ──
                AnimatedVisibility(
                    visible = ttsState == TTSState.PLAYING || ttsState == TTSState.PAUSED || ttsState == TTSState.LOADING
                ) {
                    Column {
                        val progress = if (duration > 0) {
                            (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                        } else 0f
                        
                        Slider(
                            value = progress,
                            onValueChange = { onSeek((it * duration).toLong()) },
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = TTSAccent,
                                inactiveTrackColor = TTSAccent.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                formatTime(currentPosition),
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 10.sp
                            )
                            Text(
                                formatTime(duration),
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 10.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // ── Playback Controls ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Speed selector
                    SpeedSelector(
                        speed = speed,
                        onSpeedChange = onSpeedChange
                    )

                    // Control Group
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rewind -10s
                        IconButton(onClick = onRewind, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Replay10, "Rewind", tint = Color.White.copy(alpha = 0.8f))
                        }

                        // Play/Pause button
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(TTSAccent, Color(0xFF5B21B6))))
                                .border(1.dp, TTSAccentLight.copy(alpha = 0.4f), CircleShape)
                                .clickable { onPlayPause() },
                            contentAlignment = Alignment.Center
                        ) {
                            Crossfade(targetState = ttsState, label = "playPause") { state ->
                                when (state) {
                                    TTSState.LOADING -> CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                                    TTSState.PLAYING -> Icon(Icons.Default.Pause, "Pause", tint = Color.White, modifier = Modifier.size(32.dp))
                                    else -> Icon(Icons.Default.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(32.dp))
                                }
                            }
                        }

                        // Forward +10s
                        IconButton(onClick = onForward, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Forward10, "Forward", tint = Color.White.copy(alpha = 0.8f))
                        }

                        // Save to Playlist
                        IconButton(
                            onClick = onSave,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSaved) SciAICyan.copy(alpha = 0.15f)
                                    else Color.White.copy(alpha = 0.05f)
                                )
                        ) {
                            Icon(
                                imageVector = if (isSaved) Icons.Default.BookmarkAdded else Icons.Default.BookmarkAdd,
                                contentDescription = "Save to Library",
                                tint = if (isSaved) SciAICyan else Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }


                        // Stop
                        IconButton(
                            onClick = onStop,
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f))
                        ) {
                            Icon(Icons.Default.Close, "Stop", tint = SciAIRed, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun VoiceStyleChip(style: VoiceStyle, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick() },
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) TTSAccent.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f),
        border = BorderStroke(1.dp, if (isSelected) TTSAccent else Color.White.copy(alpha = 0.08f)),
        modifier = modifier
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)) {
            Text(text = style.emoji, fontSize = 22.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = style.displayName, color = if (isSelected) Color.White else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SpeedSelector(speed: Float, onSpeedChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Speed, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        speeds.forEach { s ->
            val isSelected = speed == s
            Surface(
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onSpeedChange(s) },
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) TTSAccent.copy(alpha = 0.2f) else Color.Transparent,
                border = BorderStroke(0.5.dp, if (isSelected) TTSAccent.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f))
            ) {
                Text(text = if (s == 1.0f) "1x" else "${s}x", color = if (isSelected) Color.White else Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0 || ms == Long.MIN_VALUE) return "00:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

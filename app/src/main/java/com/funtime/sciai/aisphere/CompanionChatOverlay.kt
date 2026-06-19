package com.funtime.sciai.aisphere

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.funtime.sciai.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Full-screen glassmorphic chat interface for the AI Companion Manager.
 * Blurs the underlying app content for a premium, focused experience.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanionChatOverlay(
    visible: Boolean,
    messages: List<ChatMessage>,
    isLoading: Boolean,
    companionProfile: CompanionProfile,
    onDismiss: () -> Unit,
    onSendMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 4 },
        exit = fadeOut(tween(300)) + slideOutVertically(tween(300)) { it / 4 }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f)) // Slightly darker dim
        ) {
            // ── Background Blur Layer ───────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(25.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                SciAISurface.copy(alpha = 0.9f),
                                Color(0xFF0F172A).copy(alpha = 0.98f)
                            )
                        )
                    )
                    .clickable { onDismiss(); focusManager.clearFocus() }
            )

            // ── Main Chat Container ─────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
            ) {
                // ── Header ──────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // ── LIVE ANIMATED PROFILE ───────────────────
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                                .padding(4.dp)
                        ) {
                            AISphereComposable(
                                emotionState = messages.lastOrNull { !it.isUser }?.emotion ?: EmotionState.HAPPY,
                                bounceScale = 1f,
                                shakeOffset = 0f,
                                compressScale = 1f,
                                droopOffset = 0f,
                                tiltAngle = 0f,
                                reduceAnimations = false,
                                companionProfile = companionProfile,
                                size = 42.dp
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = companionProfile.name,
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = "Intelligent Research Butler",
                                color = SciAICyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }

                // ── Message List ────────────────────────────────────
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        ChatBubble(message, companionProfile)
                    }

                    if (isLoading) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.CenterStart) {
                                typingIndicator(companionProfile.colorTheme.primary)
                            }
                        }
                    }
                }

                // ── Input Field ─────────────────────────────────────
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp, top = 12.dp)
                        .imePadding(),
                    shape = RoundedCornerShape(28.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(4.dp)
                            .padding(start = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Command SciAI...", color = Color.White.copy(alpha = 0.4f), fontSize = 15.sp) },
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = SciAICyan,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White.copy(alpha = 0.9f)
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (inputText.isNotBlank()) {
                                    onSendMessage(inputText)
                                    inputText = ""
                                }
                            })
                        )

                        FloatingActionButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    onSendMessage(inputText)
                                    inputText = ""
                                }
                            },
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            containerColor = companionProfile.colorTheme.primary,
                            contentColor = Color.Black,
                            elevation = FloatingActionButtonDefaults.elevation(0.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage, profile: CompanionProfile) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (message.isUser) SciAICyan.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.08f)
    val borderColor = if (message.isUser) SciAICyan.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.15f)
    val textColor = if (message.isUser) SciAICyan else Color.White

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(
            horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Surface(
                color = bgColor,
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (message.isUser) 18.dp else 4.dp,
                    bottomEnd = if (message.isUser) 4.dp else 18.dp
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
            ) {
                Text(
                    text = message.text,
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            if (!message.isUser && message.emotion != null) {
                Text(
                    text = "Mood: ${message.emotion.name.lowercase().capitalize()}",
                    color = SciAISubtext,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )
            }
        }
    }
}

@Composable
fun typingIndicator(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 0), RepeatMode.Reverse), label = "dot1"
    )
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 200), RepeatMode.Reverse), label = "dot2"
    )
    val alpha3 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 400), RepeatMode.Reverse), label = "dot3"
    )

    Row(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(modifier = Modifier.size(6.dp).background(color.copy(alpha = alpha1), CircleShape))
        Box(modifier = Modifier.size(6.dp).background(color.copy(alpha = alpha2), CircleShape))
        Box(modifier = Modifier.size(6.dp).background(color.copy(alpha = alpha3), CircleShape))
    }
}

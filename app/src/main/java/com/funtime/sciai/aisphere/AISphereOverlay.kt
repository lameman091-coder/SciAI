package com.funtime.sciai.aisphere

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Top-level overlay that wraps the entire app content.
 * Renders the AI Sphere, message bubbles, and quick actions on top of everything.
 *
 * Enhanced with:
 * - SHY compress animation
 * - SAD droop offset
 * - CONFUSED tilt
 * - Emotion-specific bounce behaviors
 */
@Composable
fun AISphereOverlay(
    navController: NavController,
    viewModel: AISphereViewModel,
    onOpenSettings: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val emotionState by viewModel.emotionState.collectAsState()
    val currentMessage by viewModel.currentMessage.collectAsState()
    val isVisible by viewModel.isVisible.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val spherePosition by viewModel.spherePosition.collectAsState()
    val showQuickActions by viewModel.showQuickActions.collectAsState()
    val reduceAnimations by viewModel.reduceAnimations.collectAsState()
    val companionProfile by viewModel.companionProfile.collectAsState()

    // Screen dimensions for clamping
    var screenWidth by remember { mutableFloatStateOf(0f) }
    var screenHeight by remember { mutableFloatStateOf(0f) }

    val density = LocalDensity.current
    val sphereSizeDp = 64.dp
    val sphereSizePx = with(density) { sphereSizeDp.toPx() }

    // ── Bounce animation (tap response) ─────────────────────────────
    val bounceAnim = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    // ── Shake animation (ANGRY state) ───────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "overlay")
    val shakeOffset by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(60, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shake"
    )
    val actualShake = if (emotionState == EmotionState.ANGRY && !reduceAnimations) shakeOffset else 0f

    // ── SHY compress animation ──────────────────────────────────────
    val compressScale by animateFloatAsState(
        targetValue = if (emotionState == EmotionState.SHY) 0.85f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 600f),
        label = "compress"
    )

    // ── SAD droop animation ─────────────────────────────────────────
    val droopOffset by animateFloatAsState(
        targetValue = if (emotionState == EmotionState.SAD) 6f else 0f,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "droop"
    )

    // ── CONFUSED tilt animation ─────────────────────────────────────
    val tiltAngle by animateFloatAsState(
        targetValue = when (emotionState) {
            EmotionState.CONFUSED -> 12f
            EmotionState.SHY -> -5f
            else -> 0f
        },
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "tilt"
    )

    // ── Pet detector ────────────────────────────────────────────────
    val petDetector = remember { PetDetector() }

    // Initialize position on first layout
    LaunchedEffect(screenWidth, screenHeight) {
        if (screenWidth > 0 && screenHeight > 0) {
            viewModel.initializePosition(screenWidth, screenHeight)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                screenWidth = coordinates.size.width.toFloat()
                screenHeight = coordinates.size.height.toFloat()
            }
    ) {
        // ── App content (below sphere) ──────────────────────────────
        content()

        // ── AI Sphere overlay ───────────────────────────────────────
        if (isVisible) {

            // ── Message bubble ──────────────────────────────────
            currentMessage?.let { message ->
                MessageBubble(
                    message = message,
                    companionProfile = companionProfile,
                    sphereX = spherePosition.x,
                    sphereY = spherePosition.y,
                    sphereSize = sphereSizePx,
                    screenWidth = screenWidth,
                    onDismiss = { viewModel.dismissMessage() }
                )
            }

            // ── The Sphere itself ───────────────────────────────
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            spherePosition.x.roundToInt(),
                            spherePosition.y.roundToInt()
                        )
                    }
                    .size(sphereSizeDp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                viewModel.onTap()
                                // Trigger bounce
                                scope.launch {
                                    bounceAnim.animateTo(
                                        targetValue = 1.25f,
                                        animationSpec = spring(
                                            dampingRatio = 0.4f,
                                            stiffness = 800f
                                        )
                                    )
                                    bounceAnim.animateTo(
                                        targetValue = 1f,
                                        animationSpec = spring(
                                            dampingRatio = 0.5f,
                                            stiffness = 400f
                                        )
                                    )
                                }
                            },
                            onLongPress = {
                                viewModel.onLongPress()
                                // Gentle expand for LOVE
                                scope.launch {
                                    bounceAnim.animateTo(
                                        1.15f,
                                        spring(dampingRatio = 0.6f, stiffness = 300f)
                                    )
                                    bounceAnim.animateTo(
                                        1f,
                                        spring(dampingRatio = 0.7f, stiffness = 200f)
                                    )
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {
                                viewModel.onDragStart()
                                petDetector.reset()
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                viewModel.onDrag(dragAmount)
                                viewModel.clampPosition(screenWidth, screenHeight, sphereSizePx)

                                // Feed drag to pet detector
                                if (petDetector.onDrag(dragAmount.x)) {
                                    viewModel.onPet()
                                    // Trigger excited bounce
                                    scope.launch {
                                        bounceAnim.animateTo(
                                            1.3f,
                                            spring(dampingRatio = 0.3f, stiffness = 600f)
                                        )
                                        bounceAnim.animateTo(
                                            1f,
                                            spring(dampingRatio = 0.4f, stiffness = 300f)
                                        )
                                    }
                                }
                            },
                            onDragEnd = {
                                viewModel.onDragEnd()
                                petDetector.reset()
                            }
                        )
                    }
            ) {
                AISphereComposable(
                    emotionState = emotionState,
                    bounceScale = bounceAnim.value,
                    shakeOffset = actualShake,
                    compressScale = compressScale,
                    droopOffset = droopOffset,
                    tiltAngle = tiltAngle,
                    reduceAnimations = reduceAnimations,
                    companionProfile = companionProfile,
                    size = sphereSizeDp
                )
            }

            // ── Quick actions menu ──────────────────────────────
            QuickActionsMenu(
                visible = showQuickActions,
                sphereX = spherePosition.x,
                sphereY = spherePosition.y,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                isMuted = isMuted,
                onDismiss = { viewModel.dismissQuickActions() },
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo("home") { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onToggleMute = { viewModel.toggleMute() },
                onHideSphere = { viewModel.toggleVisibility() },
                onOpenSettings = {
                    onOpenSettings()
                }
            )
        }
    }
}

/**
 * Message bubble shown near the sphere.
 * Enhanced with emotion-aware gradient tinting.
 */
@Composable
private fun MessageBubble(
    message: SphereMessage,
    companionProfile: CompanionProfile,
    sphereX: Float,
    sphereY: Float,
    sphereSize: Float,
    screenWidth: Float,
    onDismiss: () -> Unit
) {
    val bubbleWidth = 200.dp
    val density = LocalDensity.current
    val bubbleWidthPx = with(density) { bubbleWidth.toPx() }

    val bubbleX = (sphereX + sphereSize / 2 - bubbleWidthPx / 2)
        .coerceIn(12f, screenWidth - bubbleWidthPx - 12f)
    val bubbleY = (sphereY - with(density) { 70.dp.toPx() })
        .coerceAtLeast(12f)

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(300)) + slideInVertically(
            animationSpec = spring(dampingRatio = 0.7f),
            initialOffsetY = { it / 3 }
        ),
        exit = fadeOut(tween(200))
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(bubbleX.roundToInt(), bubbleY.roundToInt()) }
                .width(bubbleWidth)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E293B).copy(alpha = 0.92f),
                            Color(0xFF0F172A).copy(alpha = 0.96f)
                        )
                    )
                )
                .pointerInput(Unit) {
                    detectTapGestures { onDismiss() }
                }
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                Text(
                    text = "${companionProfile.name} says:",
                    color = companionProfile.colorTheme.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Text(
                    text = message.text,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

package com.funtime.sciai.aisphere

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Quick actions menu shown on long-press of the AI Sphere.
 * Glassmorphic popup with navigation options and sphere controls.
 */
@Composable
fun QuickActionsMenu(
    visible: Boolean,
    sphereX: Float,
    sphereY: Float,
    screenWidth: Float,
    screenHeight: Float,
    isMuted: Boolean,
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit,
    onToggleMute: () -> Unit,
    onHideSphere: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
            initialScale = 0.4f,
            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
        ) + fadeIn(animationSpec = tween(200)),
        exit = scaleOut(
            animationSpec = tween(150),
            targetScale = 0.6f
        ) + fadeOut(animationSpec = tween(150))
    ) {
        // Calculate position — show above the sphere, adjusted for screen edges
        val menuWidth = 220.dp
        val menuWidthPx = 220f * 2.5f // Approximate
        val xOffset = (sphereX - menuWidthPx / 2).coerceIn(16f, screenWidth - menuWidthPx - 16f)
        val yOffset = (sphereY - 400f).coerceAtLeast(16f) // 400px above sphere

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) { onDismiss() }
        ) {
            Card(
                modifier = Modifier
                    .offset { IntOffset(xOffset.toInt(), yOffset.toInt()) }
                    .width(menuWidth)
                    .clickable(enabled = false) {}, // Prevent click-through
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0F172A).copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1E293B).copy(alpha = 0.8f),
                                    Color(0xFF0F172A).copy(alpha = 0.95f)
                                )
                            )
                        )
                        .padding(8.dp)
                ) {
                    // Header
                    Text(
                        text = "Quick Actions",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )

                    // Navigation actions
                    QuickActionItem(
                        icon = Icons.Default.Home,
                        label = "Go to Home",
                        color = Color(0xFF38BDF8)
                    ) {
                        onNavigate("home")
                        onDismiss()
                    }

                    QuickActionItem(
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        label = "Open Library",
                        color = Color(0xFF34D399)
                    ) {
                        onNavigate("library")
                        onDismiss()
                    }

                    QuickActionItem(
                        icon = Icons.Default.Science,
                        label = "Research Articles",
                        color = Color(0xFFA78BFA)
                    ) {
                        onNavigate("articles")
                        onDismiss()
                    }

                    Divider(
                        color = Color(0xFF334155),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )

                    // Sphere controls
                    QuickActionItem(
                        icon = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        label = if (isMuted) "Unmute Messages" else "Mute Messages",
                        color = Color(0xFFFBBF24)
                    ) {
                        onToggleMute()
                        onDismiss()
                    }

                    QuickActionItem(
                        icon = Icons.Default.VisibilityOff,
                        label = "Hide Sphere",
                        color = Color(0xFFF87171)
                    ) {
                        onHideSphere()
                        onDismiss()
                    }

                    QuickActionItem(
                        icon = Icons.Default.Settings,
                        label = "Sphere Settings",
                        color = Color(0xFF94A3B8)
                    ) {
                        onOpenSettings()
                        onDismiss()
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionItem(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

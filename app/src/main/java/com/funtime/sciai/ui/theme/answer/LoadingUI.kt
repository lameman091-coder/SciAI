package com.funtime.sciai.ui.theme.answer

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun LoadingUI() {
    val infiniteTransition = rememberInfiniteTransition()

    // Orb Pulse Animation (Breathing effect)
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Orb Glow Animation
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Dynamic Text Cycle
    val loadingPhrases = listOf("Thinking...", "Analyzing Context...", "Generating Response...")
    var currentPhraseIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1800)
            currentPhraseIndex = (currentPhraseIndex + 1) % loadingPhrases.size
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            // Pulse Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2f
                
                // Outer Cyan Glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF38BDF8).copy(alpha = alpha), // Light Cyan glow
                            Color.Transparent
                        )
                    ),
                    radius = radius * scale
                )
                
                // Deep Blue Solid Core
                drawCircle(
                    color = Color(0xFF0284C7),
                    radius = radius * 0.3f * scale
                )
                
                // Pure White intense center dot
                drawCircle(
                    color = Color.White,
                    radius = radius * 0.1f * scale
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = loadingPhrases[currentPhraseIndex],
            color = Color(0xFF7DD3FC), // Sky 300
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.2.sp
        )
    }
}
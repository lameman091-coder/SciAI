package com.funtime.sciai.ui.theme.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.funtime.sciai.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import com.funtime.sciai.data.UserManager

@Composable
fun SplashScreen(navController: NavController) {
    val context = LocalContext.current
    val userManager = UserManager(context)
    val userName = userManager.getName()

    // Animations
    val phoenixOffsetY = remember { Animatable(2500f) } // Starts below screen
    val phoenixScale = remember { Animatable(1f) }
    
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.85f) }
    
    val textAlpha1 = remember { Animatable(0f) }
    val textAlpha2 = remember { Animatable(0f) }
    val textAlpha3 = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // 1. Phoenix Swoop Exit
        launch {
            phoenixScale.animateTo(
                targetValue = 1.3f,
                animationSpec = tween(durationMillis = 1500, easing = LinearOutSlowInEasing)
            )
        }
        phoenixOffsetY.animateTo(
            targetValue = -2500f, // Exits completely above
            animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
        )

        // Delay 200ms
        delay(250)

        // 2. Logo Fade & Scale
        launch {
            logoAlpha.animateTo(1f, animationSpec = tween(600))
        }
        launch {
            logoScale.animateTo(1f, animationSpec = tween(700, easing = FastOutSlowInEasing))
        }

        // 3. Staggered Typography
        delay(300)
        launch { textAlpha1.animateTo(1f, animationSpec = tween(500)) } // "SciAi"
        delay(200)
        launch { textAlpha2.animateTo(1f, animationSpec = tween(500)) } // Tagline
        delay(300)
        launch { textAlpha3.animateTo(1f, animationSpec = tween(500)) } // Welcome

        // Hold for reading
        delay(1200)

        // 4. Navigate to Home
        navController.navigate("home") {
            popUpTo("splash") { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)), // Deep premium dark blue
        contentAlignment = Alignment.Center
    ) {

        // --- BRANDING REVEAL LAYER ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            
            // Try attempting to load the custom provided logo "image_1", fallback if missing
            val logoResId = context.resources.getIdentifier("image_1", "drawable", context.packageName)
            val drawableId = if (logoResId != 0) logoResId else R.drawable.img
            
            Image(
                painter = painterResource(id = drawableId),
                contentDescription = "SciAI Logo",
                modifier = Modifier
                    .size(140.dp)
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "SciAI",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alpha(textAlpha1.value)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Seriously Smart. Surprisingly Fun",
                color = Color(0xFF94A3B8), // Slate gray complementary
                fontSize = 14.sp,
                modifier = Modifier.alpha(textAlpha2.value)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = if (userName != null) "Welcome, $userName" else "Welcome",
                color = Color(0xFF38BDF8), // Cyan/Blue accent
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.alpha(textAlpha3.value)
            )
        }

        // --- PHOENIX ANIMATION LAYER ---
        // Exists purely to fly over the screen and vanish
        if (phoenixOffsetY.value > -2000f) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(0, phoenixOffsetY.value.toInt()) }
                    .scale(phoenixScale.value)
            ) {
                val path = Path()
                val width = size.width
                val height = size.height
                
                // Abstract sweeping phoenix shape (red/orange)
                path.moveTo(width / 2f, height * 0.2f) // Top beak
                path.quadraticBezierTo(width * 0.8f, height * 0.4f, width, height * 0.3f) // Right wing tip
                path.quadraticBezierTo(width * 0.7f, height * 0.6f, width / 2f, height * 0.8f) // Right tail
                path.quadraticBezierTo(width * 0.3f, height * 0.6f, 0f, height * 0.3f) // Left wing tip
                path.quadraticBezierTo(width * 0.2f, height * 0.4f, width / 2f, height * 0.2f) // Back to beak
                
                drawPath(
                    path = path,
                    color = Color(0xFFEF4444) // Vibrant Red
                )
            }
        }
    }
}
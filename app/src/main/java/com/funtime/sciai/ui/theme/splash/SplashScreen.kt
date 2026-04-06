package com.funtime.sciai.ui.theme.splash

import androidx.compose.animation.core.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.funtime.sciai.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.funtime.sciai.data.UserManager

@Composable
fun SplashScreen(navController: NavController) {
    val context = LocalContext.current
    val userManager = UserManager(context)
    val userName = userManager.getName()

    // Clean, premium animations
    val logoScale = remember { Animatable(0.5f) }
    val logoAlpha = remember { Animatable(0f) }
    
    val textAlpha = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // 1. Initial Logo scale & fade-in (ChatGPT style)
        launch {
            logoAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 800, easing = EaseOutCubic)
            )
        }
        logoScale.animateTo(
            targetValue = 1.0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )

        // 2. Slow fade in text
        delay(300)
        launch { 
            textAlpha.animateTo(
                1f, 
                animationSpec = tween(600, easing = EaseInOutSine)
            ) 
        }
        
        delay(300)
        launch { 
            subtitleAlpha.animateTo(
                1f, 
                animationSpec = tween(600, easing = EaseInOutSine)
            ) 
        }

        // 3. Short hold on the splash
        delay(1200)

        // 4. Subtle scale out before exiting
        launch {
            logoAlpha.animateTo(0f, animationSpec = tween(400))
        }
        launch {
            textAlpha.animateTo(0f, animationSpec = tween(300))
        }
        launch {
            subtitleAlpha.animateTo(0f, animationSpec = tween(200))
        }
        logoScale.animateTo(0.9f, animationSpec = tween(400))

        val prefs = com.funtime.sciai.aisphere.AISpherePreferences(context)
        val nextRoute = if (prefs.hasCompletedSetup) "home" else "companion_setup"
        navController.navigate(nextRoute) {
            popUpTo("splash") { inclusive = true }
        }
    }

    // Premium Solid Dark Background (like modern AI tools)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A)), // True dark UI
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // Logo Image
            val logoResId = context.resources.getIdentifier("newlogo", "drawable", context.packageName)
            val drawableId = if (logoResId != 0) logoResId else R.drawable.img
            
            Image(
                painter = painterResource(id = drawableId),
                contentDescription = "SciAI Logo",
                modifier = Modifier
                    .size(100.dp)
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Premium Typography
            Text(
                text = "SciAI",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                modifier = Modifier.alpha(textAlpha.value)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (userName != null) "Welcome back, $userName" else "Seriously Smart. Surprisingly Fun.",
                color = Color(0xFFA1A1AA), // Zinc-400 equivalent for clean look
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.5.sp,
                modifier = Modifier.alpha(subtitleAlpha.value)
            )
        }
    }
}
package com.funtime.sciai.ui.theme.splash



import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.funtime.sciai.R
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import com.funtime.sciai.data.UserManager

@Composable
fun SplashScreen(navController: NavController) {
    val scale = remember { Animatable(0.8f) }
    val context = LocalContext.current
    val userManager = UserManager(context)
    val userName = userManager.getName()



            LaunchedEffect(Unit) {
                delay(2000) // 2 seconds
                navController.navigate("home") {
                    popUpTo("splash") { inclusive = true }

                }
            }
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(1200)
        )

        delay(800)

        navController.navigate("home") {
            popUpTo("splash") { inclusive = true }
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {

            Image(
                painter = painterResource(id = R.drawable.img),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(160.dp)
                    .scale(scale.value)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "SciAI",
                color = Color.White,
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (userName != null) "Welcome, $userName" else "Welcome",
                color = Color.Gray
            )
        }
    }
}
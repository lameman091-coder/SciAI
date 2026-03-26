package com.funtime.sciai.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.*
import com.funtime.sciai.ui.theme.home.HomeScreen
import com.funtime.sciai.ui.theme.answer.AnswerScreen
import com.funtime.sciai.ui.theme.splash.SplashScreen


@Composable
fun AppNav() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {

        composable("splash") {
            SplashScreen(navController)
        }

        composable("home") {
            HomeScreen(navController)
        }
        composable("answer/{question}/{mode}") { backStackEntry ->

            val question =
                backStackEntry.arguments?.getString("question") ?: ""

            val mode =
                backStackEntry.arguments?.getString("mode") ?: "Exam"

            AnswerScreen(
                question = question,
                mode = mode,
                navController = navController
            )
        }
    }
}
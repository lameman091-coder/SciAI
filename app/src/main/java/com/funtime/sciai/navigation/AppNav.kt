package com.funtime.sciai.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.*
import com.funtime.sciai.ui.theme.home.HomeScreen
import com.funtime.sciai.ui.theme.answer.AnswerScreen
import com.funtime.sciai.ui.theme.splash.SplashScreen
import com.funtime.sciai.ui.theme.library.LibraryScreen
import com.funtime.sciai.ui.theme.library.LibraryDetailScreen
import com.funtime.sciai.ui.theme.library.ArticlesScreen


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

        composable("library") {
            LibraryScreen(navController)
        }

        composable("libraryDetail/{bookId}") { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
            LibraryDetailScreen(bookId = bookId, navController = navController)
        }
        
        composable("articles") {
            ArticlesScreen(navController)
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
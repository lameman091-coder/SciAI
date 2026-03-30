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
        composable("answer/{question}/{mode}?bookId={bookId}&hybrid={hybrid}") { backStackEntry ->

            val question =
                backStackEntry.arguments?.getString("question") ?: ""

            val mode =
                backStackEntry.arguments?.getString("mode") ?: "Exam"

            val bookId = 
                backStackEntry.arguments?.getString("bookId")

            val hybrid = 
                backStackEntry.arguments?.getString("hybrid")?.toBoolean() ?: false

            AnswerScreen(
                question = question,
                mode = mode,
                bookId = bookId,
                hybrid = hybrid,
                navController = navController
            )
        }
        
        composable("article_detail?title={title}&summary={summary}&source={source}") { backStackEntry ->
            val title = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("title") ?: "", "UTF-8")
            val summary = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("summary") ?: "", "UTF-8")
            val source = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("source") ?: "", "UTF-8")
            
            com.funtime.sciai.ui.theme.library.ArticleDetailScreen(
                title = title,
                summary = summary,
                source = source,
                navController = navController
            )
        }
    }
}
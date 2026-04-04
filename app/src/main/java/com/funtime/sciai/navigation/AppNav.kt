package com.funtime.sciai.navigation

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.funtime.sciai.ui.theme.home.HomeScreen
import com.funtime.sciai.ui.theme.answer.AnswerScreen
import com.funtime.sciai.ui.theme.splash.SplashScreen
import com.funtime.sciai.ui.theme.library.*
import com.funtime.sciai.aisphere.*
import com.funtime.sciai.data.HistoryItem
import com.funtime.sciai.data.HistoryManager
import com.funtime.sciai.data.UserManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNav() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val aiSphereViewModel: AISphereViewModel = viewModel()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val isNavigatingState = remember { mutableStateOf(false) }

    // ── History State ──
    val userManager = remember { com.funtime.sciai.data.UserManager(context) }
    var historyList by remember { mutableStateOf<List<com.funtime.sciai.data.HistoryItem>>(emptyList()) }

    // ✅ Refresh history when drawer opens or screen change
    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen) {
            historyList = com.funtime.sciai.data.HistoryManager.getHistory(context)
        }
    }

    // ✅ Safe navigation tracking
    val currentBackStackEntry by navController.currentBackStackEntryAsState()

    LaunchedEffect(currentBackStackEntry) {
        aiSphereViewModel.onScreenChanged(
            currentBackStackEntry?.destination?.route ?: ""
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isNavigatingState.value, // Disable gestures during transition
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF1E293B) // Premium Dark Slate
            ) {
                Text(
                    text = "SciAI Navigation",
                    modifier = Modifier.padding(16.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )

                HorizontalDivider(color = Color.DarkGray)

                DrawerItem("Home", Color.White, enabled = !isNavigatingState.value) {
                    scope.launch {
                        safeNavigate(navController, drawerState, "home", isNavigatingState)
                    }
                }
                DrawerItem("Library", Color.White, enabled = !isNavigatingState.value) {
                    scope.launch {
                        safeNavigate(navController, drawerState, "library", isNavigatingState)
                    }
                }
                DrawerItem("Articles", Color.White, enabled = !isNavigatingState.value) {
                    scope.launch {
                        safeNavigate(navController, drawerState, "articles", isNavigatingState)
                    }
                }
                DrawerItem("Premium", Color(0xFFFFC107)) {
                    scope.launch { drawerState.close() }
                }
                DrawerItem("AI Companion ✨", Color(0xFF38BDF8), enabled = !isNavigatingState.value) {
                    scope.launch {
                        safeNavigate(navController, drawerState, "sphere_settings", isNavigatingState)
                    }
                }

                HorizontalDivider(color = Color.DarkGray)

                Text(
                    text = "Recent Searches",
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.Medium
                )

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(
                        items = historyList, 
                        key = { "hist_${it.timestamp}_${it.query.hashCode()}" }
                    ) { item ->
                        HistoryDrawerItem(item = item) {
                            scope.launch {
                                drawerState.close()
                                val encodedQuery = Uri.encode(item.query)
                                navController.navigate("answer/${encodedQuery}/${item.mode}?hybrid=true") { launchSingleTop = true }
                            }
                        }
                    }
                }
            }
        }
    ) {
        AISphereOverlay(
            navController = navController,
            viewModel = aiSphereViewModel,
            onOpenSettings = {
                navController.navigate("sphere_settings") {
                    launchSingleTop = true
                }
            }
        ) {
            // 🔹 NAVIGATION (NOW INSIDE OVERLAY)
            NavHost(
                navController = navController,
                startDestination = "splash"
            ) {
                composable("splash") {
                    SplashScreen(navController)
                }

                composable("companion_setup") {
                    CompanionSetupScreen(
                        navController = navController,
                        viewModel = aiSphereViewModel
                    )
                }

                composable("home") {
                    HomeScreen(navController, drawerState)
                }

                composable("library") {
                    LibraryScreen(navController, drawerState)
                }

                composable("libraryDetail/{bookId}") { backStackEntry ->
                    val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
                    LibraryDetailScreen(bookId, navController)
                }

                composable("articles") {
                    ArticlesScreen(navController, drawerState)
                }

                composable("answer/{question}/{mode}?bookId={bookId}&hybrid={hybrid}") { backStackEntry ->
                    val question = backStackEntry.arguments?.getString("question") ?: ""
                    val mode = backStackEntry.arguments?.getString("mode") ?: "Exam"
                    val bookId = backStackEntry.arguments?.getString("bookId")
                    val hybrid = backStackEntry.arguments?.getString("hybrid")?.toBoolean() ?: false

                    AnswerScreen(question, mode, bookId, hybrid, navController)
                }

                composable("article_detail") {
                    ArticleDetailScreen(navController)
                }

                composable("webView/{url}") { backStackEntry ->
                    val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
                    val decodedUrl = android.net.Uri.decode(encodedUrl)
                    WebViewScreen(decodedUrl, navController)
                }

                composable("sphere_settings") {
                    AISphereSettingsScreen(navController, aiSphereViewModel)
                }
            }
        }
    }
}
package com.funtime.sciai.navigation

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.funtime.sciai.ui.theme.*
import com.funtime.sciai.aisphere.*
import com.funtime.sciai.data.HistoryItem
import com.funtime.sciai.data.HistoryManager
import com.funtime.sciai.data.UserManager
import com.funtime.sciai.data.GamificationManager
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

    // ── History & User State ──
    val userManager = remember { UserManager(context) }
    val gamificationManager = remember { GamificationManager(context) }
    var historyList by remember { mutableStateOf<List<HistoryItem>>(emptyList()) }
    var userName by remember { mutableStateOf<String?>(null) }
    var userLevel by remember { mutableIntStateOf(1) }
    var userTitle by remember { mutableStateOf("Novice") }

    // ✅ Refresh history + user data when drawer opens
    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen) {
            val freshHistory = HistoryManager.getHistory(context)
            historyList = freshHistory.distinctBy { "${it.timestamp}_${it.query}" }
            userName = userManager.getName()
            userLevel = gamificationManager.getLevel()
            userTitle = gamificationManager.getTitle()
        }
    }

    // ✅ Safe navigation tracking
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: ""

    LaunchedEffect(currentBackStackEntry) {
        aiSphereViewModel.onScreenChanged(currentRoute)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isNavigatingState.value,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = SciAISurfaceAlt
            ) {
                // ── Drawer Header ──
                DrawerHeader(
                    userName = userName,
                    level = userLevel,
                    title = userTitle
                )

                HorizontalDivider(color = SciAIBorder)

                // ── Navigation Section ──
                DrawerSectionLabel("NAVIGATION")

                DrawerItem("Home", emoji = "🏠", enabled = !isNavigatingState.value, isActive = currentRoute == "home") {
                    scope.launch {
                        safeNavigate(navController, drawerState, "home", isNavigatingState)
                    }
                }
                DrawerItem("Library", emoji = "📚", enabled = !isNavigatingState.value, isActive = currentRoute == "library") {
                    scope.launch {
                        safeNavigate(navController, drawerState, "library", isNavigatingState)
                    }
                }
                DrawerItem("Articles", emoji = "📰", enabled = !isNavigatingState.value, isActive = currentRoute == "articles") {
                    scope.launch {
                        safeNavigate(navController, drawerState, "articles", isNavigatingState)
                    }
                }
                DrawerItem("Premium", emoji = "⭐", textColor = SciAIAmber) {
                    scope.launch { drawerState.close() }
                }
                DrawerItem("AI Companion", emoji = "✨", textColor = SciAICyan, enabled = !isNavigatingState.value, isActive = currentRoute == "sphere_settings") {
                    scope.launch {
                        safeNavigate(navController, drawerState, "sphere_settings", isNavigatingState)
                    }
                }

                HorizontalDivider(color = SciAIBorder, modifier = Modifier.padding(top = 8.dp))

                // ── History Section ──
                DrawerSectionLabel("RECENT SEARCHES")

                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(
                        items = historyList, 
                        key = { index, item -> "hist_${item.timestamp}_${item.query.hashCode()}_$index" }
                    ) { index, item ->
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

                composable("library?q={q}") { backStackEntry ->
                    val query = backStackEntry.arguments?.getString("q")
                    LibraryScreen(navController, drawerState, query)
                }

                composable("libraryDetail/{bookId}") { backStackEntry ->
                    val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
                    LibraryDetailScreen(bookId, navController)
                }

                composable("articles?q={q}") { backStackEntry ->
                    val query = backStackEntry.arguments?.getString("q")
                    ArticlesScreen(navController, drawerState, query)
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
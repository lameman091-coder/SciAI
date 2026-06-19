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
import com.funtime.sciai.ui.theme.playlist.PlaylistScreen
import com.funtime.sciai.ui.theme.*
import com.funtime.sciai.aisphere.*
import com.funtime.sciai.ui.components.PremiumSettingsBottomSheet
import com.funtime.sciai.data.HistoryItem
import com.funtime.sciai.data.HistoryManager
import com.funtime.sciai.data.UserManager
import com.funtime.sciai.data.GamificationManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNav(themeViewModel: ThemeViewModel) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val aiSphereViewModel: AISphereViewModel = viewModel()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showSettingsSheet by remember { mutableStateOf(false) }

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
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = SciAISurfaceAlt
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.Start
                ) {
                    item {
                        // ── Drawer Header ──
                        DrawerHeader(
                            userName = userName,
                            level = userLevel,
                            title = userTitle,
                            onClick = {
                                scope.launch { drawerState.close() }
                                showSettingsSheet = true
                            }
                        )
                    }

                    item { HorizontalDivider(color = SciAIBorder) }

                    item {
                        // ── Navigation Section ──
                        DrawerSectionLabel("NAVIGATION")
                    }

                    item {
                        DrawerItem(
                            "Home",
                            emoji = "🏠",
                            enabled = true,
                            isActive = currentRoute == "home"
                        ) {
                            safeNavigate(navController, drawerState, "home", scope)
                        }
                    }

                    item {
                        DrawerItem(
                            "Library",
                            emoji = "📚",
                            enabled = true,
                            isActive = currentRoute.startsWith("library")
                        ) {
                            safeNavigate(navController, drawerState, "library?q=", scope)
                        }
                    }

                    item {
                        DrawerItem(
                            "Articles",
                            emoji = "📰",
                            enabled = true,
                            isActive = currentRoute.startsWith("articles")
                        ) {
                            safeNavigate(navController, drawerState, "articles?q=", scope)
                        }
                    }

                    item {
                        DrawerItem("Premium", emoji = "⭐", textColor = SciAIAmber) {
                            scope.launch { drawerState.close() }
                        }
                    }

                    item {
                        DrawerItem(
                            "AI Companion",
                            emoji = "✨",
                            textColor = SciAICyan,
                            enabled = true,
                            isActive = currentRoute == "sphere_settings"
                        ) {
                            safeNavigate(navController, drawerState, "sphere_settings", scope)
                        }
                    }

                    item {
                        DrawerItem(
                            "Playlist",
                            emoji = "🎵",
                            textColor = Color(0xFFA78BFA),
                            enabled = true,
                            isActive = currentRoute == "playlist"
                        ) {
                            safeNavigate(navController, drawerState, "playlist", scope)
                        }
                    }

                    item {
                        HorizontalDivider(color = SciAIBorder, modifier = Modifier.padding(top = 8.dp))
                        DrawerSectionLabel("RECENT SEARCHES")
                    }

                    itemsIndexed(
                        items = historyList,
                        key = { index, item -> "hist_${item.timestamp}_${item.query.hashCode()}_$index" }
                    ) { index, item ->
                        HistoryDrawerItem(item = item) {
                            scope.launch {
                                drawerState.close()
                                val encodedQuery = Uri.encode(item.query)
                                navController.navigate("answer/${encodedQuery}/${item.mode}?hybrid=true") {
                                    launchSingleTop = true
                                }
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
                    HomeScreen(navController, drawerState, themeViewModel)
                }

                composable(
                    route = "library?q={q}",
                    arguments = listOf(androidx.navigation.navArgument("q") { nullable = true; defaultValue = null })
                ) { backStackEntry ->
                    val query = backStackEntry.arguments?.getString("q")
                    LibraryScreen(navController, drawerState, query)
                }

                composable("libraryDetail/{bookId}") { backStackEntry ->
                    val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
                    LibraryDetailScreen(bookId, navController)
                }

                composable(
                    route = "articles?q={q}",
                    arguments = listOf(androidx.navigation.navArgument("q") { nullable = true; defaultValue = null })
                ) { backStackEntry ->
                    val query = backStackEntry.arguments?.getString("q")
                    ArticlesScreen(navController, drawerState, query)
                }

                composable(
                    route = "answer/{question}/{mode}?bookId={bookId}&hybrid={hybrid}&level={level}",
                    arguments = listOf(
                        androidx.navigation.navArgument("bookId") { nullable = true; defaultValue = null },
                        androidx.navigation.navArgument("hybrid") { nullable = true; defaultValue = null },
                        androidx.navigation.navArgument("level") { nullable = true; defaultValue = null }
                    )
                ) { backStackEntry ->
                    val question = backStackEntry.arguments?.getString("question") ?: ""
                    val mode = backStackEntry.arguments?.getString("mode") ?: "Exam"
                    val bookId = backStackEntry.arguments?.getString("bookId")
                    val hybrid = backStackEntry.arguments?.getString("hybrid")?.toBoolean() ?: false
                    val level = backStackEntry.arguments?.getString("level") ?: "Academic"

                    AnswerScreen(question, mode, bookId, hybrid, level, navController)
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

                composable("playlist") {
                    PlaylistScreen(navController, drawerState)
                }
            }
        }

        // ── Premium Settings Bottom Sheet ──
        PremiumSettingsBottomSheet(
            userManager = userManager,
            isVisible = showSettingsSheet,
            onDismissRequest = { showSettingsSheet = false }
        )
    }
}
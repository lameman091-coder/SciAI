package com.funtime.sciai.ui.theme.home
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.funtime.sciai.data.UserManager
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import android.widget.Toast
import com.funtime.sciai.data.GeminiService
import com.funtime.sciai.data.HistoryManager
import androidx.compose.material3.ElevatedCard
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap

import androidx.compose.material3.DrawerState
import androidx.compose.runtime.MutableState




suspend fun safeNavigate(
    navController: NavController,
    drawerState: DrawerState,
    route: String,
    isNavigatingState: MutableState<Boolean>
) {
    if (isNavigatingState.value) return

    isNavigatingState.value = true

    drawerState.close()

    // 🔥 simple & reliable instead of snapshotFlow
    delay(250)

    navController.navigate(route) {
        launchSingleTop = true
        restoreState = true
        popUpTo(navController.graph.startDestinationId) {
            saveState = true
        }
    }

    delay(200)

    isNavigatingState.value = false
}
@OptIn(ExperimentalMaterial3Api::class)


@Composable
fun HomeScreen(navController: NavController, drawerState: androidx.compose.material3.DrawerState) {
    val isNavigatingState = remember { mutableStateOf(false) }
    var isProcessingImage by remember { mutableStateOf(false) }

    var selectedDomain by remember { mutableStateOf("Biology") }
    var selectedMode by remember { mutableStateOf("Exam") }

    val context = LocalContext.current
    var query by remember { mutableStateOf("") }

    val activity = context as Activity
    val coroutineScope = rememberCoroutineScope()
    val userManager = UserManager(context)
    val intelligenceManager = remember { com.funtime.sciai.data.IntelligenceManager(context) }
    val gamificationManager = remember { com.funtime.sciai.data.GamificationManager(context) }
    val sessionViewModel: SessionViewModel = viewModel()
    val sessionTime = sessionViewModel.sessionTime.value

    var totalCount by remember { mutableStateOf(0) }
    var historyList by remember { mutableStateOf<List<com.funtime.sciai.data.HistoryItem>>(emptyList()) }
    var userName by remember { mutableStateOf<String?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    // Dashboard state
    var dashboardLoaded by remember { mutableStateOf(false) }
    var streak by remember { mutableStateOf(0) }
    var streakEmoji by remember { mutableStateOf("🔥") }
    var totalXP by remember { mutableStateOf(0) }
    var todayXP by remember { mutableStateOf(0) }
    var currentLevel by remember { mutableStateOf(1) }
    var currentTitle by remember { mutableStateOf("Novice") }
    var overallAccuracy by remember { mutableStateOf(0f) }
    var weakTopics by remember { mutableStateOf<List<com.funtime.sciai.data.IntelligenceManager.WeakTopic>>(emptyList()) }
    var suggestedAction by remember { mutableStateOf<com.funtime.sciai.data.IntelligenceManager.SuggestedAction?>(null) }
    var dailyMissions by remember { mutableStateOf<List<com.funtime.sciai.data.GamificationManager.Mission>>(emptyList()) }
    var latestAchievement by remember { mutableStateOf<com.funtime.sciai.data.GamificationManager.Achievement?>(null) }

    LaunchedEffect(Unit) {
        delay(100) // let UI render first
        withContext(Dispatchers.IO) {
            val history = HistoryManager.getHistory(context)
            val total = userManager.getTotal()
            val name = userManager.getName()

            // Record daily activity & update gamification
            gamificationManager.recordDailyActivity()

            // Load dashboard data
            val s = gamificationManager.getCurrentStreak()
            val se = gamificationManager.getStreakEmoji()
            val txp = gamificationManager.getTotalXP()
            val dxp = gamificationManager.getTodayXP()
            val lvl = gamificationManager.getLevel()
            val ttl = gamificationManager.getTitle()
            val acc = intelligenceManager.getOverallAccuracy()
            val wt = intelligenceManager.getWeakTopics()
            val sa = intelligenceManager.getSuggestedAction()
            val dm = gamificationManager.getDailyMissions()
            val la = gamificationManager.getLatestAchievement()

            withContext(Dispatchers.Main) {
                historyList = history
                totalCount = total
                userName = name
                showDialog = name == null

                streak = s
                streakEmoji = se
                totalXP = txp
                todayXP = dxp
                currentLevel = lvl
                currentTitle = ttl
                overallAccuracy = acc
                weakTopics = wt
                suggestedAction = sa
                dailyMissions = dm
                latestAchievement = la
                dashboardLoaded = true
            }
        }
    }

    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            imageUris = (imageUris + uris).distinct()
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val results = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                query = results[0]
            }
        }
    }

    val executeSearch = {
        coroutineScope.launch {
            if (imageUris.isNotEmpty()) {
                isProcessingImage = true
                val results = imageUris.map { uri ->
                    async(Dispatchers.IO) {
                        GeminiService.analyzeImage(context, uri)
                    }
                }.awaitAll()

                val combinedImageInfo = results.joinToString("\n")
                isProcessingImage = false

                userManager.incrementTotal()
                totalCount = userManager.getTotal()

                val finalQuery = if (query.isNotBlank())
                    "$query\n\nImage Info:\n$combinedImageInfo"
                else combinedImageInfo

                val encodedQuery = Uri.encode(finalQuery)
                navController.navigate("answer/$encodedQuery/$selectedMode?hybrid=true")
                imageUris = emptyList()
                query = ""
            } else if (query.isNotBlank()) {
                userManager.incrementTotal()
                totalCount = userManager.getTotal()
                val encodedQuery = Uri.encode(query)
                navController.navigate("answer/$encodedQuery/$selectedMode?hybrid=true")
                query = ""
            }
        }
    }

    if (showDialog) {
        var input by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {
                Button(onClick = {
                    if (input.isNotBlank()) {
                        userManager.saveName(input)
                        userName = input
                        showDialog = false
                    }
                }) {
                    Text("Start")
                }
            },
            title = { Text("Welcome") },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Enter your name") }
                )
            }
        )
    }

    com.funtime.sciai.components.AppScaffold(
        title = "SciAI",
        navController = navController,
        onMenuClick = { coroutineScope.launch { drawerState.open() } }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ═══════════════════════════════════════════════════════
            // 🔥 WELCOME + STREAK + QUICK STATS ROW
            // ═══════════════════════════════════════════════════════
            AnimatedVisibility(
                visible = dashboardLoaded,
                enter = fadeIn() + slideInVertically { -40 }
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Welcome, ${userName ?: "Student"}",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Lv.$currentLevel", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(" · ", color = Color.Gray, fontSize = 12.sp)
                                Text(currentTitle, color = Color(0xFFFBBF24), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        // Streak badge
                        Surface(
                            color = if (streak > 0) Color(0xFFF59E0B).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (streak > 0) Color(0xFFF59E0B).copy(alpha = 0.4f) else Color.Gray.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(streakEmoji, fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("$streak", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickStatChip("⚡ ${todayXP} XP", Color(0xFF38BDF8), Modifier.weight(1f))
                        QuickStatChip("🎯 ${overallAccuracy.toInt()}%", Color(0xFF22C55E), Modifier.weight(1f))
                        QuickStatChip("⏱ ${formatSessionTime(sessionTime)}", Color(0xFFFBBF24), Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ═══════════════════════════════════════════════════════
            // 🎯 AI SUGGESTED ACTION CARD
            // ═══════════════════════════════════════════════════════
            if (suggestedAction != null) {
                AnimatedVisibility(
                    visible = dashboardLoaded,
                    enter = fadeIn() + slideInVertically { -20 }
                ) {
                    ElevatedCard(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().clickable {
                            val action = suggestedAction!!
                            when (action.actionType) {
                                com.funtime.sciai.data.IntelligenceManager.ActionType.QUIZ -> {
                                    val topic = action.targetTopic ?: query.ifBlank { "Biology" }
                                    val encoded = Uri.encode(topic)
                                    navController.navigate("answer/$encoded/Quiz?hybrid=true")
                                }
                                com.funtime.sciai.data.IntelligenceManager.ActionType.TEST -> {
                                    val topic = action.targetTopic ?: query.ifBlank { "Biology" }
                                    val encoded = Uri.encode(topic)
                                    navController.navigate("answer/$encoded/Test?hybrid=true")
                                }
                                com.funtime.sciai.data.IntelligenceManager.ActionType.EXPERT -> {
                                    val topic = action.targetTopic ?: query.ifBlank { "Biology" }
                                    val encoded = Uri.encode(topic)
                                    navController.navigate("answer/$encoded/Expert?hybrid=true")
                                }
                                com.funtime.sciai.data.IntelligenceManager.ActionType.ARTICLE -> {
                                    navController.navigate("articles")
                                }
                                com.funtime.sciai.data.IntelligenceManager.ActionType.SEARCH -> { }
                            }
                        },
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = Color(0xFF0F172A)
                        ),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "💡 SUGGESTED",
                                    color = Color(0xFFFBBF24),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.5.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    suggestedAction!!.title,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    suggestedAction!!.description,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 13.sp
                                )
                            }
                            Surface(
                                color = Color(0xFF38BDF8).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    "GO →",
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // ═══════════════════════════════════════════════════════
            // ⚠️ WEAK TOPICS ALERT
            // ═══════════════════════════════════════════════════════
            if (weakTopics.isNotEmpty()) {
                Text(
                    "⚠️ WEAK TOPICS",
                    color = Color(0xFFEF4444),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(weakTopics.size) { idx ->
                        val wt = weakTopics[idx]
                        Surface(
                            color = Color(0xFFEF4444).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f)),
                            modifier = Modifier.clickable {
                                val encoded = Uri.encode(wt.topic)
                                navController.navigate("answer/$encoded/Quiz?hybrid=true")
                            }
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(
                                    wt.topic.replaceFirstChar { it.uppercase() },
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    "${wt.accuracy.toInt()}% · ${wt.attempts} attempts",
                                    color = Color(0xFFEF4444),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ═══════════════════════════════════════════════════════
            // 📊 PROGRESS CARD (replaces old simple progress card)
            // ═══════════════════════════════════════════════════════
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = Color(0xFF0F172A)
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📊 PROGRESS", color = Color(0xFF38BDF8), fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.sp)
                        Text("${totalXP} XP Total", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // XP Progress bar to next level
                    val xpInLevel = totalXP % 100
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Level $currentLevel → ${currentLevel + 1}", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                            Text("$xpInLevel/100 XP", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = (xpInLevel / 100f).coerceIn(0f, 1f),
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF38BDF8),
                            trackColor = Color(0xFF38BDF8).copy(alpha = 0.15f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("Total Qs", "${intelligenceManager.getProgressData().totalQuestions}")
                        StatItem("Accuracy", "${overallAccuracy.toInt()}%")
                        StatItem("Sessions", "${intelligenceManager.getProgressData().studySessions}")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ═══════════════════════════════════════════════════════
            // 🎯 DAILY MISSIONS
            // ═══════════════════════════════════════════════════════
            if (dailyMissions.isNotEmpty()) {
                ElevatedCard(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF0F172A)),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🎯 DAILY MISSIONS", color = Color(0xFFA855F7), fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.sp)
                            Text(
                                "${gamificationManager.getCompletedMissionCount()}/${dailyMissions.size}",
                                color = if (gamificationManager.areAllMissionsCompleted()) Color(0xFF22C55E) else Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        dailyMissions.forEach { mission ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(mission.emoji, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(mission.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text(mission.description, color = Color(0xFF94A3B8), fontSize = 11.sp)
                                }
                                if (mission.isCompleted) {
                                    Text("✅", fontSize = 14.sp)
                                } else {
                                    Text(
                                        "${mission.currentProgress}/${mission.targetProgress}",
                                        color = Color(0xFFA855F7),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ═══════════════════════════════════════════════════════
            // 🏆 LATEST ACHIEVEMENT
            // ═══════════════════════════════════════════════════════
            if (latestAchievement != null) {
                Surface(
                    color = Color(0xFFFBBF24).copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(latestAchievement!!.emoji, fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("LATEST ACHIEVEMENT", color = Color(0xFFFBBF24), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                            Text(latestAchievement!!.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ═══════════════════════════════════════════════════════
            // DOMAIN & MODE SELECTORS (existing feature — preserved)
            // ═══════════════════════════════════════════════════════
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(listOf("Biology", "Physics", "Chemistry", "Science")) { domain ->
                    SelectableButton(
                        text = domain,
                        isSelected = selectedDomain == domain,
                        color = when (domain) {
                            "Biology" -> Color(0xFF4CAF50)
                            "Physics" -> Color(0xFF2196F3)
                            "Chemistry" -> Color(0xFFF44336)
                            "Science" -> Color(0xFF00BCD4)
                            else -> Color.Gray
                        }
                    ) {
                        selectedDomain = domain
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(listOf("Exam", "Concept", "Expert", "Quiz", "Test")) { mode ->
                    SelectableButton(
                        text = mode,
                        isSelected = selectedMode == mode,
                        color = when (mode) {
                            "Exam" -> Color(0xFFFFC107)
                            "Concept" -> Color(0xFF2196F3)
                            "Expert" -> Color(0xFF9C27B0)
                            "Quiz" -> Color(0xFF38BDF8)
                            "Test" -> Color(0xFFEF4444)
                            else -> Color.Gray
                        }
                    ) {
                        selectedMode = mode
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ═══════════════════════════════════════════════════════
            // IMAGE PREVIEWS (existing feature — preserved)
            // ═══════════════════════════════════════════════════════
            if (imageUris.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    items(imageUris) { uri ->
                        var bitmap by remember(uri) { mutableStateOf<android.graphics.Bitmap?>(null) }
                        LaunchedEffect(uri) {
                            withContext(Dispatchers.IO) {
                                bitmap = com.funtime.sciai.data.ImageUtils.decodeSampledBitmapFromUri(context, uri, 200, 200)
                            }
                        }

                        if (bitmap != null) {
                            Box(modifier = Modifier.size(80.dp)) {
                                Image(
                                    bitmap = bitmap!!.asImageBitmap(),
                                    contentDescription = "Selected Image",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = { imageUris = imageUris.filter { it != uri } },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(24.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                        .padding(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove Image",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════════════════
            // SEARCH BAR (existing feature — preserved)
            // ═══════════════════════════════════════════════════════
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(if (isProcessingImage) "Processing images..." else "Ask anything...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isProcessingImage,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = { executeSearch() }
                ),
                shape = RoundedCornerShape(24.dp),
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
                        IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Images", tint = Color.Gray)
                        }
                        IconButton(onClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
                            }
                            speechLauncher.launch(intent)
                        }) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice", tint = Color.Gray)
                        }
                        IconButton(
                            onClick = { executeSearch() },
                            enabled = (query.isNotBlank() || imageUris.isNotEmpty()) && !isProcessingImage
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Search",
                                tint = if (isProcessingImage) Color.Gray else Color(0xFF38BDF8)
                            )
                        }
                    }
                }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════
// DASHBOARD HELPER COMPOSABLES
// ═══════════════════════════════════════════════════════

@Composable
fun QuickStatChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Text(label, color = Color(0xFF64748B), fontSize = 11.sp)
    }
}

fun getBadge(count: Int): String {
    return when {
        count >= 50 -> "Expert 🧠"
        count >= 20 -> "Smart 🔥"
        count >= 10 -> "Learner 📘"
        else -> "Beginner 🌱"
    }
}

fun formatSessionTime(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "0m"
    }
}

@Composable
fun SelectableButton(
    text: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) color.copy(alpha = 0.2f) else Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isSelected) color else Color.Gray.copy(alpha = 0.5f)
        ),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = text,
                color = if (isSelected) color else Color.Gray,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                letterSpacing = 0.5.sp,
                fontSize = 14.sp
            )
        }
    }
}

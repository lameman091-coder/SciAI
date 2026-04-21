package com.funtime.sciai.ui.theme.home

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.funtime.sciai.data.*
import com.funtime.sciai.data.rag.RagService
import com.funtime.sciai.ui.theme.*
import coil.compose.AsyncImage
import kotlinx.coroutines.*
import java.util.*

// ── CONSTANTS & MODELS ───────────────────────────────────────────────────────
val CyanAccent = SciAICyan
data class PredictedContext(val domain: String?, val mode: String?)

// ── CLIENT-SIDE FALLBACK (used when backend /route is unreachable) ────────────
fun predictSearchContextFallback(query: String): PredictedContext {
    val q = query.lowercase()
    val domain = when {
        q.contains("cell") || q.contains("dna") || q.contains("biology") || q.contains("plant") || 
        q.contains("animal") || q.contains("protein") || q.contains("gene") || q.contains("evolution") -> "Biology"
        q.contains("force") || q.contains("gravity") || q.contains("physics") || q.contains("energy") || 
        q.contains("atom") || q.contains("light") || q.contains("motion") || q.contains("matter") -> "Physics"
        q.contains("chemical") || q.contains("molecule") || q.contains("chemistry") || q.contains("acid") || 
        q.contains("reaction") || q.contains("bond") || q.contains("periodic") || q.contains("element") -> "Chemistry"
        q.contains("earth") || q.contains("space") || q.contains("science") || q.contains("nature") -> "Science"
        else -> null
    }
    val mode = when {
        q.contains("explain") || q.contains("what is") || q.contains("concept") || q.contains("how") -> "Concept"
        q.contains("quiz") || q.contains("ask me") || q.contains("trivia") -> "Quiz"
        q.contains("test") || q.contains("prepare") || q.contains("exam") -> "Test"
        q.contains("deep") || q.contains("advanced") || q.contains("expert") || q.contains("theory") -> "Expert"
        else -> null
    }
    return PredictedContext(domain, mode)
}

// ── MAIN SCREEN ──────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, drawerState: DrawerState, themeViewModel: ThemeViewModel) {
    val context = LocalContext.current
    val activity = context as Activity
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current
    
    // Managers
    val userManager = remember { UserManager(context) }
    val intelligenceManager = remember { IntelligenceManager(context) }
    val gamificationManager = remember { GamificationManager(context) }
    val sessionViewModel: SessionViewModel = viewModel()
    val sessionTime = sessionViewModel.sessionTime.value

    // State Hooks
    var query by remember { mutableStateOf("") }
    var selectedDomain by remember { mutableStateOf("Biology") }
    var selectedMode by remember { mutableStateOf("Exam") }
    var isHybrid by remember { mutableStateOf(userManager.isHybridDefault()) }
    var selectedTier by remember { mutableStateOf("Academic") }
    var isSearchFocused by remember { mutableStateOf(false) }
    var isProcessingImage by remember { mutableStateOf(false) }
    var lastPredictedContext by remember { mutableStateOf<PredictedContext?>(null) }
    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // ── Companion State ──────────────────────────────────────────────────────
    var companionMessage by remember { mutableStateOf("") }
    var isAiDetected by remember { mutableStateOf(false) }
    var companionConfidence by remember { mutableStateOf(0f) }
    var routeDebounceJob by remember { mutableStateOf<Job?>(null) }

    // Dashboard Data
    var dashboardLoaded by remember { mutableStateOf(false) }
    var userName by remember { mutableStateOf<String?>(null) }
    var totalCount by remember { mutableStateOf(0) }
    var historyList by remember { mutableStateOf<List<HistoryItem>>(emptyList()) }
    var streak by remember { mutableStateOf(0) }
    var streakEmoji by remember { mutableStateOf("🔥") }
    var totalXP by remember { mutableStateOf(0) }
    var todayXP by remember { mutableStateOf(0) }
    var currentLevel by remember { mutableStateOf(1) }
    var currentTitle by remember { mutableStateOf("Novice") }
    var overallAccuracy by remember { mutableStateOf(0f) }
    var weakTopics by remember { mutableStateOf<List<IntelligenceManager.WeakTopic>>(emptyList()) }
    var suggestedAction by remember { mutableStateOf<IntelligenceManager.SuggestedAction?>(null) }
    var dailyMissions by remember { mutableStateOf<List<GamificationManager.Mission>>(emptyList()) }
    var latestAchievement by remember { mutableStateOf<GamificationManager.Achievement?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    // Launchers
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) imageUris = (imageUris + uris).distinct()
    }
    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val results = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) query = results[0]
        }
    }

    // ── Intelligence Effect (Backend Controller /route) ──────────────────────
    LaunchedEffect(query) {
        if (query.length > 5 && isSearchFocused) {
            // Cancel previous debounce
            routeDebounceJob?.cancel()
            routeDebounceJob = coroutineScope.launch {
                delay(800) // Debounce — wait 800ms after user stops typing
                
                // Call backend Controller
                RagService.routeQuery(query) { routeResponse ->
                    if (routeResponse != null && routeResponse.confidence > 0.3f) {
                        // AI detection succeeded
                        selectedDomain = routeResponse.domain
                        selectedMode = routeResponse.mode
                        companionMessage = routeResponse.companionMessage
                        companionConfidence = routeResponse.confidence
                        isAiDetected = true
                        lastPredictedContext = PredictedContext(routeResponse.domain, routeResponse.mode)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    } else {
                        // Fallback to client-side prediction
                        val prediction = predictSearchContextFallback(query)
                        if (prediction.domain != null || prediction.mode != null) {
                            prediction.domain?.let { selectedDomain = it }
                            prediction.mode?.let { selectedMode = it }
                            lastPredictedContext = prediction
                            isAiDetected = false
                            companionMessage = ""
                        }
                    }
                }
            }
        } else if (query.length <= 5) {
            companionMessage = ""
            isAiDetected = false
        }
    }

    // Auto-dismiss companion message after 4 seconds
    LaunchedEffect(companionMessage) {
        if (companionMessage.isNotBlank()) {
            delay(4000)
            companionMessage = ""
        }
    }

    // Initialization Effect
    LaunchedEffect(Unit) {
        delay(100)
        withContext(Dispatchers.IO) {
            val history = HistoryManager.getHistory(context)
            val total = userManager.getTotal()
            val name = userManager.getName()
            gamificationManager.recordDailyActivity()
            
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

    val executeSearch: () -> Unit = {
        coroutineScope.launch {
            if (imageUris.isNotEmpty()) {
                isProcessingImage = true
                val results = imageUris.map { uri ->
                    async(Dispatchers.IO) { GeminiService.analyzeImage(context, uri) }
                }.awaitAll()
                val info = results.joinToString("\n")
                isProcessingImage = false
                userManager.incrementTotal()
                val finalQuery = if (query.isNotBlank()) "$query\n\nImage Info:\n$info" else info
                navController.navigate("answer/${Uri.encode(finalQuery)}/$selectedMode?hybrid=$isHybrid&level=$selectedTier")
                imageUris = emptyList(); query = ""
            } else if (query.isNotBlank()) {
                userManager.incrementTotal()
                navController.navigate("answer/${Uri.encode(query)}/$selectedMode?hybrid=$isHybrid&level=$selectedTier")
                query = ""
            }
        }
    }

    if (showDialog) {
        var input by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = {},
            containerColor = SciAISurface,
            shape = RoundedCornerShape(24.dp),
            confirmButton = { 
                Button(
                    onClick = { if (input.isNotBlank()) { userManager.saveName(input); userName = input; showDialog = false } },
                    colors = ButtonDefaults.buttonColors(containerColor = SciAICyan)
                ) { Text("Start Learning", color = Color.Black, fontWeight = FontWeight.Bold) } 
            },
            title = { 
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("🧬", fontSize = 40.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Welcome to SciAI", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Your intelligent science companion", color = SciAISubtext, fontSize = 13.sp)
                }
            }, 
            text = { 
                OutlinedTextField(
                    value = input, 
                    onValueChange = { input = it }, 
                    label = { Text("What should I call you?", color = SciAISubtext) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SciAICyan,
                        unfocusedBorderColor = SciAIBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = SciAIText,
                        cursorColor = SciAICyan
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) 
            }
        )
    }

    com.funtime.sciai.components.AppScaffold(
        title = "SciAI",
        navController = navController,
        onMenuClick = { coroutineScope.launch { drawerState.open() } },
        actions = {
            val isDark = themeViewModel.isDarkMode.value
            IconButton(onClick = { themeViewModel.toggleTheme() }) {
                Crossfade(targetState = isDark, label = "themeIcon") { dark ->
                    Icon(
                        imageVector = if (dark) Icons.Default.WbSunny else Icons.Default.NightsStay,
                        contentDescription = "Toggle Theme",
                        tint = if (dark) Color(0xFFFBBF24) else Color(0xFF334155)
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Dashboard
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = if (isSearchFocused) 320.dp else 180.dp)
            ) {
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Welcome Section
                        AnimatedVisibility(visible = dashboardLoaded, enter = fadeIn() + slideInVertically { -40 }) {
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text("Welcome, ${userName ?: "Student"}", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Lv.$currentLevel", color = SciAICyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text(" · ", color = Color.Gray, fontSize = 12.sp)
                                            Text(currentTitle, color = SciAIAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Surface(color = if (streak > 0) Color(0xFFF59E0B).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, if (streak > 0) Color(0xFFF59E0B).copy(alpha = 0.4f) else Color.Gray.copy(alpha = 0.2f))) {
                                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text(streakEmoji, fontSize = 18.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("$streak", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    QuickStatChip("⚡ ${todayXP} XP", SciAICyan, Modifier.weight(1f))
                                    QuickStatChip("🎯 ${overallAccuracy.toInt()}%", SciAIGreen, Modifier.weight(1f))
                                    QuickStatChip("⏰ ${formatSessionTime(sessionTime)}", SciAIAmber, Modifier.weight(1f))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Suggested Action (Glassmorphic)
                        if (suggestedAction != null) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().clickable {
                                    val action = suggestedAction!!
                                    val topic = action.targetTopic ?: query.ifBlank { "Biology" }
                                    val route = when(action.actionType) {
                                        IntelligenceManager.ActionType.QUIZ -> "answer/${Uri.encode(topic)}/Quiz?hybrid=true"
                                        IntelligenceManager.ActionType.TEST -> "answer/${Uri.encode(topic)}/Test?hybrid=true"
                                        IntelligenceManager.ActionType.EXPERT -> "answer/${Uri.encode(topic)}/Expert?hybrid=true"
                                        IntelligenceManager.ActionType.ARTICLE -> "articles"
                                        else -> null
                                    }
                                    route?.let { navController.navigate(it) }
                                },
                                colors = CardDefaults.cardColors(containerColor = SciAISurface.copy(alpha = 0.65f)),
                                border = BorderStroke(1.dp, SciAIBorderLight)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("💡 SUGGESTED", color = SciAIAmber, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(suggestedAction!!.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Text(suggestedAction!!.description, color = SciAISubtext, fontSize = 13.sp)
                                    }
                                    Surface(color = SciAICyan.copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp)) {
                                        Text("GO →", color = SciAICyan, fontWeight = FontWeight.Black, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                                // Progress Card (Premium)
                                Card(
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp)),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("📊 PROGRESS", color = SciAICyan, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                    Text("${totalXP} XP Total", color = SciAISubtext, fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                val xpInLevel = totalXP % 100
                                LinearProgressIndicator(
                                    progress = (xpInLevel / 100f).coerceIn(0f, 1f),
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = SciAICyan, trackColor = SciAICyan.copy(alpha = 0.15f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    StatItem("Total Qs", "${intelligenceManager.getProgressData().totalQuestions}")
                                    StatItem("Accuracy", "${overallAccuracy.toInt()}%")
                                    StatItem("Sessions", "${intelligenceManager.getProgressData().studySessions}")
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Missions (Premium)
                        if (dailyMissions.isNotEmpty()) {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(20.dp)),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("🎯 DAILY MISSIONS", color = SciAIPurple, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    dailyMissions.forEach { mission ->
                                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text(mission.emoji, fontSize = 16.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(mission.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                                Text(mission.description, color = SciAISubtext, fontSize = 11.sp)
                                            }
                                            if (mission.isCompleted) Text("✅", fontSize = 14.sp)
                                            else Text("${mission.currentProgress}/${mission.targetProgress}", color = SciAIPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Dim Overlay
            AnimatedVisibility(visible = isSearchFocused, enter = fadeIn(), exit = fadeOut()) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable { isSearchFocused = false; focusManager.clearFocus() })
            }

            // Smart Interface
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                FloatingSmartInterface(
                    query = query,
                    onQueryChange = { query = it },
                    isSearchFocused = isSearchFocused,
                    onFocusChange = { isSearchFocused = it },
                    selectedDomain = selectedDomain,
                    onDomainSelect = { selectedDomain = it; isAiDetected = false },
                    selectedMode = selectedMode,
                    onModeSelect = { selectedMode = it; isAiDetected = false },
                    lastPredictedContext = lastPredictedContext,
                    isProcessingImage = isProcessingImage,
                    imageUris = imageUris,
                    onAddImageClick = { galleryLauncher.launch("image/*") },
                    onRemoveImage = { uri -> imageUris = imageUris.filter { it != uri } },
                    onVoiceClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
                        }
                        speechLauncher.launch(intent)
                    },
                    onSendClick = executeSearch,
                    focusManager = focusManager,
                    isHybrid = isHybrid,
                    onHybridToggle = { isHybrid = it },
                    selectedTier = selectedTier,
                    onTierSelect = { selectedTier = it },
                    companionMessage = companionMessage,
                    isAiDetected = isAiDetected
                )
            }
        }
    }
}

// ── FLOATING SMART INTERFACE COMPONENT ───────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FloatingSmartInterface(
    query: String,
    onQueryChange: (String) -> Unit,
    isSearchFocused: Boolean,
    onFocusChange: (Boolean) -> Unit,
    selectedDomain: String,
    onDomainSelect: (String) -> Unit,
    selectedMode: String,
    onModeSelect: (String) -> Unit,
    lastPredictedContext: PredictedContext?,
    isProcessingImage: Boolean,
    imageUris: List<Uri>,
    onAddImageClick: () -> Unit,
    onRemoveImage: (Uri) -> Unit,
    onVoiceClick: () -> Unit,
    onSendClick: () -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager,
    isHybrid: Boolean,
    onHybridToggle: (Boolean) -> Unit,
    selectedTier: String,
    onTierSelect: (String) -> Unit,
    companionMessage: String = "",
    isAiDetected: Boolean = false
) {
    // Glow animation for AI detection
    val glowAlpha by animateFloatAsState(
        targetValue = if (isAiDetected && companionMessage.isNotBlank()) 0.6f else 0f,
        animationSpec = tween(600),
        label = "glowAlpha"
    )

    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)
            .zIndex(10f)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // ── Companion Message Bar ────────────────────────────────────────
            AnimatedVisibility(
                visible = companionMessage.isNotBlank(),
                enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 },
                exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { it / 2 }
            ) {
                Surface(
                    color = SciAICyan.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, SciAICyan.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🧠", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            companionMessage,
                            color = SciAICyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        if (isAiDetected) {
                            Surface(
                                color = SciAICyan.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    "AI",
                                    color = SciAICyan,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Morphing Context Bar
            Surface(
                color = SciAISurfaceAlt.copy(alpha = 0.95f),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, SciAIBorderLight),
                modifier = Modifier.fillMaxWidth().animateContentSize().padding(bottom = 12.dp)
                    .shadow(12.dp, RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    if (isSearchFocused) {
                        // Domain header with AI detection badge
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .padding(start = 12.dp, bottom = 8.dp, end = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "SELECT DOMAIN",
                                color = SciAICyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                            if (isAiDetected) {
                                Surface(
                                    color = SciAIGreen.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(0.5.dp, SciAIGreen.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        "🧠 AI Detected",
                                        color = SciAIGreen,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(
                                            horizontal = 8.dp,
                                            vertical = 3.dp
                                        )
                                    )
                                }
                            }
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            listOf("Biology", "Physics", "Chemistry", "Science").forEach { domain ->
                                val i = selectedDomain == domain
                                Surface(
                                    onClick = { onDomainSelect(domain) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (i) SciAICyan.copy(alpha = 0.15f) else Color.Transparent,
                                    border = BorderStroke(
                                        1.dp,
                                        if (i) SciAICyan else SciAIBorderLight
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 8.dp
                                        ), verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            when (domain) {
                                                "Biology" -> "🦠"; "Physics" -> "⚛"; "Chemistry" -> "🧪"; else -> "🌍"
                                            }, fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp)); Text(
                                        domain,
                                        color = if (i) Color.White else Color.Gray,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "SELECT MODE",
                            color = SciAIAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            listOf("Exam", "Concept", "Expert", "Quiz", "Test").forEach { mode ->
                                val i = selectedMode == mode
                                Surface(
                                    onClick = { onModeSelect(mode) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (i) SciAIAmber.copy(alpha = 0.15f) else Color.Transparent,
                                    border = BorderStroke(
                                        1.dp,
                                        if (i) SciAIAmber else SciAIBorderLight
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 8.dp
                                        ), verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            when (mode) {
                                                "Exam" -> "📘"; "Concept" -> "🧠"; "Expert" -> "🔬"; "Quiz" -> "🎯"; else -> "📝"
                                            }, fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp)); Text(
                                        mode,
                                        color = if (i) Color.White else Color.Gray,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    }
                                }
                            }
                        }

                        // Hybrid Search Toggle Bar
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Hub,
                                    contentDescription = null,
                                    tint = SciAICyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "HYBRID RESEARCH",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Switch(
                                checked = isHybrid,
                                onCheckedChange = onHybridToggle,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = SciAICyan,
                                    checkedTrackColor = SciAICyan.copy(alpha = 0.3f),
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                                ),
                                modifier = Modifier.scale(0.7f)
                            )
                        }

                        // Expert Tier Selector (Only if mode is Expert)
                        if (selectedMode == "Expert") {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "SELECT TIER",
                                color = Color(0xFF9C27B0),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                listOf("Beginner", "Academic", "Research").forEach { tier ->
                                    val i = selectedTier == tier
                                    Surface(
                                        onClick = { onTierSelect(tier) },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (i) Color(0xFF9C27B0).copy(alpha = 0.15f) else Color.Transparent,
                                        border = BorderStroke(
                                            1.dp,
                                            if (i) Color(0xFF9C27B0) else SciAIBorderLight
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(
                                                horizontal = 12.dp,
                                                vertical = 8.dp
                                            ), verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                when (tier) {
                                                    "Beginner" -> "🌱"; "Academic" -> "🎓"; else -> "🔬"
                                                }, fontSize = 14.sp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp)); Text(
                                            tier,
                                            color = if (i) Color.White else Color.Gray,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        }
                                    }
                                }
                            }
                        }

                        lastPredictedContext?.let {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    color = SciAICyan.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, SciAICyan.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        "${if (isAiDetected) "🧠 " else ""}${it.domain ?: selectedDomain} • ${it.mode ?: selectedMode}",
                                        color = SciAICyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 6.dp
                                        )
                                    )
                                }
                            }
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(6.dp)
                        ) {
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        when (selectedDomain) {
                                            "Biology" -> "🦠"; "Physics" -> "⚛"; "Chemistry" -> "🧪"; else -> "🌍"
                                        }, fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        selectedDomain,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        when (selectedMode) {
                                            "Exam" -> "📘"; "Concept" -> "🧠"; "Expert" -> "🔬"; "Quiz" -> "🎯"; else -> "📝"
                                        }, fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        selectedMode,
                                        color = SciAIAmber,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isAiDetected) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            color = SciAIGreen.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                "AI",
                                                color = SciAIGreen,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(
                                                    horizontal = 5.dp,
                                                    vertical = 2.dp
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Search Pill
            Surface(
                color = SciAIDark.copy(alpha = 0.95f),
                shape = RoundedCornerShape(32.dp),
                border = BorderStroke(
                    1.5.dp,
                    if (isAiDetected && glowAlpha > 0) SciAICyan.copy(alpha = glowAlpha)
                    else if (isSearchFocused) SciAICyan
                    else SciAIBorderLight
                ),
                modifier = Modifier.fillMaxWidth().shadow(20.dp, RoundedCornerShape(32.dp))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Image Previews (ChatGPT Style)
                    AnimatedVisibility(visible = imageUris.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth()
                                .padding(top = 12.dp, start = 12.dp, end = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(imageUris) { uri ->
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .border(1.dp, SciAIBorderLight, RoundedCornerShape(12.dp))
                                ) {
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    // Remove Button
                                    Surface(
                                        onClick = { onRemoveImage(uri) },
                                        color = Color.Black.copy(alpha = 0.6f),
                                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                                            .size(18.dp),
                                        shape = CircleShape
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.padding(2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = query, onValueChange = onQueryChange,
                        placeholder = {
                            Text(
                                "Ask SciAI anything...",
                                color = Color.Gray,
                                fontSize = 15.sp
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                            .onFocusChanged { onFocusChange(it.isFocused) },
                        singleLine = false, maxLines = 4, enabled = !isProcessingImage,
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            onSendClick(); onFocusChange(
                            false
                        ); focusManager.clearFocus()
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = SciAICyan,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        leadingIcon = {
                            IconButton(onClick = onAddImageClick) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color.Gray
                                )
                            }
                        },
                        trailingIcon = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                if (query.isNotBlank() || imageUris.isNotEmpty()) {
                                    IconButton(onClick = { 
                                        onSendClick()
                                        onFocusChange(false)
                                        focusManager.clearFocus() 
                                    }) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Send,
                                            contentDescription = null,
                                            tint = SciAICyan
                                        )
                                    }
                                } else {
                                    IconButton(onClick = onVoiceClick) {
                                        Icon(
                                            Icons.Default.Mic,
                                            contentDescription = null,
                                            tint = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

// ── HELPERS ──────────────────────────────────────────────────────────────────
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
            modifier = Modifier.padding(8.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Text(label, color = SciAIMuted, fontSize = 11.sp)
    }
}

fun formatSessionTime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return if (h > 0) "${h}h ${m}m" else if (m > 0) "${m}m" else "0m"
}

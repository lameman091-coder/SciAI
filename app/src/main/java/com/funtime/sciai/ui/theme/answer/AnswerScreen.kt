package com.funtime.sciai.ui.theme.answer

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import com.funtime.sciai.components.AppScaffold
import com.funtime.sciai.data.groq.GroqService
import com.funtime.sciai.data.rag.RagService
import androidx.compose.runtime.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.Info

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.SolidColor
data class QuizAnswerState(
    val selectedIdx: Int? = null,
    val revealed: Boolean = false,
    val isCorrect: Boolean = false
)

fun String.safeSubstring(startIndex: Int, endIndex: Int): String {
    if (this.isEmpty()) return ""

    val start = startIndex.coerceAtLeast(0).coerceAtMost(this.length)
    val end = endIndex.coerceAtLeast(start).coerceAtMost(this.length)

    return substring(start, end)
}


fun cleanMath(text: String): String {
    if (text.isEmpty()) return ""
    return try {
        text
            // Remove LaTeX blocks
            .replace(Regex("\\$\\$.*?\\$\\$", RegexOption.DOT_MATCHES_ALL), "")
            // Remove \frac patterns (basic cleanup)
            .replace("\\\\frac".toRegex(), "")
            .replace("\\\\".toRegex(), "")
    } catch (e: Exception) { text }
}
fun cleanResponse(text: String): String {
    if (text.isEmpty()) return ""
    return try {
        text
            .replace("**", "")
            .replace("##", "")
            .replace("*", "")
            .replace("•", "-")
            .replace(Regex("\\n{2,}"), "\n")
            .trim()
    } catch (e: Exception) { text }
}

fun parseDynamicSections(text: String): List<Pair<String, String>> {

    val lines = text.split("\n")
    val sections = mutableListOf<Pair<String, String>>()

    var currentTitle = "Overview"
    var currentContent = StringBuilder()

    for (line in lines) {

        val trimmed = line.trim()

        val isHeading = trimmed.firstOrNull()?.isUpperCase() == true &&
                trimmed.split(" ").size <= 6 &&
                !trimmed.endsWith(".") &&
                !trimmed.contains(":") &&
                trimmed.isNotEmpty()

        if (isHeading) {

            if (currentContent.toString().trim().isNotEmpty()) {
                sections.add(currentTitle to currentContent.toString().trim())
            }

            currentTitle = trimmed
            currentContent = StringBuilder()
        } else {
            currentContent.append(line).append("\n")
        }
    }

    if (currentContent.toString().trim().isNotEmpty() || currentTitle != "Overview") {
        sections.add(currentTitle to currentContent.toString().trim())
    }

    // Filter out actually empty sections
    return sections.filter { it.second.trim().isNotEmpty() }
}
@Composable
fun ExpandableSection(
    title: String,
    content: String,
    mode: String,
    isTyping: Boolean = false
) {

    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(isTyping) {
        if (isTyping) {
            expanded = true
        }
    }

    val borderColor = when (mode) {
        "Exam" -> Color(0xFFFFC107)     // Yellow
        "Concept" -> Color(0xFF2196F3)  // Blue
        "Expert" -> Color(0xFF9C27B0)   // Purple
        else -> Color.Gray
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, borderColor, shape = RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {

            Text(
                text = "▼ $title",
                color = borderColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
            )

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))

                val paragraphs = content.split('\n')
                paragraphs.forEach { paragraph ->
                    if (paragraph.isNotBlank()) {
                        Text(
                            text = paragraph.trim(),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun GameProgressHUD(xp: Int, level: Int, title: String) {
    Surface(
        color = Color(0xFF0F172A),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "SCIENTIFIC RANK", 
                        color = Color(0xFF94A3B8), 
                        fontSize = 10.sp, 
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        title.uppercase(), 
                        color = Color(0xFF38BDF8), 
                        fontSize = 18.sp, 
                        fontWeight = FontWeight.Black
                    )
                }
                
                Surface(
                    color = Color(0xFF38BDF8).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "LVL $level", 
                        color = Color.White, 
                        fontSize = 14.sp, 
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val progress = (xp % 100) / 100f
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF38BDF8),
                trackColor = Color(0xFF1E293B)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${xp % 100} / 100 XP", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("NEXT: ${100 - (xp % 100)} XP", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@OptIn(
    ExperimentalLayoutApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterialApi::class
)
@Composable
fun AnswerScreen(
    question: String,
    mode: String,
    bookId: String? = null,
    hybrid: Boolean = false,
    navController: NavController
) {
    val context = LocalContext.current
    var answer by remember { mutableStateOf("Loading...") }
    var responseType by remember { mutableStateOf("LLM_ONLY") }
    var responseSources by remember { mutableStateOf<List<String>>(emptyList()) }
    var displayedText by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var expertLevel by remember { mutableStateOf("Academic") }
    var selectedDomain by remember { mutableStateOf("Biology") }
    var hybridMode by remember { mutableStateOf(hybrid) } // Initialize from nav param (true when coming from Library)
    var refreshTrigger by remember { mutableStateOf(0) }
    var quizQuestions by remember { mutableStateOf<List<com.funtime.sciai.data.network.Question>>(emptyList()) }
    var quizError by remember { mutableStateOf<String?>(null) }
    var activeTheoryQuestion by remember { mutableStateOf<com.funtime.sciai.data.network.Question?>(null) }
    var answeredQuizIds by remember { mutableStateOf(setOf<String>()) }
    var manualLevel by remember { mutableIntStateOf(1) } // Default to 1, user can scroll to 10
    var testDetailQuestion by remember { mutableStateOf<com.funtime.sciai.data.network.Question?>(null) }
    
    // definitive state for quiz persistence
    var quizStates by remember { mutableStateOf(mapOf<String, QuizAnswerState>()) }
    
    
    val userManager = remember { com.funtime.sciai.data.UserManager(context) }
    val userId = remember { userManager.getUserId() }
    
    var xp by remember { mutableIntStateOf(userManager.getXP()) }
    var level by remember { mutableIntStateOf(userManager.getLevel()) }
    var currentTitle by remember { mutableStateOf(userManager.getTitle()) }
    var questionCountMultiplier by remember { mutableIntStateOf(1) }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isLoading,
        onRefresh = { refreshTrigger++ }
    )

    LaunchedEffect(question, mode, expertLevel, selectedDomain, hybridMode, refreshTrigger, manualLevel) {
        if (quizQuestions.isEmpty() || refreshTrigger > 0 || manualLevel > 0) {
            quizStates = emptyMap() // Reset states on new generation
            answeredQuizIds = emptySet()
        }
        
        isLoading = true
        isTyping = false
        displayedText = ""
        answer = "Loading..."
        responseType = if (hybridMode) "HYBRID" else "LLM_ONLY"
        responseSources = emptyList()

        if (mode == "Quiz" || mode == "Test") {
            // CALL NEW GENERATE QUESTIONS Endpoint
            RagService.generateQuestions(
                mode = mode,
                topic = question,
                domain = selectedDomain,
                level = when (expertLevel) {
                    "Beginner" -> (10 + (manualLevel * 5 - 5)) * questionCountMultiplier
                    "Academic" -> (50 + (manualLevel * 5 - 5)) * questionCountMultiplier
                    "Research" -> (100 + (manualLevel * 5 - 5)) * questionCountMultiplier
                    else -> manualLevel * 10
                },
                count = 4 * questionCountMultiplier
            ) { quizRes ->
                if (quizRes == null || quizRes.error != null) {
                    answer = "Failed to load $mode questions: ${quizRes?.error ?: "Network error"}"
                    quizError = answer
                } else {
                    quizQuestions = quizRes.questions
                    answer = "Loaded ${quizQuestions.size} questions."
                }
                isLoading = false
            }
        } else if (hybridMode) {
            // CALL PYTHON BACKEND (RAG)
            RagService.ask(
                question = question,
                mode = mode,
                domain = selectedDomain,
                bookId = bookId,
                hybrid = true,
                userId = userId
            ) { askRes ->
                if (askRes == null) {
                    answer = "Error: Failed to connect to research engine."
                    isLoading = false
                    return@ask
                }

                val rawAnswer = askRes.answer
                responseType = askRes.type
                responseSources = askRes.sources

                val stripped =
                    rawAnswer.replace(
                        Regex("<think>.*?</think>", RegexOption.DOT_MATCHES_ALL),
                        ""
                    )
                        .trim()

                val fullyCleaned = cleanMath(
                    cleanResponse(stripped)
                        .replace("<br>", "\n")
                        .replace("|", "")
                )

                answer =
                    if (fullyCleaned.isEmpty()) "No relevant data found in current context." else fullyCleaned

                isLoading = false
                isTyping = true
            }
        } else {
            // CALL DIRECT GROQ (PURE LLM)
            GroqService.ask(
                question = question,
                mode = mode,
                level = expertLevel,
                domain = selectedDomain
            ) { directRes ->
                val stripped =
                    directRes.replace(
                        Regex("<think>.*?</think>", RegexOption.DOT_MATCHES_ALL),
                        ""
                    )
                        .trim()

                val fullyCleaned = cleanMath(
                    cleanResponse(stripped)
                        .replace("<br>", "\n")
                        .replace("|", "")
                )

                answer = if (fullyCleaned.isEmpty() || fullyCleaned.startsWith("Error")) {
                    fullyCleaned.ifBlank { "AI response is currently empty. Please retry." }
                } else {
                    fullyCleaned
                }

                // Save history item locally
                if (answer.isNotBlank() && !answer.startsWith("Error")) {
                    com.funtime.sciai.data.HistoryManager.saveHistory(
                        context = context,
                        item = com.funtime.sciai.data.HistoryItem(
                            query = question,
                            mode = mode,
                            domain = selectedDomain,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }

                responseType = "LLM_ONLY"
                responseSources = emptyList()
                isLoading = false
                isTyping = true
            }
        }
    }

    LaunchedEffect(answer, isTyping) {
        if (!isLoading && isTyping && answer != "Loading...") {
            var currentText = ""
            val chunkLength = 15
            val len = answer.length
            for (i in 0 until len step chunkLength) {
                val end = (i + chunkLength).coerceAtMost(len)
                currentText = answer.safeSubstring(0, end)
                displayedText = currentText
                delay(40)
            }
            isTyping = false
        }
    }


    val listState = rememberLazyListState()

    LaunchedEffect(displayedText) {
        if (isTyping) {
            val lastIndex = listState.layoutInfo.totalItemsCount - 1
            if (lastIndex > 0) {
                listState.animateScrollToItem(lastIndex)
            }
        }
    }

    AppScaffold(
        title = mode,
        navController = navController,
        showBack = true
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(16.dp)
            ) {
                item {
                    // Hybrid Toggle for enhanced search
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .clickable { hybridMode = !hybridMode },
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Hybrid Research Mode",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (hybridMode) "Synthesizing Local + Live data" else "Pure LLM response",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = hybridMode,
                                onCheckedChange = { hybridMode = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF38BDF8),
                                    checkedTrackColor = Color(0xFF0F172A),
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color(0xFF0F172A)
                                )
                            )
                        }
                    }
                }

                if (mode == "Expert") {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf("Beginner", "Academic", "Research").forEach { level ->
                                val isSelected = expertLevel == level
                                Text(
                                    text = level,
                                    color = if (isSelected) Color.White else Color.Gray,
                                    modifier = Modifier
                                        .border(
                                            1.dp,
                                            if (isSelected) Color(0xFF9C27B0) else Color.Gray,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            expertLevel = level
                                        }
                                        .padding(8.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
                
                // --- PREMIUM ORBIT LEVEL SELECTOR (1-10) ---
                if (mode == "Quiz" || mode == "Test") {
                    item {
                        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "CHALLENGE DIFFICULTY", 
                                    color = Color(0xFF38BDF8), 
                                    fontSize = 11.sp, 
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.5.sp
                                )
                                Text(
                                    "LVL $manualLevel",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                items(10) { i ->
                                    val lvlNum = i + 1
                                    val isSelected = manualLevel == lvlNum
                                    
                                    val glowColor = when(lvlNum) {
                                        in 1..3 -> Color(0xFF22C55E) // Green
                                        in 4..6 -> Color(0xFFEAB308) // Yellow
                                        in 7..9 -> Color(0xFFF97316) // Orange
                                        else -> Color(0xFFEF4444) // Red/Legendary
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(width = 48.dp, height = 54.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) glowColor.copy(alpha = 0.15f) 
                                                else Color(0xFF0F172A)
                                            )
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                brush = if (isSelected) Brush.verticalGradient(listOf(glowColor, glowColor.copy(alpha = 0.3f))) 
                                                        else SolidColor(Color(0xFF334155)),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { 
                                                manualLevel = lvlNum
                                                refreshTrigger++ 
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                "$lvlNum", 
                                                color = if (isSelected) Color.White else Color.Gray,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Black,
                                                style = TextStyle(
                                                    shadow = if (isSelected) Shadow(color = glowColor, blurRadius = 10f) else null
                                                )
                                            )
                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(glowColor)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(60.dp),
                                color = Color(0xFF00B0FF), // Glowing blue
                                strokeWidth = 4.dp
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Generating Response...",
                                color = Color(0xFF00B0FF),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                    } // end item
                } else if (mode == "Quiz" || mode == "Test") {
                    item {
                        GameProgressHUD(xp = xp, level = level, title = currentTitle)
                    }

                    if (quizError != null) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                Text(quizError ?: "Error loading quiz", color = Color.Red, fontSize = 16.sp)
                            }
                        }
                    } else if (quizQuestions.isNotEmpty()) {
                        item {
                            Text(
                                "Premium $mode Generation", 
                                color = Color.Cyan, 
                                fontSize = 18.sp, 
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                        
                        itemsIndexed(quizQuestions, key = { _, q -> q.id }) { idx, q ->
                            if (mode == "Quiz") {
                                val state = quizStates[q.id] ?: QuizAnswerState()
                                QuizCard(
                                    q = q,
                                    state = state,
                                    onAnswer = { idx, correct ->
                                        // Update definitive state
                                        quizStates = quizStates + (q.id to QuizAnswerState(idx, true, correct))
                                        if (correct) {
                                            userManager.addXP(15)
                                            xp = userManager.getXP()
                                            level = userManager.getLevel()
                                            currentTitle = userManager.getTitle()
                                        }
                                        answeredQuizIds = answeredQuizIds + q.id
                                    }
                                )
                            } else {
                                TestCard(
                                    index = idx,
                                    q = q,
                                    onStart = { testDetailQuestion = it }
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        item {
                            val allAnswered = answeredQuizIds.size >= quizQuestions.size
                            
                            Surface(
                                color = if (allAnswered) Color(0xFF38BDF8).copy(alpha = 0.1f) else Color.Transparent,
                                shape = RoundedCornerShape(12.dp),
                                border = if (allAnswered) BorderStroke(1.dp, Color(0xFF38BDF8)) else null,
                                modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 48.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (!allAnswered) {
                                        Text(
                                            "COMPLETE ALL QUESTIONS TO VENTURE FURTHER",
                                            color = Color.Gray,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Text(
                                            "YOU'VE CONQUERED THIS SET!",
                                            color = Color.Cyan,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = {
                                                questionCountMultiplier *= 2
                                                refreshTrigger++
                                                answeredQuizIds = emptySet()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                                        ) {
                                            Text("CONTINUE (GENERATE ${quizQuestions.size * 2})", color = Color.Black, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val cursorText = if (isTyping) " █" else ""
                    val fullCleanedText = displayedText
                    val sections = try {
                        parseDynamicSections(fullCleanedText)
                    } catch (e: Exception) {
                        emptyList<Pair<String, String>>()
                    }

                    item {
                    // Confidence Badge (Bonus)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        val badgeColor = when (responseType) {
                            "HYBRID" -> Color(0xFF22C55E) // Green (High Confidence)
                            "LLM_ONLY" -> Color(0xFF94A3B8) // Gray (Generative)
                            "LLM_FALLBACK" -> Color(0xFFFACC15) // Yellow (Moderate)
                            else -> Color.Gray
                        }
                        val badgeText = when (responseType) {
                            "HYBRID" -> "SciAI Research (RAG)"
                            "LLM_ONLY" -> "LLM Generative Answer"
                            "LLM_FALLBACK" -> "Moderate Confidence (LLM Only)"
                            else -> "Processing"
                        }

                        Text(
                            text = badgeText,
                            color = badgeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .border(
                                    1.dp,
                                    badgeColor.copy(alpha = 0.5f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    }

                    if (sections.isNotEmpty()) {
                        itemsIndexed(sections) { index, pair ->
                            val (title, content) = pair
                            val isLast = index == sections.lastIndex

                            ExpandableSection(
                                title = title,
                                content = content + if (isLast && !displayedText.contains("Sources Used")) cursorText else "",
                                mode = mode,
                                isTyping = isLast && isTyping
                            )
                        }
                    } else {
                        val textToRender = fullCleanedText + cursorText
                        val paragraphs = textToRender.split('\n')
                        items(paragraphs) { paragraph ->
                            if (paragraph.isNotBlank()) {
                                Text(
                                    text = paragraph.trim(),
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    lineHeight = 24.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }

                    if (responseSources.isNotEmpty() && !isTyping) {
                        item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Sources Verified:",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            responseSources.forEach { source ->
                                Text(
                                    text = "• $source",
                                    color = Color(0xFF38BDF8).copy(alpha = 0.8f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .background(Color(0xFF1E293B), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        } // end item
                    }
                }
            } // Close LazyColumn

            SnackbarHost(
                hostState = remember { SnackbarHostState() },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
            )

            if (activeTheoryQuestion != null) {
                TheoryWritingDialog(
                    question = activeTheoryQuestion!!,
                    onDismiss = { activeTheoryQuestion = null },
                    onSubmit = { _ -> 
                        answeredQuizIds = answeredQuizIds + activeTheoryQuestion!!.id
                        activeTheoryQuestion = null
                        userManager.addXP(25) // +25 XP for theory submission
                        xp = userManager.getXP()
                        level = userManager.getLevel()
                        currentTitle = userManager.getTitle()
                    }
                )
            }

            if (testDetailQuestion != null) {
                TestQuestionDetailDialog(
                    question = testDetailQuestion!!,
                    onDismiss = { testDetailQuestion = null },
                    onStart = {
                        activeTheoryQuestion = testDetailQuestion
                        testDetailQuestion = null
                    }
                )
            }
        }
    }
}

@Composable
fun TestCard(
    index: Int,
    q: com.funtime.sciai.data.network.Question,
    onStart: (com.funtime.sciai.data.network.Question) -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color(0xFF38BDF8).copy(alpha = 0.15f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(44.dp),
                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.3f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "${index + 1}", 
                        color = Color(0xFF38BDF8), 
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(18.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    q.question, 
                    color = Color.White, 
                    fontSize = 15.sp, 
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (q.difficultyTag.lowercase() == "hard") Color.Red else Color.Cyan)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        q.difficultyTag.uppercase(), 
                        color = Color.Gray, 
                        fontSize = 10.sp, 
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Button(
                onClick = { onStart(q) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text("START", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun TheoryWritingDialog(
    question: com.funtime.sciai.data.network.Question,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var userAnswer by remember { mutableStateOf("") }
    val maxSeconds = when(question.difficultyTag.lowercase()) {
        "easy" -> 5 * 60
        "medium" -> 10 * 60
        else -> 15 * 60
    }
    var timeLeft by remember { mutableIntStateOf(maxSeconds) }
    var isEvaluating by remember { mutableStateOf(false) }
    var evaluationResult by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(timeLeft) {
        if (timeLeft > 0 && evaluationResult == null) {
            delay(1000L)
            timeLeft--
        } else if (timeLeft == 0 && evaluationResult == null) {
            if (userAnswer.trim().isNotEmpty()) {
                onSubmit(userAnswer)
            } else {
                evaluationResult = "TIME EXPIRED: Don't give up! Every effort brings you closer to being a Legend."
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.95f),
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Theory Analysis", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text("Deep Focus Mode", color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
                Surface(
                    color = if (timeLeft < 60) Color(0xFFEF4444).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (timeLeft < 60) Color(0xFFEF4444) else Color(0xFF38BDF8).copy(alpha = 0.3f))
                ) {
                    Text(
                        "${timeLeft / 60}:${(timeLeft % 60).toString().padStart(2, '0')}",
                        color = if (timeLeft < 60) Color(0xFFEF4444) else Color.Cyan,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(question.question, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(16.dp))
                
                if (evaluationResult == null) {
                    OutlinedTextField(
                        value = userAnswer,
                        onValueChange = { userAnswer = it },
                        modifier = Modifier.fillMaxWidth().height(250.dp),
                        placeholder = { Text("Write your detailed explanation here...", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("AI FEEDBACK", color = Color(0xFF38BDF8), fontWeight = FontWeight.Black, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(evaluationResult!!, color = Color.White, fontSize = 14.sp, lineHeight = 22.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (evaluationResult == null) {
                Button(
                    onClick = {
                        isEvaluating = true
                        RagService.evaluateAnswer(question.question, userAnswer, question.answer) { feedback ->
                            isEvaluating = false
                            evaluationResult = feedback ?: "Evaluation failed. Try again."
                        }
                    },
                    enabled = userAnswer.trim().length > 10 && !isEvaluating,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                ) {
                    if (isEvaluating) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                    else Text("Submit Answer", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(onClick = onDismiss) { Text("Close") }
            }
        }
    )
}

@Composable
fun TestQuestionDetailDialog(
    question: com.funtime.sciai.data.network.Question,
    onDismiss: () -> Unit,
    onStart: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                "Theory Question Analysis", 
                color = Color(0xFF38BDF8), 
                fontSize = 18.sp, 
                fontWeight = FontWeight.Black
            )
        },
        text = {
            Column {
                Surface(
                    color = Color.Black.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Text(
                        question.difficultyTag.uppercase(),
                        color = Color.Yellow,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                
                Text(
                    question.question,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 24.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "You will have ${when(question.difficultyTag.lowercase()){"easy"->5;"medium"->10;else->15}} minutes to write a detailed scientific explanation. Accuracy and depth will be evaluated by the SciAI engine.",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onStart,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
            ) {
                Text("START SESSION", color = Color.Black, fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Back", color = Color.Gray)
            }
        }
    )
}

@Composable
fun QuizCard(
    q: com.funtime.sciai.data.network.Question,
    state: QuizAnswerState,
    onAnswer: (Int, Boolean) -> Unit
) {
    var timerSeconds by remember { mutableStateOf(60) }
    var timerActive by remember { mutableStateOf(true) }

    LaunchedEffect(timerActive) {
        if (state.revealed) {
            timerActive = false
            return@LaunchedEffect
        }
        while (timerSeconds > 0 && timerActive && !state.revealed) {
            delay(1000L)
            timerSeconds--
        }
    }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.7f)),
        elevation = CardDefaults.outlinedCardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.02f))
            )
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header: Difficulty, Type, and TIMER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Text(
                            q.difficultyTag.uppercase(),
                            color = when (q.difficultyTag.lowercase()) {
                                "easy" -> Color(0xFF22C55E)
                                "medium" -> Color(0xFFEAB308)
                                "hard" -> Color(0xFFF97316)
                                "legendary" -> Color(0xFFA855F7)
                                "goat" -> Color(0xFFEF4444)
                                else -> Color.Gray
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        q.type.uppercase(),
                        color = Color.Cyan.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                // Timer Visual
                if (!state.revealed) {
                    Surface(
                        color = if (timerSeconds < 10) Color(0xFFEF4444).copy(alpha = 0.1f) else Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, if (timerSeconds < 10) Color(0xFFEF4444) else Color.Gray.copy(alpha = 0.3f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "0:${timerSeconds.toString().padStart(2, '0')}",
                                color = if (timerSeconds < 10) Color(0xFFEF4444) else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                q.question,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 24.sp,
                style = TextStyle(
                    shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 4f)
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Options (Interactive)
            if (q.options != null && q.options.isNotEmpty()) {
                q.options.forEachIndexed { idx, opt ->
                    val letter = ('A' + idx).toString()
                    val isSelected = state.selectedIdx == idx
                    val isCorrect = opt.trim().lowercase() == q.answer.trim().lowercase() || 
                                    (idx < 4 && q.answer.trim().startsWith(letter, ignoreCase = true)) ||
                                    (q.answer.contains(opt, ignoreCase = true) && opt.length > 5)

                    val borderColor = when {
                        state.revealed && isCorrect -> Color(0xFF22C55E)
                        isSelected && !isCorrect && state.revealed -> Color(0xFFEF4444)
                        isSelected -> Color(0xFF38BDF8)
                        else -> Color.Gray.copy(alpha = 0.2f)
                    }

                    val bgColor = when {
                        state.revealed && isCorrect -> Color(0xFF22C55E).copy(alpha = 0.15f)
                        isSelected && !isCorrect && state.revealed -> Color(0xFFEF4444).copy(alpha = 0.15f)
                        isSelected -> Color(0xFF38BDF8).copy(alpha = 0.1f)
                        else -> Color.White.copy(alpha = 0.02f)
                    }

                    Surface(
                        color = bgColor,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable(enabled = !state.revealed) {
                                onAnswer(idx, isCorrect)
                            },
                        border = BorderStroke(if (isSelected || (state.revealed && isCorrect)) 1.5.dp else 1.dp, borderColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        color = if (isSelected) borderColor else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .border(1.dp, borderColor, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    letter, 
                                    color = if (isSelected) Color.Black else Color.Cyan, 
                                    fontSize = 13.sp, 
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(opt, color = if (isSelected) Color.White else Color.LightGray, fontSize = 15.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!state.revealed) {
                TextButton(
                    onClick = { 
                        onAnswer(-1, false) // Using -1 as skip
                        timerActive = false
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Skip / Reveal", color = Color.Gray, fontSize = 12.sp)
                }
            } else {
                Surface(
                    color = Color(0xFF1E293B).copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                androidx.compose.material.icons.Icons.Default.Info, 
                                contentDescription = null, 
                                tint = Color.Cyan, 
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SCIENTIFIC INSIGHT", color = Color.Cyan, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(q.explanation, color = Color.LightGray, fontSize = 13.sp, lineHeight = 20.sp)
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = Color(0xFF22C55E).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "ANSWER: ${q.answer}", 
                                color = Color(0xFF22C55E), 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

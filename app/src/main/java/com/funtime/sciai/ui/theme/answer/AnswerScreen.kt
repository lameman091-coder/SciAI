package com.funtime.sciai.ui.theme.answer

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.funtime.sciai.components.AppScaffold
import com.funtime.sciai.data.groq.GroqService
import com.funtime.sciai.data.rag.RagService
import androidx.compose.runtime.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment


import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.unit.dp
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
    
    val userManager = remember { com.funtime.sciai.data.UserManager(context) }
    val userId = remember { userManager.getUserId() }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isLoading,
        onRefresh = { refreshTrigger++ }
    )

    LaunchedEffect(question, mode, expertLevel, selectedDomain, hybridMode, refreshTrigger) {
        isLoading = true
        isTyping = false
        displayedText = ""
        answer = "Loading..."
        responseType = if (hybridMode) "HYBRID" else "LLM_ONLY"
        responseSources = emptyList()

        if (hybridMode) {
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

                if (mode == "Expert") {
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
                } // End of top item block

                if (isLoading) {
                    item {
                    // PREMIUM CIRCULAR LOADING UI
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

            PullRefreshIndicator(
                refreshing = isLoading,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                backgroundColor = Color(0xFF1E293B),
                contentColor = Color(0xFF38BDF8)
            )
        }
    }
}









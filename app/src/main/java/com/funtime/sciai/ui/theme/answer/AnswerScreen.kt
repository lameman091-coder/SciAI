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
import androidx.compose.runtime.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape


fun cleanMath(text: String): String {
    return text
        // Remove LaTeX blocks
        .replace(Regex("\\$\\$.*?\\$\\$", RegexOption.DOT_MATCHES_ALL), "")
        // Remove \frac patterns (basic cleanup)
        .replace("\\\\frac".toRegex(), "")
        .replace("\\\\".toRegex(), "")
}
fun cleanResponse(text: String): String {
    return text
        .replace("**", "")
        .replace("##", "")
        .replace("*", "")
        .replace("•", "-")
        .replace(Regex("\\n{2,}"), "\n")
        .trim()
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

    return sections
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

                Text(
                    text = content,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun AnswerScreen(
    question: String,
    mode: String,
    navController: NavController
) {
    val context = LocalContext.current
    var answer by remember { mutableStateOf("Loading...") }
    var displayedText by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var expertLevel by remember { mutableStateOf("Academic") }
    var selectedDomain by remember { mutableStateOf("Biology") }

    LaunchedEffect(question, mode, expertLevel, selectedDomain) {
        isLoading = true
        isTyping = false
        displayedText = ""
        answer = "Loading..."

        val result = suspendCancellableCoroutine<String> { continuation ->
            GroqService.ask(
                question = question,
                mode = mode,
                level = expertLevel,
                domain = selectedDomain
            ) { res ->
                if (continuation.isActive) {
                    continuation.resume(res)
                }
            }
        }
        
        // Strip out <think> internal reasoning blocks
        val strippedResult = result.replace(Regex("<think>.*?</think>", RegexOption.DOT_MATCHES_ALL), "").trim()
        
        println("Answer: $strippedResult")
        answer = strippedResult
        
        // Save history item locally
        if (strippedResult.isNotBlank() && !strippedResult.startsWith("Error")) {
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
        
        isLoading = false
        isTyping = true
    }

    LaunchedEffect(answer, isTyping) {
        if (!isLoading && isTyping && answer != "Loading...") {
            var currentText = ""
            val chunkLength = 4
            for (i in 0 until answer.length step chunkLength) {
                val end = if (i + chunkLength > answer.length) answer.length else i + chunkLength
                currentText += answer.substring(i, end)
                displayedText = currentText
                delay(15)
            }
            isTyping = false
        }
    }

    val scrollState = rememberScrollState()

    LaunchedEffect(displayedText) {
        if (isTyping) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    AppScaffold(
        title = mode,
        navController = navController,
        showBack = true
    ) { scaffoldModifier ->

        Column(
            modifier = scaffoldModifier
                .fillMaxSize()
                .verticalScroll(scrollState) // ✅ FIX
                .padding(16.dp)
        ) {


// 🔥 EXPERT LEVEL BUTTONS (keep this)
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


// 🔥 MAIN ANSWER BLOCK (THIS IS THE FIX)
            if (isLoading) {

                LoadingUI()

            } else {
                
                // Show cursor text depending on whether it's currently typing
                val cursorText = if (isTyping) " █" else ""

                val cleanedText = cleanMath(
                    cleanResponse(displayedText) // Use progressively typed text
                        .replace("<br>", "\n")
                        .replace("•", "-")
                        .replace("*", "")
                        .replace("|", "")
                )

                val sections = parseDynamicSections(cleanedText)

                if (sections.isNotEmpty()) {

                    sections.forEachIndexed { index, pair ->
                        val (title, content) = pair
                        val isLast = index == sections.lastIndex
                        
                        ExpandableSection(
                            title = title,
                            content = content + if (isLast) cursorText else "",
                            mode = mode,
                            isTyping = isLast && isTyping
                        )
                    }

                } else {

                    // Fallback for un-sectioned text
                    Text(
                        text = cleanedText + cursorText,
                        color = Color.White
                    )
                }
            }
        }
    }
  }







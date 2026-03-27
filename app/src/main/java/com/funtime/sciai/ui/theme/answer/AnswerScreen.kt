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

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
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
fun parseDynamicSections(text: String): List<Pair<String, String>> {

    val lines = text.split("\n")
    val sections = mutableListOf<Pair<String, String>>()

    var currentTitle = ""
    var currentContent = StringBuilder()

    for (line in lines) {

        val trimmed = line.trim()

        val isHeading = trimmed.length in 3..60 &&
                !trimmed.endsWith(".") &&
                !trimmed.contains(":") &&
                trimmed.split(" ").size <= 8

        if (isHeading) {

            if (currentTitle.isNotEmpty()) {
                sections.add(currentTitle to currentContent.toString().trim())
                currentContent = StringBuilder()
            }

            currentTitle = trimmed
        } else {
            currentContent.append(line).append("\n")
        }
    }

    if (currentTitle.isNotEmpty()) {
        sections.add(currentTitle to currentContent.toString().trim())
    }

    return sections
}
@Composable
fun ExpandableSection(
    title: String,
    content: String,
    mode: String
) {

    var expanded by remember { mutableStateOf(false) }

    val borderColor = when (mode) {
        "Exam" -> Color(0xFFFFC107)     // Yellow
        "Concept" -> Color(0xFF2196F3)  // Blue
        "Expert" -> Color(0xFF9C27B0)   // Purple
        else -> Color.Gray
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.5.dp, borderColor, shape = RoundedCornerShape(12.dp))
            .padding(12.dp)
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

@Composable
fun AnswerScreen(
    question: String,
    mode: String,
    navController: NavController
) {
    var answer by remember { mutableStateOf("Loading...") }
    var isLoading by remember { mutableStateOf(true) }

    var hasLoaded by remember { mutableStateOf(false) }
    var expertLevel by remember { mutableStateOf("Academic") }
    var selectedDomain by remember { mutableStateOf("Biology") }
    LaunchedEffect(Unit) {
        if (!hasLoaded) {
            hasLoaded = true

            GroqService.ask(
                question = question,
                mode = mode,
                level = expertLevel,
                domain = selectedDomain
            ) { result ->
                answer = result
            }
        }
    }

    if (isLoading) {
        LoadingUI()
    } else {

        val cleanedText = cleanMath(
            cleanResponse(answer)
                .replace("<br>", "\n")
                .replace("+", " ")
                .replace("|", "")
        )

        val sections = parseDynamicSections(cleanedText)

        if (sections.isEmpty()) {
            Text(cleanedText)
        } else {
            sections.forEach { section ->
                Text(
                    text = section.toString(),
                    color = Color.White
                )
            }
        }
    }


    AppScaffold(
        title = mode,
        navController = navController,
        showBack = true
    ) { padding ->

        Column(
            modifier = Modifier

                .fillMaxSize()
                .verticalScroll(rememberScrollState()) // ✅ FIX
                .padding(16.dp)
        ) {

            Text("Question:")
            Text(question)

            Spacer(modifier = Modifier.height(16.dp))

            Text("Mode: $mode")

            Spacer(modifier = Modifier.height(16.dp))
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
                                .padding(8.dp)
                                .clickable {
                                    expertLevel = level

                                    GroqService.ask(
                                        question = question,
                                        mode = mode,
                                        level = expertLevel,
                                        domain = "Biology"
                                    ) {
                                        answer = it
                                    }
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            val cleanedText = cleanMath(
                cleanResponse(answer)

                        .replace("<br>", "\n")
                        .replace("•", "-")
                        .replace("*", "")
                        .replace("|", "")

            )

            val sections = parseDynamicSections(cleanedText)


            if (sections.isEmpty()) {
                Text(cleanedText)
            } else {
                sections.forEach { (title, content) ->
                    ExpandableSection(title, content, mode)
                }
            }
        }
    }
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



package com.funtime.sciai.ui.theme.answer


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
fun parseDynamicSections(text: String): List<Pair<String, String>> {

    val sections = mutableListOf<Pair<String, String>>()

    val regex = Regex("(\\w[\\w\\s]+):")

    val matches = regex.findAll(text).toList()

    if (matches.isEmpty()) {
        return listOf("ANSWER" to text)
    }

    for (i in matches.indices) {

        val start = matches[i].range.first
        val end = if (i < matches.size - 1)
            matches[i + 1].range.first
        else
            text.length

        val title = matches[i].value.replace(":", "").trim()
        val content = text.substring(start + matches[i].value.length, end).trim()

        sections.add(title to content)
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
    var expertLevel by remember { mutableStateOf("Academic") }
    LaunchedEffect(Unit) {
        GroqService.ask(
            question = question,
            mode = mode,
            level = expertLevel,
            domain = "Biology" // temporary (we'll make dynamic later)
        ) {
            answer = it
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

            val cleaned = answer
                .replace("<br>", "\n")
                .replace("•", "-")
                .replace("*", "")
                .replace("|", "")

            val sections = parseDynamicSections(cleaned)

            sections.forEach { (title, content) ->
                ExpandableSection(title, content, mode)
            }
            if (sections.isEmpty()) {
                Text(cleaned)
            } else {
                sections.forEach { (title, content) ->
                    ExpandableSection(title, content, mode)
                }
            }
        }
    }
}


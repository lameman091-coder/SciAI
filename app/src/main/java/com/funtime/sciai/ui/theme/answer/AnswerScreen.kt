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
fun ExpandableSection(title: String, content: String) {

    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {

        Text(
            text = "▼ $title",
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(12.dp)
        )

        if (expanded) {
            Text(
                text = content,
                modifier = Modifier.padding(12.dp)
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
    LaunchedEffect(Unit) {
        GroqService.ask(
            question = question,
            mode = mode
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

            val cleaned = answer
                .replace("<br>", "\n")
                .replace("•", "-")
                .replace("*", "")
                .replace("|", "")

            val sections = parseDynamicSections(cleaned)

            sections.forEach { (title, content) ->
                ExpandableSection(title, content)
            }
            if (sections.isEmpty()) {
                Text(cleaned)
            } else {
                sections.forEach { (title, content) ->
                    ExpandableSection(title, content)
                }
            }
        }
    }
}


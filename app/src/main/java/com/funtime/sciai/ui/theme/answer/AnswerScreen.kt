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

            Text(
                text = cleaned,
                lineHeight = 20.sp
            )
        }
    }
}


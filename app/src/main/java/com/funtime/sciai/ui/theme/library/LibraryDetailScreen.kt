package com.funtime.sciai.ui.theme.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.funtime.sciai.components.AppScaffold
import com.funtime.sciai.data.rag.RagService
import com.funtime.sciai.ui.theme.answer.ExpandableSection
import com.funtime.sciai.ui.theme.answer.cleanMath
import com.funtime.sciai.ui.theme.answer.cleanResponse
import com.funtime.sciai.ui.theme.answer.parseDynamicSections
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryDetailScreen(
    bookId: String,
    navController: NavController
) {
    var query by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf<String?>(null) }
    var isSearching by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    AppScaffold(
        title = "Book Search",
        navController = navController,
        showBack = true
    ) { scaffoldModifier ->
        Column(
            modifier = scaffoldModifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            
            // Search Input
            val doSearch = {
                if (query.isNotBlank()) {
                    val encodedQuery = Uri.encode(query)
                    navController.navigate("answer/$encodedQuery/Library?bookId=$bookId&hybrid=true")
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Ask about this book...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isSearching,
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { doSearch() }
                ),
                trailingIcon = {
                    IconButton(
                        onClick = { doSearch() },
                        enabled = query.isNotBlank() && !isSearching
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Search Book",
                            tint = if (isSearching) Color.Gray else Color(0xFF38BDF8)
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Book Search Output (Using SAME UI STYLE as AnswerScreen visually)
            if (isSearching) {
                Text("Searching Database...", color = Color.Gray)
            } else if (answer != null) {
                Column(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(scrollState)) {
                    val cleanedText = cleanMath(
                        cleanResponse(answer!!)
                            .replace("<br>", "\n")
                    )
                    val sections = parseDynamicSections(cleanedText)

                    if (sections.isNotEmpty()) {
                        sections.forEach { (title, content) ->
                            ExpandableSection(
                                title = title,
                                content = content,
                                mode = "Exam", // Yellow border styling by default
                                isTyping = false
                            )
                        }
                    } else {
                        Text(text = cleanedText, color = Color.White)
                    }
                }
            }
        }
    }
}

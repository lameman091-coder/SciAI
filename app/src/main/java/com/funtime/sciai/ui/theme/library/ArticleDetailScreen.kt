package com.funtime.sciai.ui.theme.library

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.funtime.sciai.components.AppScaffold
import com.funtime.sciai.data.UserManager
import com.funtime.sciai.data.network.ArticleState
import com.funtime.sciai.data.rag.RagService
import kotlinx.coroutines.launch

@Composable
fun ArticleDetailScreen(
    navController: NavController
) {
    // ── Pre-State Context Hoisting ──
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Smart Articles States 
    var isGeneratingKeyPoints by remember { mutableStateOf(false) }
    var keyPoints by remember { mutableStateOf<List<String>>(emptyList()) }
    
    var isGeneratingTopics by remember { mutableStateOf(false) }
    var relatedTopics by remember { mutableStateOf<List<String>>(emptyList()) }
    
    // ── Data & Logic ──
    val article = ArticleState.selectedArticle
    val userManager = remember { UserManager(context) }
    val userId = remember { userManager.getUserId() }
    
    val title = article?.title ?: "No Title"
    val summary = article?.summary ?: "No description available"
    val source = article?.source ?: "Unknown Source"
    val articleLink = article?.link ?: ""

    AppScaffold(
        title = "Article View",
        navController = navController,
        showBack = true
    ) { padding ->
        // Use a Box as the immediate container for the Snackbar alignment scope.
        Box(modifier = Modifier.fillMaxSize()) {
            
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                item {
                // Trust HUD
                val tierColor = when (article?.tier) {
                    "peer_reviewed" -> TrustGreen
                    "preprint" -> TrustYellow
                    "background" -> TrustBlue
                    else -> CyanAccent
                }
                val tierLabel = when (article?.tier) {
                    "peer_reviewed" -> "Peer Reviewed"
                    "preprint" -> "Preprint"
                    "background" -> "General"
                    else -> "Research"
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = CyanAccent.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = source.ifBlank { "Unknown Source" }.uppercase(),
                            color = CyanAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(tierColor))
                        Text(tierLabel, color = tierColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = title.ifBlank { "No Title" },
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 32.sp
                )
                
                if (!article?.authors.isNullOrBlank()) {
                    Text(
                        text = "By ${article?.authors}",
                        color = SlateGray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (!article?.date.isNullOrBlank() || !article?.journal.isNullOrBlank()) {
                    Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (!article?.journal.isNullOrBlank() && article?.source != "Wikipedia") {
                            Text(
                                text = article?.journal ?: "",
                                color = CyanAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(" • ", color = Color.Gray)
                        }
                        Text(
                            text = article?.date ?: "",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(16.dp))

                // ── 🔥 SMART ARTICLES (Phase 3) ──
                Text("🧠 Smart Actions", color = CyanAccent, fontWeight = FontWeight.Black, fontSize = 14.sp, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        OutlinedCard(
                            modifier = Modifier.clickable { 
                                val encoded = Uri.encode(title)
                                navController.navigate("answer/$encoded/Exam?hybrid=true")
                            },
                            colors = CardDefaults.outlinedCardColors(containerColor = CyanAccent.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, CyanAccent)
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Assignment, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Exam Mode", color = CyanAccent, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    item {
                        OutlinedCard(
                            modifier = Modifier.clickable { 
                                if (keyPoints.isEmpty()) {
                                    isGeneratingKeyPoints = true
                                    val prompt = "Extract 4 brief key bullet points for the article titled '$title'."
                                    RagService.ask(
                                        question = prompt,
                                        mode = "Concept",
                                        domain = "Science",
                                        userId = userId
                                    ) { res ->
                                        if (res != null && res.answer.isNotBlank()) {
                                            keyPoints = res.answer.split("\n").filter { it.isNotBlank() }
                                        } else {
                                            scope.launch { snackbarHostState.showSnackbar("Failed to generate key points") }
                                        }
                                        isGeneratingKeyPoints = false
                                    }
                                }
                            },
                            colors = CardDefaults.outlinedCardColors(containerColor = Color(0xFFF59E0B).copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B))
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (isGeneratingKeyPoints) CircularProgressIndicator(color = Color(0xFFF59E0B), modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                else Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Key Points", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    item {
                        OutlinedCard(
                            modifier = Modifier.clickable { 
                                if (relatedTopics.isEmpty()) {
                                    isGeneratingTopics = true
                                    val prompt = "List 3 related scientific topics to '$title'. Output ONLY comma separated list."
                                    RagService.ask(
                                        question = prompt,
                                        mode = "Concept",
                                        domain = "Science",
                                        userId = userId
                                    ) { res ->
                                        if (res != null && res.answer.isNotBlank()) {
                                            relatedTopics = res.answer.split(",").map{ it.trim() }.filter { it.isNotBlank() }
                                        } else {
                                            scope.launch { snackbarHostState.showSnackbar("Failed to generate topics") }
                                        }
                                        isGeneratingTopics = false
                                    }
                                }
                            },
                            colors = CardDefaults.outlinedCardColors(containerColor = Color(0xFFA855F7).copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, Color(0xFFA855F7))
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (isGeneratingTopics) CircularProgressIndicator(color = Color(0xFFA855F7), modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                else Icon(Icons.Default.Tag, contentDescription = null, tint = Color(0xFFA855F7), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Related Topics", color = Color(0xFFA855F7), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Display outputs if generated
                if (keyPoints.isNotEmpty()) {
                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Key Points", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            keyPoints.forEach { pt ->
                                val cleanPt = pt.trim().removePrefix("-").removePrefix("*").trim()
                                if (cleanPt.isNotEmpty()) {
                                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                        Text("•", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(cleanPt, color = Color(0xFFCBD5E1), fontSize = 14.sp, lineHeight = 20.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                if (relatedTopics.isNotEmpty()) {
                    Text("Related Topics", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        relatedTopics.forEach { topic ->
                            val cleanTopic = topic.trim().removePrefix("-").removePrefix("*").trim()
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFA855F7).copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.5f)),
                                modifier = Modifier.clickable {
                                    val encoded = Uri.encode(cleanTopic)
                                    navController.navigate("answer/$encoded/Concept?hybrid=true")
                                }
                            ) {
                                Text(cleanTopic, color = Color(0xFFA855F7), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(24.dp))
                }
                
                val paragraphs = summary.ifBlank { "No description available" }.split('\n')
                
                items(paragraphs) { paragraph ->
                    if (paragraph.isNotBlank()) {
                        Text(
                            text = paragraph.trim(),
                            color = Color(0xFFCBD5E1),
                            fontSize = 17.sp,
                            lineHeight = 26.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }
                
                item {
                
                Spacer(modifier = Modifier.height(48.dp))
                
                if (articleLink.isNotEmpty()) {
                    Button(
                        onClick = { 
                            val encodedUrl = Uri.encode(articleLink)
                            navController.navigate("webView/$encodedUrl")
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Read in App", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = { 
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(articleLink))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                        border = BorderStroke(1.5.dp, CyanAccent),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Public, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Open in Browser", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                Button(
                    onClick = {
                        article?.let {
                            RagService.saveArticle(userId, it) { success ->
                                scope.launch {
                                    if (success) snackbarHostState.showSnackbar("Saved to library")
                                    else snackbarHostState.showSnackbar("Failed to save")
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Save to Library", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                    TextButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
                    ) {
                        Text("Back to Search")
                    }
                } // Close item block
            } // Close LazyColumn
            
            // Align the snackbar directly within the parent Box (BoxScope).
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        } // Close Box
    } // Close AppScaffold Lambda
} // Close ArticleDetailScreen

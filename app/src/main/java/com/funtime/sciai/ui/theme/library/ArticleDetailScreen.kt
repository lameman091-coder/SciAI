package com.funtime.sciai.ui.theme.library

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Public
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
    ) { scaffoldModifier ->
        // Use a Box as the immediate container for the Snackbar alignment scope.
        Box(modifier = Modifier.fillMaxSize()) {
            
            Column(
                modifier = scaffoldModifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
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
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = summary.ifBlank { "No description available" },
                    color = Color(0xFFCBD5E1),
                    fontSize = 17.sp,
                    lineHeight = 26.sp
                )
                
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
            } // Close Column
            
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

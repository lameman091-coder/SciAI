package com.funtime.sciai.ui.theme.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.funtime.sciai.data.network.ArticleState
import com.funtime.sciai.data.network.Article
import com.funtime.sciai.components.AppScaffold
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Description

@Composable
fun ArticleDetailScreen(
    navController: NavController
) {
    val article = ArticleState.selectedArticle
    val title = article?.title ?: "No Title"
    val summary = article?.summary ?: "No description available"
    val source = article?.source ?: "Unknown Source"
    AppScaffold(
        title = "Article View",
        navController = navController,
        showBack = true
    ) { scaffoldModifier ->
        Column(
            modifier = scaffoldModifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Trust indicator row
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
                    color = Color(0xFF94A3B8),
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
                            color = Color(0xFF38BDF8),
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
            
            Divider(color = Color.Gray.copy(alpha = 0.3f))
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = summary.ifBlank { "No description available" },
                color = Color(0xFFCBD5E1),
                fontSize = 17.sp,
                lineHeight = 26.sp
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            val articleLink = article?.link ?: ""
            
            if (articleLink.isNotEmpty()) {
                // Button 1: Read in App (WebView)
                Button(
                    onClick = { 
                        val encodedUrl = Uri.encode(articleLink)
                        navController.navigate("webView/$encodedUrl")
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Description, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Read in App", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Button 2: Open in Browser (External Intent)
                OutlinedButton(
                    onClick = { 
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(articleLink))
                        navController.context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth().height(51.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Public, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Open in Browser", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
            ) {
                Text("Back to Search")
            }
        }
    }
}

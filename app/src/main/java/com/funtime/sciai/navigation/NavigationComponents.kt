package com.funtime.sciai.navigation

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.funtime.sciai.data.HistoryItem
import com.funtime.sciai.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

fun safeNavigate(
    navController: NavController,
    drawerState: DrawerState,
    route: String,
    scope: kotlinx.coroutines.CoroutineScope
) {
    // Close drawer immediately and independently
    scope.launch { 
        try {
            drawerState.close() 
        } catch (e: Exception) {
            // Ignore cancellation or animation errors
        }
    }

    try {
        navController.navigate(route) {
            launchSingleTop = true
            popUpTo("home")
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
fun DrawerItem(
    title: String,
    emoji: String = "",
    textColor: Color = Color.White,
    enabled: Boolean = true,
    isActive: Boolean = false,
    onClick: () -> Unit = {}
) {
    Surface(
        color = if (isActive) SciAICyan.copy(alpha = 0.08f) else Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onClick() }
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Active indicator
            if (isActive) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(SciAICyan)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            if (emoji.isNotBlank()) {
                Text(emoji, fontSize = 18.sp)
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            Text(
                text = title,
                color = if (isActive) SciAICyan else textColor,
                fontSize = 15.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun DrawerHeader(userName: String?, level: Int, title: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(SciAISurface, SciAISurfaceAlt)
                )
            )
            .padding(24.dp)
    ) {
        // Avatar placeholder
        Surface(
            color = SciAICyan.copy(alpha = 0.15f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.size(52.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, SciAICyan.copy(alpha = 0.4f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = (userName?.firstOrNull()?.uppercase() ?: "S"),
                    color = SciAICyan,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = userName ?: "Student",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Lv.$level", color = SciAICyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(" · ", color = SciAIMuted, fontSize = 12.sp)
            Text(title, color = SciAIAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DrawerSectionLabel(label: String) {
    Text(
        text = label,
        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp),
        color = SciAIMuted,
        fontWeight = FontWeight.Black,
        fontSize = 10.sp,
        letterSpacing = 1.5.sp
    )
}

@Composable
fun HistoryDrawerItem(item: HistoryItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        val dateStr = sdf.format(Date(item.timestamp))

        Text(
            text = item.query,
            color = Color.White,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(modifier = Modifier.padding(top = 4.dp)) {
            Text(
                text = "${item.mode} • ${item.domain}",
                color = SciAISubtext,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = dateStr,
                color = SciAIMuted,
                fontSize = 10.sp
            )
        }
    }
}

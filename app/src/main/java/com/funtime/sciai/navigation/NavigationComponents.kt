package com.funtime.sciai.navigation

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.funtime.sciai.data.HistoryItem
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.runtime.Composable

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth


import androidx.compose.material3.Text


import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@OptIn(ExperimentalMaterial3Api::class)
suspend fun safeNavigate(
    navController: NavController,
    drawerState: DrawerState,
    route: String,
    isNavigatingState: MutableState<Boolean>
) {
    if (isNavigatingState.value) return

    isNavigatingState.value = true

    drawerState.close()

    // 🔥 simple & reliable instead of snapshotFlow
    delay(250)

    navController.navigate(route) {
        launchSingleTop = true
        restoreState = true
        popUpTo(navController.graph.startDestinationId) {
            saveState = true
        }
    }

    delay(200)

    isNavigatingState.value = false
}

@Composable
fun DrawerItem(
    title: String,
    textColor: Color = Color.White,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    Text(
        text = title,
        color = textColor,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) {
                onClick()
            }
            .padding(16.dp)
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
                color = Color(0xFF94A3B8), // slate 400
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = dateStr,
                color = Color(0xFF64748B), // slate 500
                fontSize = 10.sp
            )
        }
    }
}

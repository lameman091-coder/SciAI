package com.funtime.sciai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.funtime.sciai.data.UserManager
import com.funtime.sciai.data.tts.VoiceStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumSettingsBottomSheet(
    userManager: UserManager,
    isVisible: Boolean,
    onDismissRequest: () -> Unit
) {
    if (!isVisible) return

    val context = LocalContext.current

    // State loading from UserManager
    var ttsEnabled by remember { mutableStateOf(userManager.isTTSEnabled()) }
    var selectedVoice by remember { mutableStateOf(VoiceStyle.fromId(userManager.getTTSVoiceStyle())) }
    var autoDomain by remember { mutableStateOf(userManager.isAutoDomainDetection()) }
    var saveHistory by remember { mutableStateOf(userManager.isSaveHistory()) }
    var fastMode by remember { mutableStateOf(userManager.isFastMode()) }
    var expertDefault by remember { mutableStateOf(userManager.isExpertModeDefault()) }
    var privacyMode by remember { mutableStateOf(userManager.isPrivacyMode()) }
    var themeSelection by remember { mutableStateOf(userManager.getThemeSelection()) }
    var hybridDefault by remember { mutableStateOf(userManager.isHybridDefault()) }

    var showVoiceSheet by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xFF0F172A), // Dark slate background
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray.copy(alpha = 0.5f)) },
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ── Profile Header ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E293B))
                .padding(top = 48.dp, bottom = 24.dp, start = 20.dp, end = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF38BDF8).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🤖", fontSize = 28.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        "SciAI Premium",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "User Profile",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Audio Settings ──
        SettingsHeader("Audio & Voice")

        SettingsSwitchItem(
            icon = Icons.Default.VolumeUp,
            title = "TTS Enabled",
            subtitle = "Read answers aloud automatically",
            checked = ttsEnabled,
            onCheckedChange = {
                ttsEnabled = it
                userManager.setTTSEnabled(it)
            }
        )

        // Voice Selection Item (Opens Bottom Sheet)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = ttsEnabled) { showVoiceSheet = true },
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PersonSearch,
                    contentDescription = null,
                    tint = if (ttsEnabled) Color(0xFF38BDF8) else Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Voice Selection",
                        color = if (ttsEnabled) Color.White else Color.Gray,
                        fontSize = 16.sp
                    )
                    Text(
                        "${selectedVoice.displayName} (${selectedVoice.gender})",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = if (ttsEnabled) Color.Gray else Color.Transparent
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Divider(color = Color.White.copy(alpha = 0.05f))
        Spacer(modifier = Modifier.height(16.dp))

        // ── App Settings ──
        SettingsHeader("Preferences")

        SettingsSwitchItem(
            icon = Icons.Default.AutoAwesome,
            title = "Auto Domain Detection",
            subtitle = "Smart routing for subject Context",
            checked = autoDomain,
            tint = Color(0xFF10B981), // Emerald
            onCheckedChange = {
                autoDomain = it
                userManager.setAutoDomainDetection(it)
            }
        )

        SettingsSwitchItem(
            icon = Icons.Default.History,
            title = "Save History",
            subtitle = "Keep record of asked queries",
            checked = saveHistory,
            onCheckedChange = {
                saveHistory = it
                userManager.setSaveHistory(it)
            }
        )

        SettingsSwitchItem(
            icon = Icons.Default.Speed,
            title = "Fast Mode (Low Token)",
            subtitle = "Faster responses, less detail",
            checked = fastMode,
            tint = Color(0xFFF59E0B), // Amber
            onCheckedChange = {
                fastMode = it
                userManager.setFastMode(it)
            }
        )

        SettingsSwitchItem(
            icon = Icons.Default.Hub,
            title = "Default Hybrid Mode",
            subtitle = "RAG research enabled by default",
            checked = hybridDefault,
            onCheckedChange = {
                hybridDefault = it
                userManager.setHybridDefault(it)
            }
        )

        SettingsSwitchItem(
            icon = Icons.Default.Psychology,
            title = "Expert Mode Default",
            subtitle = "Always default to expert answers",
            checked = expertDefault,
            onCheckedChange = {
                expertDefault = it
                userManager.setExpertModeDefault(it)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
        Divider(color = Color.White.copy(alpha = 0.05f))
        Spacer(modifier = Modifier.height(16.dp))

        // ── Privacy & Appearance ──
        SettingsHeader("Security & Look")

        SettingsSwitchItem(
            icon = Icons.Default.Lock,
            title = "Privacy Mode",
            subtitle = "Do not use queries for training",
            checked = privacyMode,
            tint = Color(0xFFEF4444), // Red
            onCheckedChange = {
                privacyMode = it
                userManager.setPrivacyMode(it)
            }
        )

        // Theme selection pseudo-item
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    // Cycle theme
                    themeSelection = when (themeSelection) {
                        "system" -> "dark"
                        "dark" -> "light"
                        else -> "system"
                    }
                    userManager.setThemeSelection(themeSelection)
                },
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Theme Selection", color = Color.White, fontSize = 16.sp)
                    Text(themeSelection.capitalize(), color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        }
    }

    // ── Render Voice Bottom Sheet if open ──
    if (showVoiceSheet) {
        VoiceSelectionBottomSheet(
            isVisible = showVoiceSheet,
            selectedStyle = selectedVoice,
            onStyleSelected = {
                selectedVoice = it
                userManager.setTTSVoiceStyle(it.id)
            },
            onDismissRequest = { showVoiceSheet = false }
        )
    }
}

@Composable
fun SettingsHeader(title: String) {
    Text(
        text = title.uppercase(),
        color = Color.Gray,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    tint: Color = Color(0xFF38BDF8),
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) tint else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = tint,
                checkedTrackColor = tint.copy(alpha = 0.3f),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color(0xFF1E293B)
            )
        )
    }
}

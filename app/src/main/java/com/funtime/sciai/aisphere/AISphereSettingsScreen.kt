package com.funtime.sciai.aisphere

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

/**
 * Settings screen for the AI Sphere Companion.
 * Enhanced with affection display, sound toggle, and new personality modes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AISphereSettingsScreen(
    navController: NavController,
    viewModel: AISphereViewModel
) {
    val isEnabled by viewModel.isEnabled.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val isSoundEnabled by viewModel.isSoundEnabled.collectAsState()
    val reduceAnimations by viewModel.reduceAnimations.collectAsState()
    val personalityMode by viewModel.personalityMode.collectAsState()
    val emotionState by viewModel.emotionState.collectAsState()
    val petHappiness by viewModel.petHappiness.collectAsState()
    val affectionLevel by viewModel.affectionLevel.collectAsState()
    val companionProfile by viewModel.companionProfile.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Companion Settings", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                navigationIcon = {
                    FilledIconButton(
                        onClick = { navController.popBackStack() },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0xFF1E293B),
                            contentColor = Color(0xFF38BDF8)
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0F1C))
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Preview sphere with emotion & stats ─────────────
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Talk to ${companionProfile.name}",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Live sphere preview
                    AISphereComposable(
                        emotionState = emotionState,
                        reduceAnimations = reduceAnimations,
                        size = 80.dp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Kaomoji + mood name
                    val visuals = EmotionPalette.getVisuals(emotionState)
                    Text(
                        text = visuals.kaomoji,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Mood: ${emotionState.name.lowercase().replaceFirstChar { it.uppercase() }}",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Happiness indicator
                    StatBar(
                        label = "Happiness",
                        value = petHappiness,
                        color = when {
                            petHappiness > 0.7f -> Color(0xFFFBBF24)
                            petHappiness > 0.3f -> Color(0xFF38BDF8)
                            else -> Color(0xFF64748B)
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Affection indicator (heart meter)
                    StatBar(
                        label = "Affection ♡",
                        value = affectionLevel,
                        color = when {
                            affectionLevel > 0.7f -> Color(0xFFEC4899)
                            affectionLevel > 0.3f -> Color(0xFFF472B6)
                            else -> Color(0xFFFDA4AF)
                        }
                    )
                }
            }

            // ── Identity Settings ───────────────────────────────
            SettingsSection(title = "Identity") {
                // Name
                OutlinedTextField(
                    value = companionProfile.name,
                    onValueChange = { if (it.length <= 15) viewModel.updateCompanionProfile(companionProfile.copy(name = it)) },
                    label = { Text("Name", color = Color(0xFF94A3B8)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = companionProfile.colorTheme.primary,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        unfocusedBorderColor = Color(0xFF334155),
                        cursorColor = companionProfile.colorTheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))
                
                // Gender
                Text("Gender", color = Color(0xFF94A3B8), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CompanionGender.values().forEach { gender ->
                        SelectableChip(
                            modifier = Modifier.weight(1f),
                            text = "${gender.emoji} ${gender.displayName}",
                            isSelected = companionProfile.gender == gender,
                            activeColor = companionProfile.colorTheme.primary,
                            onClick = { viewModel.updateCompanionProfile(companionProfile.copy(gender = gender)) }
                        )
                    }
                }
            }

            // ── Appearance Settings ─────────────────────────────
            SettingsSection(title = "Appearance") {
                // Glow
                SettingsToggleItem(
                    title = "Radiant Aura",
                    subtitle = "Enable glowing outer aura",
                    checked = companionProfile.hasGlow,
                    onCheckedChange = { viewModel.updateCompanionProfile(companionProfile.copy(hasGlow = it)) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Color Theme
                Text("Color Theme", color = Color(0xFF94A3B8), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(CompanionColorTheme.values()) { theme ->
                        ThemeSelectorItem(
                            theme = theme,
                            isSelected = companionProfile.colorTheme == theme,
                            onClick = { viewModel.updateCompanionProfile(companionProfile.copy(colorTheme = theme)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Accessory
                Text("Accessory", color = Color(0xFF94A3B8), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(CompanionAccessory.values()) { acc ->
                        SelectableChip(
                            text = if (acc == CompanionAccessory.NONE) "None" else "${acc.emoji} ${acc.displayName}",
                            isSelected = companionProfile.accessory == acc,
                            activeColor = companionProfile.colorTheme.primary,
                            onClick = { viewModel.updateCompanionProfile(companionProfile.copy(accessory = acc)) }
                        )
                    }
                }
            }

            // ── General settings ────────────────────────────────
            SettingsSection(title = "General") {
                SettingsToggleItem(
                    title = "Enable AI Companion",
                    subtitle = "Show the floating sphere on all screens",
                    checked = isEnabled,
                    onCheckedChange = { viewModel.setEnabled(it) }
                )

                SettingsToggleItem(
                    title = "Mute Messages",
                    subtitle = "Stop showing message bubbles",
                    checked = isMuted,
                    onCheckedChange = { viewModel.setMuted(it) }
                )

                SettingsToggleItem(
                    title = "Sound Effects",
                    subtitle = "Play cute sounds on emotion changes",
                    checked = isSoundEnabled,
                    onCheckedChange = { viewModel.setSoundEnabled(it) }
                )

                SettingsToggleItem(
                    title = "Reduce Animations",
                    subtitle = "Slower animations for less distraction",
                    checked = reduceAnimations,
                    onCheckedChange = { viewModel.setReduceAnimations(it) }
                )
            }

            // ── Personality mode ────────────────────────────────
            SettingsSection(title = "Personality") {
                PersonalityMode.values().forEach { mode ->
                    PersonalityModeItem(
                        mode = mode,
                        isSelected = personalityMode == mode,
                        onClick = { viewModel.setPersonalityMode(mode) }
                    )
                }
            }

            // ── Interaction guide ───────────────────────────────
            SettingsSection(title = "How It Works") {
                InfoItem(
                    emoji = "👆",
                    title = "Tap",
                    description = "Happy bounce + contextual tip"
                )
                InfoItem(
                    emoji = "👆👆",
                    title = "Double Tap",
                    description = "Shy reaction >~<"
                )
                InfoItem(
                    emoji = "👆👆👆👆",
                    title = "Rapid Tap",
                    description = "Annoyed reaction >:("
                )
                InfoItem(
                    emoji = "✋",
                    title = "Long Press",
                    description = "Love reaction ♡ or quick actions"
                )
                InfoItem(
                    emoji = "↔️",
                    title = "Drag",
                    description = "Move sphere anywhere on screen"
                )
                InfoItem(
                    emoji = "🫳",
                    title = "Pet",
                    description = "Swipe back & forth for affection!"
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StatBar(
    label: String,
    value: Float,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(0.7f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = Color(0xFF64748B),
                fontSize = 11.sp
            )
            Text(
                text = "${(value * 100).toInt()}%",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { value },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = Color(0xFF334155),
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title.uppercase(),
            color = Color(0xFF64748B),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(4.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = Color(0xFF94A3B8),
                fontSize = 12.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF38BDF8),
                checkedTrackColor = Color(0xFF38BDF8).copy(alpha = 0.3f),
                uncheckedThumbColor = Color(0xFF64748B),
                uncheckedTrackColor = Color(0xFF334155)
            )
        )
    }
}

@Composable
private fun PersonalityModeItem(
    mode: PersonalityMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val description = when (mode) {
        PersonalityMode.PLAYFUL -> "Bubbly, kaomoji-heavy, anime-inspired >w<"
        PersonalityMode.CALM -> "Gentle, supportive, and warm ♡"
        PersonalityMode.SILENT -> "No messages — visual companion only"
        PersonalityMode.STUDY -> "Minimal, academic hints for focus 📖"
    }

    val emoji = when (mode) {
        PersonalityMode.PLAYFUL -> "😄"
        PersonalityMode.CALM -> "😌"
        PersonalityMode.SILENT -> "🤫"
        PersonalityMode.STUDY -> "📚"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) Color(0xFF38BDF8).copy(alpha = 0.1f)
                else Color.Transparent
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = emoji,
            fontSize = 20.sp,
            modifier = Modifier.padding(end = 12.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mode.displayName,
                color = if (isSelected) Color(0xFF38BDF8) else Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                color = Color(0xFF94A3B8),
                fontSize = 12.sp
            )
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF38BDF8))
            )
        }
    }
}

@Composable
private fun InfoItem(
    emoji: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = emoji,
            fontSize = 18.sp,
            modifier = Modifier
                .width(44.dp)
                .padding(end = 8.dp)
        )
        Column {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                color = Color(0xFF94A3B8),
                fontSize = 12.sp
            )
        }
    }
}

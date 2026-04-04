package com.funtime.sciai.aisphere

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

/**
 * Onboarding screen for personalizing the AI Companion.
 * Appears on first launch.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanionSetupScreen(
    navController: NavController,
    viewModel: AISphereViewModel
) {
    var name by remember { mutableStateOf("Orbi") }
    var selectedGender by remember { mutableStateOf(CompanionGender.NEUTRAL) }
    var selectedTheme by remember { mutableStateOf(CompanionColorTheme.OCEAN) }
    var selectedAccessory by remember { mutableStateOf(CompanionAccessory.NONE) }
    var hasGlow by remember { mutableStateOf(true) }

    val scrollState = rememberScrollState()

    // Temporary profile for the live preview
    val previewProfile = CompanionProfile(
        name = name.ifBlank { "Companion" },
        gender = selectedGender,
        colorTheme = selectedTheme,
        accessory = selectedAccessory,
        hasGlow = hasGlow
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0A0F1C)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp) // Room for the bottom button
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // ── Live Preview ─────────────────────────────────────────
            Text(
                text = "Meet your companion",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Customize how they look and feel",
                fontSize = 16.sp,
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(40.dp))

            Box(
                modifier = Modifier
                    .size(160.dp)
                    .background(Color(0xFF1E293B).copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Large preview of the sphere
                AISphereComposable(
                    emotionState = EmotionState.HAPPY,
                    companionProfile = previewProfile,
                    size = 120.dp
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // ── Step 1: Name ─────────────────────────────────────────
            SectionTitle("1. What's their name?", "Give them an identity")
            
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 15) name = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = selectedTheme.primary,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    unfocusedBorderColor = Color(0xFF334155),
                    cursorColor = selectedTheme.primary
                ),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── Step 2: Gender ───────────────────────────────────────
            SectionTitle("2. Select a gender", "Affects future voice & personality")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CompanionGender.values().forEach { gender ->
                    SelectableChip(
                        modifier = Modifier.weight(1f),
                        text = "${gender.emoji} ${gender.displayName}",
                        isSelected = selectedGender == gender,
                        activeColor = selectedTheme.primary,
                        onClick = { selectedGender = gender }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Step 3: Color Theme ──────────────────────────────────
            SectionTitle("3. Choose a color theme", "Their emotional aura")

            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(CompanionColorTheme.values()) { theme ->
                    ThemeSelectorItem(
                        theme = theme,
                        isSelected = selectedTheme == theme,
                        onClick = { selectedTheme = theme }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Step 4: Accessorize ──────────────────────────────────
            SectionTitle("4. Add an accessory", "A little personal touch")

            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(CompanionAccessory.values()) { acc ->
                    SelectableChip(
                        text = if (acc == CompanionAccessory.NONE) "None" else "${acc.emoji} ${acc.displayName}",
                        isSelected = selectedAccessory == acc,
                        activeColor = selectedTheme.primary,
                        onClick = { selectedAccessory = acc }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Step 5: Glow ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .background(Color(0xFF1E293B), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Radiant Aura", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text("Enable glowing emotional aura", color = Color(0xFF94A3B8), fontSize = 14.sp)
                }
                Switch(
                    checked = hasGlow,
                    onCheckedChange = { hasGlow = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = selectedTheme.primary,
                        uncheckedThumbColor = Color(0xFF94A3B8),
                        uncheckedTrackColor = Color(0xFF334155)
                    )
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        // ── Floating Action Button (Continue) ──────────────────────
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Button(
                onClick = {
                    viewModel.updateCompanionProfile(previewProfile)
                    viewModel.completeSetup()
                    navController.navigate("home") {
                        popUpTo("companion_setup") { inclusive = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = selectedTheme.primary
                ),
                enabled = name.isNotBlank()
            ) {
                Text("Start Journey", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Color(0xFF94A3B8), fontSize = 14.sp)
    }
}

@Composable
fun SelectableChip(
    modifier: Modifier = Modifier,
    text: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) activeColor.copy(alpha = 0.2f) else Color(0xFF1E293B)
    val borderColor = if (isSelected) activeColor else Color.Transparent

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) activeColor else Color.White,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ThemeSelectorItem(
    theme: CompanionColorTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    brush = Brush.linearGradient(listOf(theme.primary, theme.secondary)),
                    shape = CircleShape
                )
                .border(
                    width = if (isSelected) 3.dp else 0.dp,
                    color = if (isSelected) Color.White else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            theme.displayName,
            color = if (isSelected) Color.White else Color.Gray,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

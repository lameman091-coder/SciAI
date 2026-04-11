package com.funtime.sciai.ui.theme.home
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.funtime.sciai.data.UserManager
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import android.widget.Toast
import com.funtime.sciai.data.GeminiService
import com.funtime.sciai.data.HistoryManager
import androidx.compose.material3.ElevatedCard
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard

import androidx.compose.material3.DrawerState
import androidx.compose.runtime.MutableState




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
@OptIn(ExperimentalMaterial3Api::class)


@Composable
fun HomeScreen(navController: NavController, drawerState: androidx.compose.material3.DrawerState) {
    val isNavigatingState = remember { mutableStateOf(false) }
    var isProcessingImage by remember { mutableStateOf(false) }

    var selectedDomain by remember { mutableStateOf("Biology") }
    var selectedMode by remember { mutableStateOf("Exam") }

    val context = LocalContext.current
    var query by remember { mutableStateOf("") }

    val activity = context as Activity
    val coroutineScope = rememberCoroutineScope()
    val userManager = UserManager(context)
    val sessionViewModel: SessionViewModel = viewModel()
    val sessionTime = sessionViewModel.sessionTime.value

    var totalCount by remember { mutableStateOf(0) }
    var historyList by remember { mutableStateOf<List<com.funtime.sciai.data.HistoryItem>>(emptyList()) }
    var userName by remember { mutableStateOf<String?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100) // let UI render first
        withContext(Dispatchers.IO) {
            val history = HistoryManager.getHistory(context)
            val total = userManager.getTotal()
            val name = userManager.getName()

            withContext(Dispatchers.Main) {
                historyList = history
                totalCount = total
                userName = name
                showDialog = name == null
            }
        }
    }

    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            imageUris = (imageUris + uris).distinct()
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val results = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                query = results[0]
            }
        }
    }

    val executeSearch = {
        coroutineScope.launch {
            if (imageUris.isNotEmpty()) {
                isProcessingImage = true
                val results = imageUris.map { uri ->
                    async(Dispatchers.IO) {
                        GeminiService.analyzeImage(context, uri)
                    }
                }.awaitAll()

                val combinedImageInfo = results.joinToString("\n")
                isProcessingImage = false

                userManager.incrementTotal()
                totalCount = userManager.getTotal()

                val finalQuery = if (query.isNotBlank())
                    "$query\n\nImage Info:\n$combinedImageInfo"
                else combinedImageInfo

                val encodedQuery = Uri.encode(finalQuery)
                navController.navigate("answer/$encodedQuery/$selectedMode?hybrid=true")
                imageUris = emptyList()
                query = ""
            } else if (query.isNotBlank()) {
                userManager.incrementTotal()
                totalCount = userManager.getTotal()
                val encodedQuery = Uri.encode(query)
                navController.navigate("answer/$encodedQuery/$selectedMode?hybrid=true")
                query = ""
            }
        }
    }

    if (showDialog) {
        var input by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {
                Button(onClick = {
                    if (input.isNotBlank()) {
                        userManager.saveName(input)
                        userName = input
                        showDialog = false
                    }
                }) {
                    Text("Start")
                }
            },
            title = { Text("Welcome") },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Enter your name") }
                )
            }
        )
    }

    com.funtime.sciai.components.AppScaffold(
        title = "SciAI",
        navController = navController,
        onMenuClick = { coroutineScope.launch { drawerState.open() } }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("Welcome, ${userName ?: "Student"}")
            Spacer(modifier = Modifier.height(8.dp))

            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = Color(0xFF0F172A)
                ),
                elevation = CardDefaults.elevatedCardElevation(
                    defaultElevation = 6.dp
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🔥 Progress")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⏱ ${formatSessionTime(sessionTime)}",
                        color = Color(0xFFFFC107)
                    )
                    Text("Total: ${userManager.getTotal()}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = getBadge(userManager.getTotal()),
                        color = Color(0xFFFFC107)
                    )
                }
            }

            Button(
                onClick = {
                    userManager.resetTotal()
                    totalCount = 0
                    sessionViewModel.resetTime()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text("Reset Progress")
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(listOf("Biology", "Physics", "Chemistry", "Science")) { domain ->
                    SelectableButton(
                        text = domain,
                        isSelected = selectedDomain == domain,
                        color = when (domain) {
                            "Biology" -> Color(0xFF4CAF50)
                            "Physics" -> Color(0xFF2196F3)
                            "Chemistry" -> Color(0xFFF44336)
                            "Science" -> Color(0xFF00BCD4)
                            else -> Color.Gray
                        }
                    ) {
                        selectedDomain = domain
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(listOf("Exam", "Concept", "Expert", "Quiz", "Test")) { mode ->
                    SelectableButton(
                        text = mode,
                        isSelected = selectedMode == mode,
                        color = when (mode) {
                            "Exam" -> Color(0xFFFFC107)
                            "Concept" -> Color(0xFF2196F3)
                            "Expert" -> Color(0xFF9C27B0)
                            "Quiz" -> Color(0xFF38BDF8)
                            "Test" -> Color(0xFFEF4444)
                            else -> Color.Gray
                        }
                    ) {
                        selectedMode = mode
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (imageUris.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    items(imageUris) { uri ->
                        var bitmap by remember(uri) { mutableStateOf<android.graphics.Bitmap?>(null) }
                        LaunchedEffect(uri) {
                            withContext(Dispatchers.IO) {
                                bitmap = com.funtime.sciai.data.ImageUtils.decodeSampledBitmapFromUri(context, uri, 200, 200)
                            }
                        }

                        if (bitmap != null) {
                            Box(modifier = Modifier.size(80.dp)) {
                                Image(
                                    bitmap = bitmap!!.asImageBitmap(),
                                    contentDescription = "Selected Image",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = { imageUris = imageUris.filter { it != uri } },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(24.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                        .padding(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove Image",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(if (isProcessingImage) "Processing images..." else "Ask anything...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isProcessingImage,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = { executeSearch() }
                ),
                shape = RoundedCornerShape(24.dp),
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
                        IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Images", tint = Color.Gray)
                        }
                        IconButton(onClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
                            }
                            speechLauncher.launch(intent)
                        }) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice", tint = Color.Gray)
                        }
                        IconButton(
                            onClick = { executeSearch() },
                            enabled = (query.isNotBlank() || imageUris.isNotEmpty()) && !isProcessingImage
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Search",
                                tint = if (isProcessingImage) Color.Gray else Color(0xFF38BDF8)
                            )
                        }
                    }
                }
            )
        }
    }
}

fun getBadge(count: Int): String {
    return when {
        count >= 50 -> "Expert 🧠"
        count >= 20 -> "Smart 🔥"
        count >= 10 -> "Learner 📘"
        else -> "Beginner 🌱"
    }
}

fun formatSessionTime(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "0m"
    }
}

@Composable
fun SelectableButton(
    text: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) color.copy(alpha = 0.2f) else Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isSelected) color else Color.Gray.copy(alpha = 0.5f)
        ),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = text,
                color = if (isSelected) color else Color.Gray,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                letterSpacing = 0.5.sp,
                fontSize = 14.sp
            )
        }
    }
}



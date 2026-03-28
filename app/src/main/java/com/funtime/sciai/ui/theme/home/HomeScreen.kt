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
import androidx.compose.material3.AlertDialog
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {


    var selectedDomain by remember { mutableStateOf("Biology") }
    var selectedMode by remember { mutableStateOf("Exam") }

    val context = LocalContext.current
    var query by remember { mutableStateOf("") }  // ✅ FIRST

    val activity = context as Activity
    val coroutineScope = rememberCoroutineScope()
    val userManager = UserManager(context)
    val sessionViewModel: SessionViewModel = viewModel()
    val sessionTime = sessionViewModel.sessionTime.value

    var totalCount by remember { mutableStateOf<Int>(userManager.getTotal()) }

    var imageUri by remember { mutableStateOf<Uri?>(null) }

// Gallery
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->

        if (uri != null) {
            imageUri = uri

            coroutineScope.launch {

                val result = GeminiService.analyzeImage(context, uri)

                query = result   // 🔥 directly fill answer OR navigate

                // OPTIONAL AUTO NAVIGATION
                val encodedQuery = Uri.encode(result)
                navController.navigate("answer/$encodedQuery/$selectedMode")
            }
        }
    }

// Camera
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        // You can process bitmap (later for OCR)
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?: ""

            query = spokenText
        }
    }



    var userName by remember { mutableStateOf<String?>(userManager.getName()) }
    var showDialog by remember { mutableStateOf(userManager.getName() == null) }



    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {

            ModalDrawerSheet {

                Text("SciAI", modifier = Modifier.padding(16.dp))

                Divider()

                DrawerItem("Home")
                DrawerItem("Library")
                DrawerItem("Downloads")

                Divider()

                DrawerItem("Premium")
                DrawerItem("Support")

                Divider()

                DrawerItem("Exit")
            }
        }
    ) {

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("SciAI") },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { padding ->

            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp)
            ) {

                Text("Welcome, ${userName ?: "Student"}")
                Spacer(modifier = Modifier.height(8.dp))


                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
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
                        totalCount = 0   // 🔥 THIS LINE IS CRITICAL
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text("Reset Progress")
                }



                Spacer(modifier = Modifier.height(16.dp))

                // Domain Chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Biology", "Physics", "Chemistry").forEach { domain ->
                        SelectableButton(
                            text = domain,
                            isSelected = selectedDomain == domain,
                            color = when (domain) {
                                "Biology" -> Color(0xFF4CAF50)
                                "Physics" -> Color(0xFF2196F3)
                                "Chemistry" -> Color(0xFFF44336)
                                else -> Color.Gray
                            }
                        ) {
                            selectedDomain = domain
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mode Chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Exam", "Concept", "Expert").forEach { mode ->
                        SelectableButton(
                            text = mode,
                            isSelected = selectedMode == mode,
                            color = when (mode) {
                                "Exam" -> Color(0xFFFFC107)
                                "Concept" -> Color(0xFF2196F3)
                                "Expert" -> Color(0xFF9C27B0)
                                else -> Color.Gray
                            }
                        ) {
                            selectedMode = mode
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Search Bar with Voice
                OutlinedTextField(
                    value = query,
                    onValueChange = { newText ->
                        query = newText
                    },
                    placeholder = { Text("Ask anything...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {

                        Row {

                            // ➕ IMAGE
                            IconButton(onClick = {
                                galleryLauncher.launch("image/*")
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "Add Image")
                            }

                            // 🎤 VOICE
                            IconButton(onClick = {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                                intent.putExtra(
                                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                )
                                voiceLauncher.launch(intent)
                            }) {
                                Icon(Icons.Default.Mic, contentDescription = "Voice")
                            }
                        }
                    }
                )




                Button(
                    onClick = {
                        if (query.isNotBlank()) {


                            userManager.incrementTotal()
                            totalCount = userManager.getTotal()

                            val encodedQuery = Uri.encode(query)

                            navController.navigate("answer/$encodedQuery/$selectedMode")
                        }
                    }
                ) {
                    Text("Search")
                }


            }
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
        else -> "${hours}h ${minutes}m"
    }
}

@Composable
fun DrawerItem(title: String) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    )
}
@Composable
fun SelectableButton(
    text: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Text(
        text = text,
        color = if (isSelected) Color.White else Color.Gray,
        modifier = Modifier
            .border(
                1.5.dp,
                if (isSelected) color else Color.Gray,
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() }
    )
}



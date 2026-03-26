package com.funtime.sciai.ui.theme.home
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.funtime.sciai.data.UserManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {

    var selectedDomain by remember { mutableStateOf("Biology") }
    var selectedMode by remember { mutableStateOf("Exam") }

    val context = LocalContext.current
    val userManager = UserManager(context)

    var userName by remember { mutableStateOf<String?>(userManager.getName()) }
    var showDialog by remember { mutableStateOf(userManager.getName() == null) }

    var sessionCount by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("")}

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

                Text("Session: $sessionCount")
                Text("Total: ${userManager.getTotal()}")

                Spacer(modifier = Modifier.height(16.dp))

                // Domain Chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Biology", "Physics", "Chemistry").forEach { domain ->
                        FilterChip(
                            selected = selectedDomain == domain,
                            onClick = { selectedDomain = domain },
                            label = { Text(domain) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mode Chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Exam", "Concept", "Expert").forEach { mode ->
                        FilterChip(
                            selected = selectedMode == mode,
                            onClick = { selectedMode = mode },
                            label = { Text(mode) }
                        )
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
                        IconButton(onClick = {
                            // TODO: Voice input later
                        }) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice"
                            )
                        }
                    }
                )


                Button(
                    onClick = {
                        if (query.isNotBlank()) {

                            sessionCount++
                            userManager.incrementTotal()

                            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")

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

@Composable
fun DrawerItem(title: String) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    )
}



package com.funtime.sciai.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    title: String,
    navController: NavController,
    showBack: Boolean = false,
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (Modifier) -> Unit
) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A), // Deep premium dark blue
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                navigationIcon = {
                    if (showBack) {
                        FilledIconButton(
                            onClick = { navController.popBackStack() },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color(0xFF1E293B), // Slate 800
                                contentColor = Color(0xFF38BDF8)    // Cyan accent
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go Back")
                        }
                    }
                }
            )
        },
        floatingActionButton = floatingActionButton
    ) { padding ->
        content(Modifier.padding(padding))
    }
}
package com.funtime.sciai.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    title: String,
    navController: NavController,
    showBack: Boolean = false,
    content: @Composable (Modifier) -> Unit
) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Text("<")
                        }
                    }
                }
            )
        }
    ) { padding ->
        content(Modifier.padding(padding))
    }
}
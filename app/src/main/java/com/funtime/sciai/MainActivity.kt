package com.funtime.sciai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.funtime.sciai.data.local.OfflineCache
import com.funtime.sciai.navigation.AppNav
import com.funtime.sciai.ui.theme.SciAITheme
import com.funtime.sciai.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        OfflineCache.init(applicationContext)
        
        setContent {
            val isDark = themeViewModel.isDarkMode.value
            SciAITheme(darkTheme = isDark) {
                AppNav(themeViewModel = themeViewModel)
            }
        }
    }
}
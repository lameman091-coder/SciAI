package com.funtime.sciai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.funtime.sciai.navigation.AppNav
import com.funtime.sciai.ui.theme.SciAITheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SciAITheme {
                AppNav()
            }
        }
    }
}
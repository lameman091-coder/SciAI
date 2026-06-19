package com.funtime.sciai.ui.theme

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.funtime.sciai.data.UserManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val userManager: UserManager
) : ViewModel() {

    private val _isDarkMode = mutableStateOf(userManager.isDarkMode())
    val isDarkMode: State<Boolean> = _isDarkMode

    fun toggleTheme() {
        val newValue = !_isDarkMode.value
        _isDarkMode.value = newValue
        userManager.setDarkMode(newValue)
    }
}

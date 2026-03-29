package com.funtime.sciai.ui.theme.home

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SessionViewModel : ViewModel() {

    var sessionTime = mutableStateOf(0L)
        private set

    init {
        startTimer()
    }

    private fun startTimer() {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                sessionTime.value += 1
            }
        }
    }

    fun resetTime() {
        sessionTime.value = 0L
    }
}
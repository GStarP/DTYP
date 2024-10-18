package com.example.dtyp

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class MainStore : ViewModel() {
    var isServiceRunning  = mutableStateOf(false)
}
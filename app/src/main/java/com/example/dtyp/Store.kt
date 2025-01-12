package com.example.dtyp

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class MainStore : ViewModel() {
    var serviceState  = mutableStateOf(ServiceState.OFF)
}

enum class ServiceState {
    ON, LOADING, OFF
}
package com.kuvandikov.nfc

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel: ViewModel() {
    private val _status = MutableStateFlow("")
    val status = _status.asStateFlow()

    fun updateStatus(newStatus: String) {
        _status.value = newStatus
    }
}
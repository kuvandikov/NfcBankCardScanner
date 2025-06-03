package com.kuvandikov.nfc

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel: ViewModel() {
    private val _status = MutableStateFlow("")
    val status = _status.asStateFlow()

    private val _nfcDispatch = MutableStateFlow(false)
    val nfcDispatch = _nfcDispatch.asStateFlow()

    fun updateNfcDispatch(newValue: Boolean) {
        _nfcDispatch.value = newValue
    }

    fun updateStatus(newStatus: String) {
        _status.value = newStatus
    }
}
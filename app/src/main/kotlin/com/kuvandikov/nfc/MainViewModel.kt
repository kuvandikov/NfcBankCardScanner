package com.kuvandikov.nfc

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ReadCard())
    val uiState: StateFlow<ReadCard> = _uiState.asStateFlow()

    fun readCard(number: String, expireDate: String, type: String) {
        _uiState.value = ReadCard(number, expireDate, type)
    }
}

data class ReadCard(
    val number: String = "",
    val expireDate: String = "",
    val type: String = ""
) {
    fun getCardInfo(): String = buildString {
        appendLine(if (number.isNotEmpty()) "Number: $number" else "")
        appendLine(if (expireDate.isNotEmpty()) "Expire Date: $expireDate" else "")
        appendLine(if (type.isNotEmpty()) "Type: $type" else "")
    }
}
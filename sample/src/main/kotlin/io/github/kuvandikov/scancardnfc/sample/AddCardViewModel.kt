/*
 * Copyright 2026 Kuvandikov and the scan-card-nfc contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.kuvandikov.scancardnfc.sample

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import io.github.kuvandikov.scancardnfc.model.EmvCard
import io.github.kuvandikov.scancardnfc.model.NfcErrorType
import io.github.kuvandikov.scancardnfc.model.NfcScanResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AddCardUiState(
    /** Read card. Kept in memory only; a real app sends `card.pan` to its backend and drops it. */
    val card: EmvCard? = null,
    val isScanning: Boolean = false,
    @StringRes val status: Int? = null,
    val showNfcSettingsAction: Boolean = false,
)

/**
 * Integration example for a bank app: maps [NfcScanResult] to screen state and user-facing text.
 *
 * The scan itself runs in the composition scope (see [AddCardScreen]) because the scanner is
 * bound to the activity, which a ViewModel must not hold.
 */
class AddCardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AddCardUiState())
    val uiState: StateFlow<AddCardUiState> = _uiState.asStateFlow()

    fun onScanStarted() {
        _uiState.update {
            it.copy(isScanning = true, status = R.string.status_waiting, showNfcSettingsAction = false)
        }
    }

    fun onScanCancelled() {
        _uiState.update { it.copy(isScanning = false, status = null) }
    }

    fun onScanResult(result: NfcScanResult) {
        _uiState.update { state ->
            val base = state.copy(isScanning = false, showNfcSettingsAction = false)
            when (result) {
                is NfcScanResult.Success -> base.copy(card = result.card, status = R.string.status_success)
                NfcScanResult.CardMovedTooFast -> base.copy(status = R.string.status_card_moved)
                NfcScanResult.CardBlocked -> base.copy(status = R.string.status_card_blocked)
                NfcScanResult.NfcDisabled -> base.copy(status = R.string.status_nfc_disabled, showNfcSettingsAction = true)
                NfcScanResult.NfcNotSupported -> base.copy(status = R.string.status_nfc_not_supported)
                NfcScanResult.Cancelled -> base.copy(status = null)
                is NfcScanResult.Error -> base.copy(
                    status = if (result.type == NfcErrorType.UNSUPPORTED_TAG) {
                        R.string.status_unsupported_tag
                    } else {
                        R.string.status_error
                    },
                )
            }
        }
    }

    fun onAddCard() {
        // Demo only: nothing is stored or sent. Clear the card from memory right away.
        _uiState.value = AddCardUiState(status = R.string.status_card_added)
    }

    override fun onCleared() {
        _uiState.value = AddCardUiState()
    }
}

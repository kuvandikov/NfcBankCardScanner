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

package io.github.kuvandikov.scancardnfc.compose

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.kuvandikov.scancardnfc.EmvReaderConfig
import io.github.kuvandikov.scancardnfc.NfcCardScanner
import io.github.kuvandikov.scancardnfc.model.NfcScanResult
import io.github.kuvandikov.scancardnfc.scanner.AndroidNfcScanner

/**
 * Creates and remembers an [NfcCardScanner] bound to the hosting [ComponentActivity].
 *
 * In `@Preview` (inspection mode) a stub that reports NFC as unsupported is returned.
 *
 * ```kotlin
 * val scanner = rememberNfcCardScanner()
 * val scope = rememberCoroutineScope()
 * Button(onClick = { scope.launch { onResult(scanner.scan()) } }) { Text("Scan card") }
 * ```
 */
@Composable
public fun rememberNfcCardScanner(config: EmvReaderConfig = EmvReaderConfig()): NfcCardScanner {
    if (LocalInspectionMode.current) return remember { PreviewNfcCardScanner }
    val activity = LocalContext.current.findComponentActivity()
    return remember(activity, config) { AndroidNfcScanner(activity, config) }
}

/**
 * Whether NFC is switched on. Re-checked every time the screen resumes, so the value updates
 * after the user comes back from the system settings (see `openNfcSettings()`).
 */
@Composable
public fun rememberNfcEnabledState(scanner: NfcCardScanner): State<Boolean> {
    val state = remember(scanner) { mutableStateOf(scanner.isEnabled) }
    LifecycleResumeEffect(scanner) {
        state.value = scanner.isEnabled
        onPauseOrDispose { }
    }
    return state
}

private tailrec fun Context.findComponentActivity(): ComponentActivity = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> error("rememberNfcCardScanner() must be called inside a ComponentActivity")
}

private object PreviewNfcCardScanner : NfcCardScanner {
    override val isSupported: Boolean = false
    override val isEnabled: Boolean = false
    override suspend fun scan(): NfcScanResult = NfcScanResult.NfcNotSupported
}

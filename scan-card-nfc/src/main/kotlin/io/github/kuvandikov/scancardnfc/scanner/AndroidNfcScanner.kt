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

package io.github.kuvandikov.scancardnfc.scanner

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleDestroyedException
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.withResumed
import io.github.kuvandikov.scancardnfc.EmvReader
import io.github.kuvandikov.scancardnfc.EmvReaderConfig
import io.github.kuvandikov.scancardnfc.NfcCardScanner
import io.github.kuvandikov.scancardnfc.model.NfcErrorType
import io.github.kuvandikov.scancardnfc.model.NfcScanResult
import io.github.kuvandikov.scancardnfc.provider.AndroidCardProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * [NfcCardScanner] based on [NfcAdapter.enableReaderMode] (foreground reader mode).
 *
 * Lifecycle-aware:
 * - [scan] waits until [activity] is resumed before enabling reader mode.
 * - The platform keeps reader mode across pause/resume of the same activity.
 * - Reader mode is disabled when [scan] returns or its coroutine is cancelled.
 * - If the activity is destroyed while waiting, [scan] returns [NfcScanResult.Cancelled].
 *
 * Create one instance per activity, e.g. `private val scanner by lazy { AndroidNfcScanner(this) }`,
 * or use `rememberNfcCardScanner()` from `scan-card-nfc-compose`.
 */
public class AndroidNfcScanner @JvmOverloads constructor(
    private val activity: ComponentActivity,
    private val config: EmvReaderConfig = EmvReaderConfig(),
) : NfcCardScanner {

    private val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)

    override val isSupported: Boolean get() = adapter != null

    override val isEnabled: Boolean get() = adapter?.isEnabled == true

    override suspend fun scan(): NfcScanResult {
        val adapter = adapter ?: return NfcScanResult.NfcNotSupported
        if (!adapter.isEnabled) return NfcScanResult.NfcDisabled
        val lifecycle = activity.lifecycle
        if (lifecycle.currentState == Lifecycle.State.DESTROYED) return NfcScanResult.Cancelled

        // `null` means the activity was destroyed before a card was presented.
        val detected = CompletableDeferred<Tag?>()
        val destroyObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) detected.complete(null)
        }

        try {
            lifecycle.withResumed {
                lifecycle.addObserver(destroyObserver)
                adapter.enableReaderMode(activity, { tag -> detected.complete(tag) }, READER_FLAGS, readerExtras())
            }
            val tag = detected.await() ?: return NfcScanResult.Cancelled
            val isoDep = IsoDep.get(tag)
                ?: return NfcScanResult.Error(NfcErrorType.UNSUPPORTED_TAG, "Tag does not support ISO-DEP")
            return readIsoDep(isoDep)
        } catch (e: LifecycleDestroyedException) {
            // Thrown by withResumed when the activity is destroyed before it resumes.
            return NfcScanResult.Cancelled
        } finally {
            withContext(NonCancellable + Dispatchers.Main.immediate) {
                lifecycle.removeObserver(destroyObserver)
                if (lifecycle.currentState != Lifecycle.State.DESTROYED) {
                    runCatching { adapter.disableReaderMode(activity) }
                }
            }
        }
    }

    private suspend fun readIsoDep(isoDep: IsoDep): NfcScanResult = withContext(Dispatchers.IO) {
        try {
            isoDep.connect()
            isoDep.timeout = config.transceiveTimeoutMillis
            EmvReader(AndroidCardProvider(isoDep), config).read()
        } catch (e: IOException) {
            NfcScanResult.CardMovedTooFast
        } finally {
            runCatching { isoDep.close() }
        }
    }

    private fun readerExtras() = Bundle().apply {
        putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, PRESENCE_CHECK_DELAY_MS)
    }

    private companion object {
        const val READER_FLAGS = NfcAdapter.FLAG_READER_NFC_A or
            NfcAdapter.FLAG_READER_NFC_B or
            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK

        const val PRESENCE_CHECK_DELAY_MS = 250
    }
}

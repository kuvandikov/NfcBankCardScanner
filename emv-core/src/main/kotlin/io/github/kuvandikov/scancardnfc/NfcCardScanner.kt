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

package io.github.kuvandikov.scancardnfc

import io.github.kuvandikov.scancardnfc.model.NfcScanResult

/**
 * High-level scanner: waits for a card, reads it and returns the result.
 *
 * The production implementation is `AndroidNfcScanner` in the `scan-card-nfc` artifact.
 * Depend on this interface in ViewModels and replace it with a fake in tests:
 *
 * ```kotlin
 * class FakeScanner(var next: NfcScanResult) : NfcCardScanner {
 *     override val isSupported = true
 *     override val isEnabled = true
 *     override suspend fun scan() = next
 * }
 * ```
 */
public interface NfcCardScanner {
    /** `true` when the device has NFC hardware. */
    public val isSupported: Boolean

    /** `true` when NFC is supported and switched on in system settings. */
    public val isEnabled: Boolean

    /**
     * Suspends until a card is read, an error occurs, or the coroutine is cancelled.
     * Cancelling the coroutine stops the NFC session.
     */
    public suspend fun scan(): NfcScanResult
}

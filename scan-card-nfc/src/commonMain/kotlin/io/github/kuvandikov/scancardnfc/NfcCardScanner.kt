package io.github.kuvandikov.scancardnfc

import io.github.kuvandikov.scancardnfc.model.NfcScanResult

/**
 * High-level scanner: waits for a card, reads it and returns the result.
 *
 * Implementations:
 * - Android: `AndroidNfcScanner(activity)` — uses `NfcAdapter` reader mode.
 * - iOS: `IosNfcScanner()` — uses `NFCTagReaderSession` and shows the system scan sheet.
 *
 * Keep the shared UI code against this interface; create the platform instance at the edge.
 */
public interface NfcCardScanner {
    /** `true` when the device has NFC hardware that can read ISO-DEP cards. */
    public val isSupported: Boolean

    /** `true` when NFC is supported and switched on. */
    public val isEnabled: Boolean

    /**
     * Suspends until a card is read, an error occurs, or the coroutine is cancelled.
     * Cancelling the coroutine stops the NFC session.
     */
    public suspend fun scan(): NfcScanResult
}

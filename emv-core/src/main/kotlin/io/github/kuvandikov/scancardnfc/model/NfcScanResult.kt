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

package io.github.kuvandikov.scancardnfc.model

/** Outcome of a single scan. */
public sealed interface NfcScanResult {

    public data class Success(val card: EmvCard) : NfcScanResult

    /** The card left the field before reading finished. Ask the user to hold it still. */
    public data object CardMovedTooFast : NfcScanResult

    /** The payment application on the card is blocked (SW `6A81` / `6283`). */
    public data object CardBlocked : NfcScanResult

    /** NFC is present but switched off in system settings. */
    public data object NfcDisabled : NfcScanResult

    /** The device has no NFC hardware. */
    public data object NfcNotSupported : NfcScanResult

    /** The scan was cancelled, e.g. the screen was closed. */
    public data object Cancelled : NfcScanResult

    public data class Error(val type: NfcErrorType, val message: String? = null) : NfcScanResult
}

public enum class NfcErrorType {
    /** The tag is not an ISO 14443-4 (ISO-DEP) smart card, e.g. a transport card or a sticker. */
    UNSUPPORTED_TAG,

    /** No payment application was found on the card. */
    NO_PAYMENT_APPLICATION,

    /** A payment application was selected but no card number could be read. */
    CARD_DATA_NOT_FOUND,

    /** The card answered with an unexpected status word. */
    COMMAND_FAILED,

    /** The card response could not be parsed. */
    PARSING_FAILED,

    UNKNOWN,
}

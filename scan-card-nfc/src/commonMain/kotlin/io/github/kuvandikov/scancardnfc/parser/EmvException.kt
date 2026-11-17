package io.github.kuvandikov.scancardnfc.parser

import io.github.kuvandikov.scancardnfc.model.NfcErrorType

/** Internal failure of the EMV flow. Mapped to [io.github.kuvandikov.scancardnfc.model.NfcScanResult.Error]. */
internal class EmvException(val type: NfcErrorType, message: String) : Exception(message) {
    companion object {
        fun parsing(message: String) = EmvException(NfcErrorType.PARSING_FAILED, message)
    }
}

/** The card, or every payment application on it, is blocked. Mapped to [io.github.kuvandikov.scancardnfc.model.NfcScanResult.CardBlocked]. */
internal class CardBlockedException(message: String) : Exception(message)

package io.github.kuvandikov.scancardnfc.model

/** Outcome of a single scan. */
public sealed interface NfcScanResult {

    public data class Success(val card: EmvCard) : NfcScanResult

    /** The card left the field before reading finished. Ask the user to hold it still. */
    public data object CardMovedTooFast : NfcScanResult

    /**
     * The card or all of its payment applications are blocked by the issuer
     * (SW `6A81` to SELECT PPSE, or `6283` / `6A81` to SELECT AID). Retrying will not help.
     */
    public data object CardBlocked : NfcScanResult

    /** The device has no NFC hardware (or, on iOS, no NFC tag reading support). */
    public data object NfcNotSupported : NfcScanResult

    /** NFC is present but switched off in system settings (Android only). */
    public data object NfcDisabled : NfcScanResult

    /** The user dismissed the system scan sheet (iOS) or the scan was cancelled. */
    public data object Cancelled : NfcScanResult

    public data class Error(val type: NfcErrorType, val message: String? = null) : NfcScanResult
}

public enum class NfcErrorType {
    /** The tag is not an ISO 14443-4 (ISO-DEP) smart card. */
    UNSUPPORTED_TAG,

    /** No payment application was found on the card. */
    NO_PAYMENT_APPLICATION,

    /** A payment application was selected but no PAN could be read. */
    CARD_DATA_NOT_FOUND,

    /** The card answered with an unexpected status word. */
    COMMAND_FAILED,

    /** The card response could not be parsed. */
    PARSING_FAILED,

    /** The platform NFC session timed out. */
    TIMEOUT,

    UNKNOWN,
}

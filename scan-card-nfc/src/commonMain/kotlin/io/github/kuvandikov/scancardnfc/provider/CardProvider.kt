package io.github.kuvandikov.scancardnfc.provider

/**
 * Platform transport that exchanges raw APDUs with a connected card.
 *
 * Implementations: `AndroidCardProvider` (IsoDep), `IosCardProvider` (NFCISO7816Tag).
 * You can write your own, e.g. for a PC/SC reader on desktop, and pass it to `EmvReader`.
 */
public fun interface CardProvider {
    /**
     * Sends a command APDU and returns the full response APDU, **including** the trailing
     * status word (SW1 SW2).
     *
     * @throws CardConnectionLostException when the card leaves the field.
     */
    public suspend fun transceive(command: ByteArray): ByteArray
}

/** Thrown by a [CardProvider] when the connection to the card is lost mid-transaction. */
public class CardConnectionLostException(cause: Throwable? = null) :
    Exception("Connection to the card was lost", cause)

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

package io.github.kuvandikov.scancardnfc.provider

/**
 * Platform transport that exchanges raw APDUs with a connected card.
 *
 * The Android implementation is `AndroidCardProvider` (IsoDep) in `scan-card-nfc`.
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

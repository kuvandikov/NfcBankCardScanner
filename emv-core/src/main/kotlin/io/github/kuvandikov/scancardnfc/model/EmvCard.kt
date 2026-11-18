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

/**
 * Public data read from the contactless interface of a payment card.
 *
 * Only data that the card exposes without cardholder verification is available:
 * CVV2/CVC2 and the PIN never leave the chip.
 *
 * [toString] masks the PAN so that card numbers are not leaked into logs by accident.
 * This is a regular class rather than a `data class` to keep `copy`/`componentN` out of the
 * public API, so fields can be added in later versions without breaking binary compatibility.
 */
public class EmvCard(
    /** Primary Account Number, digits only. Never log it; use [maskedPan]. */
    public val pan: String,
    public val expiry: CardExpiry?,
    public val scheme: EmvCardScheme,
    /** Cardholder name (tag 5F20). Most modern cards return an empty or generic value. */
    public val holderName: String?,
    /** AID of the application that was read, as hex. */
    public val aid: String? = null,
    /** Application label (tag 50) or preferred name (tag 9F12), e.g. "VISA DEBIT". */
    public val applicationLabel: String? = null,
) {
    /** PAN with all but the first 6 and last 4 digits hidden: `4111 11•• •••• 1111`. */
    public val maskedPan: String
        get() = pan.mapIndexed { i, c -> if (i < 6 || i >= pan.length - 4) c else '•' }
            .chunked(4) { it.joinToString("") }
            .joinToString(" ")

    /** PAN grouped by 4 digits for input fields: `4111 1111 1111 1111`. */
    public val formattedPan: String
        get() = pan.chunked(4).joinToString(" ")

    /** Result of the Luhn checksum on [pan]. */
    public val isPanValid: Boolean
        get() = luhnCheck(pan)

    override fun equals(other: Any?): Boolean = other is EmvCard &&
        pan == other.pan &&
        expiry == other.expiry &&
        scheme == other.scheme &&
        holderName == other.holderName &&
        aid == other.aid &&
        applicationLabel == other.applicationLabel

    override fun hashCode(): Int {
        var result = pan.hashCode()
        result = 31 * result + (expiry?.hashCode() ?: 0)
        result = 31 * result + scheme.hashCode()
        result = 31 * result + (holderName?.hashCode() ?: 0)
        result = 31 * result + (aid?.hashCode() ?: 0)
        result = 31 * result + (applicationLabel?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String = "EmvCard(pan=$maskedPan, expiry=$expiry, scheme=$scheme)"
}

/**
 * Card expiry date. [year] is the full year, e.g. 2029.
 *
 * Not `java.time.YearMonth`: that needs API 26 (or desugaring in the consuming app).
 */
public class CardExpiry(public val month: Int, public val year: Int) {
    init {
        require(month in 1..12) { "Invalid month: $month" }
    }

    override fun equals(other: Any?): Boolean = other is CardExpiry && month == other.month && year == other.year

    override fun hashCode(): Int = 31 * month + year

    /** `MM/YY`, as printed on the card. */
    override fun toString(): String =
        "${month.toString().padStart(2, '0')}/${(year % 100).toString().padStart(2, '0')}"
}

internal fun luhnCheck(number: String): Boolean {
    if (number.length < 12 || number.any { !it.isDigit() }) return false
    var sum = 0
    number.reversed().forEachIndexed { index, c ->
        var d = c.digitToInt()
        if (index % 2 == 1) {
            d *= 2
            if (d > 9) d -= 9
        }
        sum += d
    }
    return sum % 10 == 0
}

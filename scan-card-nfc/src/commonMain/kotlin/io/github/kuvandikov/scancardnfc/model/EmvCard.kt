package io.github.kuvandikov.scancardnfc.model

/**
 * Public data read from the contactless interface of a payment card.
 *
 * Only data that the card exposes without cardholder verification is available:
 * CVV2/CVC2 and the PIN never leave the chip.
 *
 * [toString] masks the PAN so that cards are not leaked into logs by accident.
 */
public data class EmvCard(
    /** Primary Account Number, digits only. */
    val pan: String,
    val expiry: CardExpiry?,
    val scheme: EmvCardScheme,
    /** Cardholder name (tag 5F20). Most modern cards return an empty or generic value. */
    val holderName: String?,
    /** AID of the application that was read, as hex. */
    val aid: String?,
    /** Application label (tag 50) or preferred name (tag 9F12), e.g. "VISA DEBIT". */
    val applicationLabel: String?,
) {
    /** PAN with all but the first 6 and last 4 digits hidden: `4111 11•• •••• 1111`. */
    val maskedPan: String
        get() = pan.mapIndexed { i, c -> if (i < 6 || i >= pan.length - 4) c else '•' }
            .chunked(4) { it.joinToString("") }
            .joinToString(" ")

    /** Result of the Luhn checksum on [pan]. */
    val isPanValid: Boolean
        get() = luhnCheck(pan)

    override fun toString(): String =
        "EmvCard(pan=$maskedPan, expiry=$expiry, scheme=$scheme, aid=$aid, label=$applicationLabel)"
}

/** Card expiry date. [year] is the full year, e.g. 2029. */
public data class CardExpiry(val month: Int, val year: Int) {
    init {
        require(month in 1..12) { "Invalid month: $month" }
    }

    /** `MM/YY` */
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

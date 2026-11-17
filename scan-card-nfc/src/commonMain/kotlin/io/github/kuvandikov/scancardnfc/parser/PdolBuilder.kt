package io.github.kuvandikov.scancardnfc.parser

import io.github.kuvandikov.scancardnfc.EmvReaderConfig
import io.github.kuvandikov.scancardnfc.util.currentDate
import io.github.kuvandikov.scancardnfc.util.hexToBytes
import kotlin.random.Random

/**
 * Builds the terminal data requested by the card's PDOL (tag 9F38).
 *
 * The values describe a zero-risk "online-only contactless terminal". No transaction is
 * authorised: the card is only asked to reveal its public records.
 */
internal class PdolBuilder(
    private val config: EmvReaderConfig,
    private val random: Random = Random.Default,
) {

    fun build(pdol: List<DolEntry>): ByteArray {
        val out = ArrayList<Byte>()
        for (entry in pdol) {
            val value = valueFor(entry.tag, entry.length)
            out += fit(value, entry.length).toList()
        }
        return out.toByteArray()
    }

    private fun valueFor(tag: Int, length: Int): ByteArray = when (tag) {
        // Terminal Transaction Qualifiers: contactless qVSDC + EMV, online capable.
        EmvTags.TTQ -> "F0204000".hexToBytes()
        EmvTags.AMOUNT_AUTHORISED -> "000000000100".hexToBytes()
        EmvTags.AMOUNT_OTHER -> ByteArray(6)
        EmvTags.TERMINAL_COUNTRY_CODE -> numericBcd(config.terminalCountryCode, 2)
        EmvTags.TRANSACTION_CURRENCY_CODE -> numericBcd(config.transactionCurrencyCode, 2)
        EmvTags.TVR -> ByteArray(5)
        EmvTags.TRANSACTION_DATE -> currentDate().let { d ->
            numericBcd(d.year % 100, 1) + numericBcd(d.month, 1) + numericBcd(d.day, 1)
        }
        EmvTags.TRANSACTION_TYPE -> byteArrayOf(0x00) // goods and services
        EmvTags.UNPREDICTABLE_NUMBER -> random.nextBytes(length.coerceAtLeast(4))
        EmvTags.TERMINAL_TYPE -> byteArrayOf(0x22) // attended, offline with online capability
        EmvTags.TERMINAL_CAPABILITIES -> "E0F0C8".hexToBytes()
        EmvTags.ADDITIONAL_TERMINAL_CAPABILITIES -> "6000F0A001".hexToBytes()
        else -> ByteArray(length)
    }

    /** Left-pads with zeros or keeps the rightmost bytes, so numeric values keep their meaning. */
    private fun fit(value: ByteArray, length: Int): ByteArray = when {
        value.size == length -> value
        value.size > length -> value.copyOfRange(value.size - length, value.size)
        else -> ByteArray(length - value.size) + value
    }

    private fun numericBcd(number: Int, bytes: Int): ByteArray {
        val digits = number.toString().padStart(bytes * 2, '0').takeLast(bytes * 2)
        return digits.hexToBytes()
    }
}

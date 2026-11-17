package io.github.kuvandikov.scancardnfc.parser

import io.github.kuvandikov.scancardnfc.util.toUnsigned

/**
 * BER-TLV node as used by EMV (ISO 7816-4 / ASN.1 subset).
 *
 * @property tag tag number with all tag bytes packed big-endian, e.g. `0x9F38`.
 */
internal class Tlv(
    val tag: Int,
    val value: ByteArray,
    val children: List<Tlv>,
) {
    /** Depth-first search of this node and its descendants. */
    fun find(tag: Int): Tlv? {
        if (this.tag == tag) return this
        for (child in children) child.find(tag)?.let { return it }
        return null
    }

    fun findAll(tag: Int): List<Tlv> = buildList { collect(tag, this) }

    private fun collect(tag: Int, out: MutableList<Tlv>) {
        if (this.tag == tag) out += this
        children.forEach { it.collect(tag, out) }
    }

    override fun toString(): String = "Tlv(${tag.toString(16).uppercase()}, ${value.size} bytes)"
}

internal fun List<Tlv>.find(tag: Int): Tlv? = firstNotNullOfOrNull { it.find(tag) }

internal fun List<Tlv>.findAll(tag: Int): List<Tlv> = flatMap { it.findAll(tag) }

/** Entry of a Data Object List (PDOL, CDOL...): tag and the length the card expects. */
internal data class DolEntry(val tag: Int, val length: Int)

internal object TlvParser {

    /** Parses a sequence of BER-TLV objects. Constructed objects are parsed recursively. */
    fun parse(bytes: ByteArray): List<Tlv> = parse(bytes, 0, bytes.size)

    private fun parse(bytes: ByteArray, start: Int, end: Int): List<Tlv> {
        val result = mutableListOf<Tlv>()
        var pos = start
        while (pos < end) {
            // EMV allows 00 / FF padding between objects.
            val first = bytes[pos].toUnsigned()
            if (first == 0x00 || first == 0xFF) {
                pos++
                continue
            }

            val (tag, afterTag) = readTag(bytes, pos, end)
            val (length, afterLength) = readLength(bytes, afterTag, end)
            val valueEnd = afterLength + length
            if (valueEnd > end) throw EmvException.parsing("TLV length $length exceeds buffer at offset $pos")

            val value = bytes.copyOfRange(afterLength, valueEnd)
            val constructed = first and 0x20 != 0
            val children = if (constructed) parse(bytes, afterLength, valueEnd) else emptyList()
            result += Tlv(tag, value, children)
            pos = valueEnd
        }
        return result
    }

    /** Parses a Data Object List: concatenated tag + length pairs without values. */
    fun parseDol(bytes: ByteArray): List<DolEntry> {
        val result = mutableListOf<DolEntry>()
        var pos = 0
        while (pos < bytes.size) {
            val (tag, afterTag) = readTag(bytes, pos, bytes.size)
            val (length, afterLength) = readLength(bytes, afterTag, bytes.size)
            result += DolEntry(tag, length)
            pos = afterLength
        }
        return result
    }

    private fun readTag(bytes: ByteArray, start: Int, end: Int): Pair<Int, Int> {
        var pos = start
        var tag = bytes[pos++].toUnsigned()
        if (tag and 0x1F == 0x1F) {
            // Subsequent tag bytes: bit 8 set means "another byte follows".
            do {
                if (pos >= end) throw EmvException.parsing("Truncated tag at offset $start")
                val b = bytes[pos++].toUnsigned()
                tag = (tag shl 8) or b
                if (pos - start > 4) throw EmvException.parsing("Tag longer than 4 bytes at offset $start")
            } while (b and 0x80 != 0)
        }
        return tag to pos
    }

    private fun readLength(bytes: ByteArray, start: Int, end: Int): Pair<Int, Int> {
        if (start >= end) throw EmvException.parsing("Missing length at offset $start")
        val first = bytes[start].toUnsigned()
        if (first < 0x80) return first to start + 1

        val count = first and 0x7F
        if (count == 0 || count > 3 || start + 1 + count > end) {
            throw EmvException.parsing("Invalid length encoding at offset $start")
        }
        var length = 0
        for (i in 1..count) length = (length shl 8) or bytes[start + i].toUnsigned()
        return length to start + 1 + count
    }
}

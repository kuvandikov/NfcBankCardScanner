package io.github.kuvandikov.scancardnfc.util

private const val HEX_DIGITS = "0123456789ABCDEF"

/** Uppercase hex representation, e.g. `byteArrayOf(0x6F, 0x1A)` → `"6F1A"`. */
public fun ByteArray.toHex(separator: String = ""): String = buildString(size * (2 + separator.length)) {
    this@toHex.forEachIndexed { index, byte ->
        if (index > 0) append(separator)
        val v = byte.toInt() and 0xFF
        append(HEX_DIGITS[v ushr 4])
        append(HEX_DIGITS[v and 0x0F])
    }
}

/** Parses a hex string (whitespace is ignored) into bytes. */
public fun String.hexToBytes(): ByteArray {
    val clean = filterNot { it.isWhitespace() }
    require(clean.length % 2 == 0) { "Hex string must have an even length: $this" }
    return ByteArray(clean.length / 2) { i ->
        ((clean[2 * i].digitToInt(16) shl 4) or clean[2 * i + 1].digitToInt(16)).toByte()
    }
}

internal fun Byte.toUnsigned(): Int = toInt() and 0xFF

/** Overwrites sensitive buffers once they are no longer needed. */
internal fun ByteArray.wipe() {
    fill(0)
}

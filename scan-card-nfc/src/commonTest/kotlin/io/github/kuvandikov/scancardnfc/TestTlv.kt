package io.github.kuvandikov.scancardnfc

import io.github.kuvandikov.scancardnfc.provider.CardProvider
import io.github.kuvandikov.scancardnfc.util.hexToBytes
import io.github.kuvandikov.scancardnfc.util.toHex

/** Builds a BER-TLV object from a hex tag and value, so test fixtures never get lengths wrong. */
internal fun tlv(tag: String, value: ByteArray): ByteArray {
    val length = when {
        value.size < 0x80 -> byteArrayOf(value.size.toByte())
        value.size <= 0xFF -> byteArrayOf(0x81.toByte(), value.size.toByte())
        else -> byteArrayOf(0x82.toByte(), (value.size shr 8).toByte(), value.size.toByte())
    }
    return tag.hexToBytes() + length + value
}

internal fun tlv(tag: String, hexValue: String): ByteArray = tlv(tag, hexValue.hexToBytes())

internal fun tlv(tag: String, vararg children: ByteArray): ByteArray =
    tlv(tag, children.fold(ByteArray(0)) { acc, b -> acc + b })

internal fun ascii(text: String): ByteArray = text.encodeToByteArray()

internal val SW_OK: ByteArray = "9000".hexToBytes()

/**
 * Scripted card: maps command-hex prefixes to responses.
 * Unknown commands answer 6A82 (file not found).
 */
internal class FakeCardProvider(
    private val script: List<Pair<String, ByteArray>>,
) : CardProvider {
    val sent = mutableListOf<String>()

    override suspend fun transceive(command: ByteArray): ByteArray {
        val hex = command.toHex()
        sent += hex
        return script.firstOrNull { (prefix, _) -> hex.startsWith(prefix) }?.second
            ?: "6A82".hexToBytes()
    }
}

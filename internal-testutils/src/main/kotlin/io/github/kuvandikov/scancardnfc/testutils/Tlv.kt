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

package io.github.kuvandikov.scancardnfc.testutils

import io.github.kuvandikov.scancardnfc.util.hexToBytes

/** Builds a BER-TLV object from a hex tag and value, so fixtures never get lengths wrong. */
fun tlv(tag: String, value: ByteArray): ByteArray {
    val length = when {
        value.size < 0x80 -> byteArrayOf(value.size.toByte())
        value.size <= 0xFF -> byteArrayOf(0x81.toByte(), value.size.toByte())
        else -> byteArrayOf(0x82.toByte(), (value.size shr 8).toByte(), value.size.toByte())
    }
    return tag.hexToBytes() + length + value
}

fun tlv(tag: String, hexValue: String): ByteArray = tlv(tag, hexValue.hexToBytes())

fun tlv(tag: String, vararg children: ByteArray): ByteArray =
    tlv(tag, children.fold(ByteArray(0)) { acc, b -> acc + b })

fun ascii(text: String): ByteArray = text.encodeToByteArray()

/** Status word 9000 (success). */
val SW_OK: ByteArray = "9000".hexToBytes()

fun sw(hex: String): ByteArray = hex.hexToBytes()

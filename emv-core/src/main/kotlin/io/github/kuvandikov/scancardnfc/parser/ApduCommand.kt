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

package io.github.kuvandikov.scancardnfc.parser

import io.github.kuvandikov.scancardnfc.util.hexToBytes
import io.github.kuvandikov.scancardnfc.util.toUnsigned

/**
 * Short ISO 7816-4 command APDU.
 *
 * @param le expected response length (1..256, where 256 is encoded as `00`), or `null` to omit Le.
 */
internal class ApduCommand(
    val cla: Int,
    val ins: Int,
    val p1: Int,
    val p2: Int,
    val data: ByteArray? = null,
    val le: Int? = 256,
) {
    init {
        require(data == null || data.size in 1..255) { "Short APDU data must be 1..255 bytes" }
        require(le == null || le in 1..256) { "Le must be 1..256" }
    }

    fun withLe(newLe: Int): ApduCommand = ApduCommand(cla, ins, p1, p2, data, if (newLe == 0) 256 else newLe)

    fun toByteArray(): ByteArray {
        val header = byteArrayOf(cla.toByte(), ins.toByte(), p1.toByte(), p2.toByte())
        val body = data?.let { byteArrayOf(it.size.toByte()) + it } ?: ByteArray(0)
        val trailer = le?.let { byteArrayOf((it and 0xFF).toByte()) } ?: ByteArray(0)
        return header + body + trailer
    }
}

/** Factory for the EMV commands used during a contactless read. */
internal object ApduCommands {
    /** "2PAY.SYS.DDF01" — Proximity Payment System Environment. */
    val PPSE_NAME: ByteArray = "325041592E5359532E4444463031".hexToBytes()

    fun select(name: ByteArray): ApduCommand = ApduCommand(0x00, 0xA4, 0x04, 0x00, name)

    fun selectPpse(): ApduCommand = select(PPSE_NAME)

    /** GET PROCESSING OPTIONS with the PDOL data wrapped in tag 83. */
    fun getProcessingOptions(pdolData: ByteArray): ApduCommand {
        val template = byteArrayOf(0x83.toByte(), pdolData.size.toByte()) + pdolData
        return ApduCommand(0x80, 0xA8, 0x00, 0x00, template)
    }

    fun readRecord(sfi: Int, record: Int): ApduCommand =
        ApduCommand(0x00, 0xB2, record, (sfi shl 3) or 0x04)

    fun getResponse(length: Int): ApduCommand = ApduCommand(0x00, 0xC0, 0x00, 0x00, le = if (length == 0) 256 else length)
}

/** Response APDU split into data and status word. */
internal class ApduResponse(val data: ByteArray, val sw1: Int, val sw2: Int) {
    val sw: Int get() = (sw1 shl 8) or sw2
    val isSuccess: Boolean get() = sw == 0x9000

    companion object {
        fun parse(raw: ByteArray): ApduResponse {
            if (raw.size < 2) throw EmvException.parsing("Response shorter than a status word")
            return ApduResponse(
                data = raw.copyOfRange(0, raw.size - 2),
                sw1 = raw[raw.size - 2].toUnsigned(),
                sw2 = raw[raw.size - 1].toUnsigned(),
            )
        }
    }
}

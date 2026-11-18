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
import io.github.kuvandikov.scancardnfc.util.toHex
import kotlin.test.Test
import kotlin.test.assertEquals

class ApduCommandTest {

    @Test
    fun selectPpse() {
        assertEquals("00A404000E325041592E5359532E444446303100", ApduCommands.selectPpse().toByteArray().toHex())
    }

    @Test
    fun selectAid() {
        assertEquals(
            "00A4040007A000000003101000",
            ApduCommands.select("A0000000031010".hexToBytes()).toByteArray().toHex(),
        )
    }

    @Test
    fun getProcessingOptionsWrapsPdolInTag83() {
        // Empty PDOL → command data "83 00"
        assertEquals("80A8000002830000", ApduCommands.getProcessingOptions(ByteArray(0)).toByteArray().toHex())
        assertEquals("80A80000048302112200", ApduCommands.getProcessingOptions("1122".hexToBytes()).toByteArray().toHex())
    }

    @Test
    fun readRecordEncodesSfiInP2() {
        // SFI 1, record 2 → P2 = (1 << 3) | 4 = 0x0C
        assertEquals("00B2020C00", ApduCommands.readRecord(sfi = 1, record = 2).toByteArray().toHex())
    }

    @Test
    fun responseSplitsStatusWord() {
        val response = ApduResponse.parse("6F009000".hexToBytes())
        assertEquals("6F00", response.data.toHex())
        assertEquals(0x9000, response.sw)
    }
}

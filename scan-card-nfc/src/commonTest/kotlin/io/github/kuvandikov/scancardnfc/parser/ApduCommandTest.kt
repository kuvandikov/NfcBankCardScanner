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

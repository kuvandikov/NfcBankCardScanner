package io.github.kuvandikov.scancardnfc.parser

import io.github.kuvandikov.scancardnfc.util.hexToBytes
import io.github.kuvandikov.scancardnfc.util.toHex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class TlvParserTest {

    @Test
    fun parsesNestedPpseResponse() {
        // 6F → 84 (DF name), A5 → BF0C → 61 → 4F (AID), 50 (label), 87 (priority)
        val bytes = ("6F2F840E325041592E5359532E4444463031A51DBF0C1A61184F07A0000000031010" +
            "500A56495341204445424954870101").hexToBytes()

        val root = TlvParser.parse(bytes).single()
        assertEquals(0x6F, root.tag)
        assertEquals("A0000000031010", root.find(EmvTags.AID)!!.value.toHex())
        assertEquals("VISA DEBIT", root.find(EmvTags.APPLICATION_LABEL)!!.value.decodeToString())
        assertNotNull(root.find(EmvTags.FCI_ISSUER_DISCRETIONARY))
    }

    @Test
    fun parsesMultiByteTagsAndLongLengths() {
        val value = ByteArray(200) { 0x11 }
        val bytes = "9F38".hexToBytes() + byteArrayOf(0x81.toByte(), 200.toByte()) + value

        val node = TlvParser.parse(bytes).single()
        assertEquals(0x9F38, node.tag)
        assertEquals(200, node.value.size)
    }

    @Test
    fun skipsPaddingBytes() {
        val nodes = TlvParser.parse("00005A0841111111111111110000".hexToBytes())
        assertEquals("4111111111111111", nodes.single().value.toHex())
    }

    @Test
    fun parsesDataObjectList() {
        val dol = TlvParser.parseDol("9F66049F02069F37045F2A02".hexToBytes())
        assertEquals(
            listOf(DolEntry(0x9F66, 4), DolEntry(0x9F02, 6), DolEntry(0x9F37, 4), DolEntry(0x5F2A, 2)),
            dol,
        )
    }

    @Test
    fun rejectsLengthBeyondBuffer() {
        assertFailsWith<EmvException> { TlvParser.parse("5A10411111".hexToBytes()) }
    }
}

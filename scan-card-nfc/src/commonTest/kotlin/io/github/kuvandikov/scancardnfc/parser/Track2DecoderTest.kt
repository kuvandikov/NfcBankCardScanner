package io.github.kuvandikov.scancardnfc.parser

import io.github.kuvandikov.scancardnfc.model.CardExpiry
import io.github.kuvandikov.scancardnfc.util.hexToBytes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class Track2DecoderTest {

    @Test
    fun decodesPanAndExpiry() {
        val track2 = Track2Decoder.decode("4111111111111111D29122010000000000000F".hexToBytes())!!
        assertEquals("4111111111111111", track2.pan)
        assertEquals(CardExpiry(month = 12, year = 2029), track2.expiry)
    }

    @Test
    fun rejectsDataWithoutSeparator() {
        assertNull(Track2Decoder.decode("41111111111111112912".hexToBytes()))
    }

    @Test
    fun rejectsInvalidMonth() {
        assertNull(Track2Decoder.parseYyMm("2913"))
    }
}

package io.github.kuvandikov.scancardnfc.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EmvCardSchemeTest {

    @Test
    fun detectsByAid() {
        assertEquals(EmvCardScheme.VISA, EmvCardScheme.detect(pan = null, aid = "A0000000031010"))
        assertEquals(EmvCardScheme.MIR, EmvCardScheme.detect(pan = null, aid = "A0000006581010"))
    }

    @Test
    fun maestroAidWinsOverMastercardPrefix() {
        assertEquals(EmvCardScheme.MAESTRO, EmvCardScheme.fromAid("A0000000043060"))
        assertEquals(EmvCardScheme.MASTERCARD, EmvCardScheme.fromAid("A0000000041010"))
    }

    @Test
    fun domesticBinWinsOverCoBadgedAid() {
        // Humo card co-badged with a Visa application
        assertEquals(EmvCardScheme.HUMO, EmvCardScheme.detect(pan = "9860123412341234", aid = "A0000000031010"))
        assertEquals(EmvCardScheme.UZCARD, EmvCardScheme.detect(pan = "8600123412341234", aid = null))
    }

    @Test
    fun fallsBackToBin() {
        assertEquals(EmvCardScheme.MASTERCARD, EmvCardScheme.detect(pan = "5555555555554444", aid = null))
        assertEquals(EmvCardScheme.MASTERCARD, EmvCardScheme.detect(pan = "2221000000000009", aid = null))
        assertEquals(EmvCardScheme.UNKNOWN, EmvCardScheme.detect(pan = "1234567890123456", aid = null))
    }

    @Test
    fun maskedPanAndLuhn() {
        val card = EmvCard("4111111111111111", null, EmvCardScheme.VISA, null, null, null)
        assertEquals("4111 11•• •••• 1111", card.maskedPan)
        assertTrue(card.isPanValid)
        assertFalse(card.toString().contains("4111111111111111"))
        assertFalse(luhnCheck("4111111111111112"))
    }
}

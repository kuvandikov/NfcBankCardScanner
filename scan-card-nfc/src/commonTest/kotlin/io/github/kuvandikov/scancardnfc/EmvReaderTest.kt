package io.github.kuvandikov.scancardnfc

import io.github.kuvandikov.scancardnfc.model.CardExpiry
import io.github.kuvandikov.scancardnfc.model.EmvCardScheme
import io.github.kuvandikov.scancardnfc.model.NfcErrorType
import io.github.kuvandikov.scancardnfc.model.NfcScanResult
import io.github.kuvandikov.scancardnfc.provider.CardConnectionLostException
import io.github.kuvandikov.scancardnfc.provider.CardProvider
import io.github.kuvandikov.scancardnfc.util.hexToBytes
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class EmvReaderTest {

    private val visaAid = "A0000000031010"

    private val ppseResponse = tlv(
        "6F",
        tlv("84", ascii("2PAY.SYS.DDF01")),
        tlv("A5", tlv("BF0C", tlv("61", tlv("4F", visaAid), tlv("50", ascii("VISA DEBIT")), tlv("87", "01")))),
    ) + SW_OK

    private val selectVisaResponse = tlv(
        "6F",
        tlv("84", visaAid),
        tlv("A5", tlv("50", ascii("VISA DEBIT")), tlv("9F38", "9F66049F02069F37045F2A029A03")),
    ) + SW_OK

    @Test
    fun readsVisaCardThroughAfl() = runTest {
        val provider = FakeCardProvider(
            listOf(
                "00A404000E325041592E" to ppseResponse,
                "00A4040007$visaAid" to selectVisaResponse,
                "80A80000" to tlv("77", tlv("82", "2000"), tlv("94", "08010100")) + SW_OK,
                "00B2010C" to tlv(
                    "70",
                    tlv("57", "4111111111111111D29122010000000000000F"),
                    tlv("5F20", ascii("IVANOV/IVAN ")),
                ) + SW_OK,
            ),
        )

        val result = EmvReader(provider).read()

        val card = assertIs<NfcScanResult.Success>(result).card
        assertEquals("4111111111111111", card.pan)
        assertEquals(CardExpiry(12, 2029), card.expiry)
        assertEquals(EmvCardScheme.VISA, card.scheme)
        assertEquals("IVANOV/IVAN", card.holderName)
        assertEquals("VISA DEBIT", card.applicationLabel)

        // GPO must carry exactly the PDOL lengths: 4 + 6 + 4 + 2 + 3 = 19 bytes inside tag 83.
        val gpo = provider.sent.first { it.startsWith("80A8") }
        assertTrue(gpo.startsWith("80A80000158313"), gpo)
        // Country / currency default to Uzbekistan (0860) at the 5F2A position.
        assertEquals("0860", gpo.substring(14 + 2 * 14, 14 + 2 * 16))
    }

    @Test
    fun readsPanFromTag5AWithFormat1Gpo() = runTest {
        val humoPan = "9860123412341234"
        val provider = FakeCardProvider(
            listOf(
                "00A404000E" to "6A82".hexToBytes(), // no PPSE → fall back to known AIDs
                "00A4040007$visaAid" to tlv("6F", tlv("84", visaAid)) + SW_OK,
                "80A80000" to tlv("80", "1980" + "10010200") + SW_OK, // AIP + AFL(SFI 2, rec 1..2)
                "00B20114" to tlv("70", tlv("5F24", "300131")) + SW_OK,
                "00B20214" to tlv("70", tlv("5A", humoPan)) + SW_OK,
            ),
        )

        val card = assertIs<NfcScanResult.Success>(EmvReader(provider).read()).card
        assertEquals(humoPan, card.pan)
        assertEquals(CardExpiry(1, 2030), card.expiry)
        assertEquals(EmvCardScheme.HUMO, card.scheme)
    }

    @Test
    fun retriesWithCorrectLeOn6Cxx() = runTest {
        var calls = 0
        val provider = CardProvider { command ->
            calls++
            val hex = command.joinToString("") { ((it.toInt() and 0xFF) + 0x100).toString(16).substring(1) }.uppercase()
            when {
                hex.startsWith("00A404000E") && hex.endsWith("00") && calls == 1 -> "6C20".hexToBytes()
                hex.startsWith("00A404000E") && hex.endsWith("20") -> ppseResponse
                hex.startsWith("00A4040007") -> selectVisaResponse
                hex.startsWith("80A8") -> tlv("77", tlv("57", "4000000000000002D30012010F")) + SW_OK
                else -> "6A82".hexToBytes()
            }
        }

        val card = assertIs<NfcScanResult.Success>(EmvReader(provider).read()).card
        assertEquals("4000000000000002", card.pan)
        assertEquals(EmvCardScheme.VISA, card.scheme)
    }

    @Test
    fun mapsLostConnectionToCardMovedTooFast() = runTest {
        val provider = CardProvider { throw CardConnectionLostException() }
        assertEquals(NfcScanResult.CardMovedTooFast, EmvReader(provider).read())
    }

    @Test
    fun reportsBlockedCardWhenPpseAnswers6A81() = runTest {
        val provider = FakeCardProvider(listOf("00A404000E" to "6A81".hexToBytes()))
        assertEquals(NfcScanResult.CardBlocked, EmvReader(provider).read())
    }

    @Test
    fun reportsBlockedCardWhenOnlyApplicationIsBlocked() = runTest {
        val provider = FakeCardProvider(
            listOf(
                "00A404000E" to ppseResponse,
                "00A4040007$visaAid" to "6283".hexToBytes(),
            ),
        )
        assertEquals(NfcScanResult.CardBlocked, EmvReader(provider).read())
    }

    @Test
    fun readsNextApplicationWhenFirstIsBlocked() = runTest {
        val mastercardAid = "A0000000041010"
        val provider = FakeCardProvider(
            listOf(
                "00A404000E" to tlv(
                    "6F",
                    tlv("84", ascii("2PAY.SYS.DDF01")),
                    tlv(
                        "A5",
                        tlv(
                            "BF0C",
                            tlv("61", tlv("4F", visaAid), tlv("87", "01")),
                            tlv("61", tlv("4F", mastercardAid), tlv("87", "02")),
                        ),
                    ),
                ) + SW_OK,
                "00A4040007$visaAid" to "6283".hexToBytes(),
                "00A4040007$mastercardAid" to tlv("6F", tlv("84", mastercardAid)) + SW_OK,
                "80A8" to tlv("77", tlv("57", "5555555555554444D30012010F")) + SW_OK,
            ),
        )

        val card = assertIs<NfcScanResult.Success>(EmvReader(provider).read()).card
        assertEquals("5555555555554444", card.pan)
        assertEquals(EmvCardScheme.MASTERCARD, card.scheme)
    }

    @Test
    fun reportsMissingPaymentApplication() = runTest {
        val result = EmvReader(FakeCardProvider(emptyList())).read()
        assertEquals(NfcErrorType.NO_PAYMENT_APPLICATION, assertIs<NfcScanResult.Error>(result).type)
    }
}

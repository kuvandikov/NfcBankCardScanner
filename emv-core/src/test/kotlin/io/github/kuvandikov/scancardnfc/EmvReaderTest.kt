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

package io.github.kuvandikov.scancardnfc

import io.github.kuvandikov.scancardnfc.model.CardExpiry
import io.github.kuvandikov.scancardnfc.model.EmvCardScheme
import io.github.kuvandikov.scancardnfc.model.NfcErrorType
import io.github.kuvandikov.scancardnfc.model.NfcScanResult
import io.github.kuvandikov.scancardnfc.provider.CardConnectionLostException
import io.github.kuvandikov.scancardnfc.provider.CardProvider
import io.github.kuvandikov.scancardnfc.testutils.CardFixtures
import io.github.kuvandikov.scancardnfc.testutils.FakeCardProvider
import io.github.kuvandikov.scancardnfc.testutils.SW_OK
import io.github.kuvandikov.scancardnfc.testutils.tlv
import io.github.kuvandikov.scancardnfc.util.hexToBytes
import io.github.kuvandikov.scancardnfc.util.toHex
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class EmvReaderTest {

    @Test
    fun readsVisaCardThroughAfl() = runTest {
        val provider = FakeCardProvider(CardFixtures.visa())

        val card = assertIs<NfcScanResult.Success>(EmvReader(provider).read()).card

        assertEquals("4111111111111111", card.pan)
        assertEquals(CardExpiry(12, 2029), card.expiry)
        assertEquals(EmvCardScheme.VISA, card.scheme)
        assertEquals("IVANOV/IVAN", card.holderName)
        assertEquals("VISA DEBIT", card.applicationLabel)

        // GPO must carry exactly the PDOL lengths: 4 + 6 + 4 + 2 + 3 = 19 bytes inside tag 83.
        val gpo = provider.sent.first { it.startsWith(CardFixtures.GPO) }
        assertTrue(gpo.startsWith("80A80000158313"), gpo)
        // Country / currency default to Uzbekistan (0860) at the 5F2A position.
        assertEquals("0860", gpo.substring(14 + 2 * 14, 14 + 2 * 16))
    }

    @Test
    fun readsCoBadgedHumoAsHumo() = runTest {
        val card = assertIs<NfcScanResult.Success>(
            EmvReader(FakeCardProvider(CardFixtures.humoCoBadgedVisa())).read(),
        ).card

        assertEquals("9860123412341234", card.pan)
        assertEquals(CardExpiry(1, 2030), card.expiry)
        assertEquals(EmvCardScheme.HUMO, card.scheme)
    }

    @Test
    fun reportsBlockedCard() = runTest {
        assertEquals(
            NfcScanResult.CardBlocked,
            EmvReader(FakeCardProvider(CardFixtures.blockedMastercard())).read(),
        )
    }

    @Test
    fun retriesWithCorrectLeOn6Cxx() = runTest {
        val visa = CardFixtures.visa().toMap()
        var calls = 0
        val provider = CardProvider { command ->
            calls++
            val hex = command.toHex()
            when {
                hex.startsWith(CardFixtures.SELECT_PPSE) && hex.endsWith("00") && calls == 1 -> "6C20".hexToBytes()
                hex.startsWith(CardFixtures.SELECT_PPSE) && hex.endsWith("20") -> visa.getValue(CardFixtures.SELECT_PPSE)
                hex.startsWith("00A4040007") -> visa.getValue(CardFixtures.selectAid(CardFixtures.VISA_AID))
                hex.startsWith(CardFixtures.GPO) -> tlv("77", tlv("57", "4000000000000002D30012010F")) + SW_OK
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
    fun reportsMissingPaymentApplication() = runTest {
        val result = EmvReader(FakeCardProvider(emptyList())).read()
        assertEquals(NfcErrorType.NO_PAYMENT_APPLICATION, assertIs<NfcScanResult.Error>(result).type)
    }
}

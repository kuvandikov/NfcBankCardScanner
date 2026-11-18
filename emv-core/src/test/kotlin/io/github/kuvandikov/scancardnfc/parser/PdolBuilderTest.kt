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

import io.github.kuvandikov.scancardnfc.EmvReaderConfig
import io.github.kuvandikov.scancardnfc.util.hexToBytes
import io.github.kuvandikov.scancardnfc.util.toHex
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class PdolBuilderTest {

    private val builder = PdolBuilder(EmvReaderConfig(), Random(seed = 42))

    @Test
    fun outputLengthMatchesRequestedLengths() {
        val pdol = TlvParser.parseDol("9F66049F02069F37045F2A029A039F1A02".hexToBytes())
        assertEquals(4 + 6 + 4 + 2 + 3 + 2, builder.build(pdol).size)
    }

    @Test
    fun encodesCountryAndCurrencyAsBcd() {
        val pdol = listOf(DolEntry(EmvTags.TERMINAL_COUNTRY_CODE, 2), DolEntry(EmvTags.TRANSACTION_CURRENCY_CODE, 2))
        assertEquals("08600860", builder.build(pdol).toHex())
    }

    @Test
    fun usesConfiguredCodes() {
        val kz = PdolBuilder(EmvReaderConfig(terminalCountryCode = 398, transactionCurrencyCode = 398))
        assertEquals("0398", kz.build(listOf(DolEntry(EmvTags.TERMINAL_COUNTRY_CODE, 2))).toHex())
    }

    @Test
    fun unknownTagsAreZeroFilled() {
        assertEquals("000000", builder.build(listOf(DolEntry(0xDF01, 3))).toHex())
    }

    @Test
    fun truncatesOrPadsToRequestedLength() {
        // TTQ is 4 bytes; a card asking for 2 gets the rightmost 2.
        assertEquals("4000", builder.build(listOf(DolEntry(EmvTags.TTQ, 2))).toHex())
        assertEquals("00F0204000", builder.build(listOf(DolEntry(EmvTags.TTQ, 5))).toHex())
    }
}

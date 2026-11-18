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

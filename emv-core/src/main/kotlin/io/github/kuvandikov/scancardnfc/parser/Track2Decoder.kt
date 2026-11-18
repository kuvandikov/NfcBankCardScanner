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
import io.github.kuvandikov.scancardnfc.util.toHex

/**
 * Decodes Track 2 Equivalent Data (tag 57 / 9F6B):
 * `PAN 'D' YYMM ServiceCode DiscretionaryData 'F'`.
 *
 * The discretionary data (which may contain a dynamic CVV) is intentionally dropped.
 */
internal object Track2Decoder {

    data class Track2(val pan: String, val expiry: CardExpiry?)

    fun decode(track2: ByteArray): Track2? {
        val hex = track2.toHex()
        val separator = hex.indexOf('D')
        if (separator !in 12..19) return null
        val pan = hex.substring(0, separator)
        if (pan.any { !it.isDigit() }) return null
        val expiry = hex.substring(separator + 1).take(4).let { parseYyMm(it) }
        return Track2(pan, expiry)
    }

    /** `YYMM` → [CardExpiry], or `null` when malformed. */
    fun parseYyMm(yymm: String): CardExpiry? {
        if (yymm.length < 4 || yymm.any { !it.isDigit() }) return null
        val year = 2000 + yymm.substring(0, 2).toInt()
        val month = yymm.substring(2, 4).toInt()
        return if (month in 1..12) CardExpiry(month, year) else null
    }
}

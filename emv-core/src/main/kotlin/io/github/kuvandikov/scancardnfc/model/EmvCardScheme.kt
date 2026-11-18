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

package io.github.kuvandikov.scancardnfc.model

/**
 * Payment scheme of a card.
 *
 * A scheme is recognised by its Registered Application Provider Identifier (RID, the first
 * 5 bytes of the AID) and, as a fallback, by the PAN's BIN range.
 *
 * Domestic Uzbek schemes (Uzcard, Humo) are detected by BIN first, because these cards are
 * frequently co-badged with an international scheme and expose that scheme's AID.
 */
public enum class EmvCardScheme(
    public val displayName: String,
    internal val aidPrefixes: List<String>,
    internal val panPattern: Regex?,
) {
    VISA("Visa", listOf("A000000003"), Regex("^4\\d{12}(\\d{3}|\\d{6})?$")),
    MASTERCARD("Mastercard", listOf("A000000004"), Regex("^(5[1-5]\\d{14}|2(22[1-9]|2[3-9]\\d|[3-6]\\d{2}|7[01]\\d|720)\\d{12})$")),
    MAESTRO("Maestro", listOf("A0000000043060", "A0000000050001"), Regex("^(5018|5020|5038|6304|6759|676[1-3])\\d{8,15}$")),
    AMERICAN_EXPRESS("American Express", listOf("A000000025"), Regex("^3[47]\\d{13}$")),
    UNIONPAY("UnionPay", listOf("A000000333"), Regex("^62\\d{14,17}$")),
    JCB("JCB", listOf("A000000065"), Regex("^35(2[89]|[3-8]\\d)\\d{12}$")),
    DISCOVER("Discover", listOf("A000000152"), Regex("^6(011|5\\d{2}|4[4-9]\\d)\\d{12,15}$")),
    MIR("Mir", listOf("A000000658"), Regex("^220[0-4]\\d{12}$")),

    // TODO(stage 3): record the domestic AIDs from real cards (PPSE tag 4F) and add them here.
    UZCARD("Uzcard", emptyList(), Regex("^8600\\d{12}$")),
    HUMO("Humo", emptyList(), Regex("^9860\\d{12}$")),

    UNKNOWN("Unknown", emptyList(), null),
    ;

    public companion object {
        private val domestic = listOf(UZCARD, HUMO)

        /** Detects the scheme from the PAN and/or AID (hex). Never returns `null`. */
        public fun detect(pan: String?, aid: String?): EmvCardScheme {
            if (pan != null) domestic.firstOrNull { it.panPattern!!.matches(pan) }?.let { return it }
            if (aid != null) fromAid(aid)?.let { return it }
            if (pan != null) entries.firstOrNull { it.panPattern?.matches(pan) == true }?.let { return it }
            return UNKNOWN
        }

        /** Longest-prefix match, so Maestro (A0000000043060) wins over Mastercard (A000000004). */
        public fun fromAid(aid: String): EmvCardScheme? {
            val normalized = aid.uppercase()
            return entries
                .flatMap { scheme -> scheme.aidPrefixes.map { prefix -> prefix to scheme } }
                .filter { (prefix, _) -> normalized.startsWith(prefix) }
                .maxByOrNull { (prefix, _) -> prefix.length }
                ?.second
        }

        /** Full AIDs tried one by one when the card has no PPSE directory. */
        internal val knownAids: List<String> = listOf(
            "A0000000031010", // Visa credit/debit
            "A0000000032010", // Visa Electron
            "A0000000041010", // Mastercard
            "A0000000043060", // Maestro
            "A0000006581010", // Mir
            "A0000003330101", // UnionPay
            "A00000002501", // American Express
            "A0000000651010", // JCB
            "A0000001523010", // Discover
        )
    }
}

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

package io.github.kuvandikov.scancardnfc.testutils

/**
 * APDU scripts for [FakeCardProvider], one per card profile.
 *
 * RULE: every value here is synthetic. When a script is derived from a real card log, replace
 * the PAN, cardholder name, Track 2 discretionary data and all cryptograms before committing.
 * Test PANs: 4111111111111111 (Visa), 5555555555554444 (Mastercard), 9860… / 8600… (made up).
 */
object CardFixtures {

    const val VISA_AID: String = "A0000000031010"
    const val MASTERCARD_AID: String = "A0000000041010"

    /** PPSE (`SELECT 2PAY.SYS.DDF01`) command prefix. */
    const val SELECT_PPSE: String = "00A404000E325041592E"

    fun selectAid(aid: String): String = "00A40400" + "%02X".format(aid.length / 2) + aid

    const val GPO: String = "80A80000"

    fun ppse(aid: String, label: String): ByteArray = tlv(
        "6F",
        tlv("84", ascii("2PAY.SYS.DDF01")),
        tlv("A5", tlv("BF0C", tlv("61", tlv("4F", aid), tlv("50", ascii(label)), tlv("87", "01")))),
    ) + SW_OK

    /**
     * Visa qVSDC: PPSE → SELECT with PDOL → GPO format 2 with AFL → Track 2 in SFI 1 record 1.
     * PAN 4111111111111111, expiry 12/29.
     */
    fun visa(): List<Pair<String, ByteArray>> = listOf(
        SELECT_PPSE to ppse(VISA_AID, "VISA DEBIT"),
        selectAid(VISA_AID) to tlv(
            "6F",
            tlv("84", VISA_AID),
            tlv("A5", tlv("50", ascii("VISA DEBIT")), tlv("9F38", "9F66049F02069F37045F2A029A03")),
        ) + SW_OK,
        GPO to tlv("77", tlv("82", "2000"), tlv("94", "08010100")) + SW_OK,
        "00B2010C" to tlv(
            "70",
            tlv("57", "4111111111111111D29122010000000000000F"),
            tlv("5F20", ascii("IVANOV/IVAN ")),
        ) + SW_OK,
    )

    /**
     * Humo card co-badged with Visa: exposes the Visa AID, PAN in tag 5A with BIN 9860.
     * Card has no PPSE; GPO answers format 1 (AIP + AFL: SFI 2, records 1..2).
     */
    fun humoCoBadgedVisa(): List<Pair<String, ByteArray>> = listOf(
        SELECT_PPSE to sw("6A82"),
        selectAid(VISA_AID) to tlv("6F", tlv("84", VISA_AID)) + SW_OK,
        GPO to tlv("80", "1980" + "10010200") + SW_OK,
        "00B20114" to tlv("70", tlv("5F24", "300131")) + SW_OK,
        "00B20214" to tlv("70", tlv("5A", "9860123412341234")) + SW_OK,
    )

    /** Mastercard whose payment application is blocked: SELECT AID answers `6A81`. */
    fun blockedMastercard(): List<Pair<String, ByteArray>> = listOf(
        SELECT_PPSE to ppse(MASTERCARD_AID, "MASTERCARD"),
        selectAid(MASTERCARD_AID) to sw("6A81"),
    )
}

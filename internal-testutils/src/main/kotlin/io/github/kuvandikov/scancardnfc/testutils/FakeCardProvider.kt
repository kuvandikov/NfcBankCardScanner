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

import io.github.kuvandikov.scancardnfc.provider.CardProvider
import io.github.kuvandikov.scancardnfc.util.hexToBytes
import io.github.kuvandikov.scancardnfc.util.toHex

/**
 * Scripted card: maps command-hex prefixes to responses, first match wins.
 * Unknown commands answer `6A82` (file not found), like a real card.
 *
 * Prefix matching is needed because GET PROCESSING OPTIONS carries a random
 * unpredictable number (9F37).
 */
class FakeCardProvider(
    private val script: List<Pair<String, ByteArray>>,
) : CardProvider {

    /** Every command sent, as uppercase hex. */
    val sent: MutableList<String> = mutableListOf()

    override suspend fun transceive(command: ByteArray): ByteArray {
        val hex = command.toHex()
        sent += hex
        return script.firstOrNull { (prefix, _) -> hex.startsWith(prefix) }?.second
            ?: "6A82".hexToBytes()
    }
}

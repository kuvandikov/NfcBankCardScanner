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

import io.github.kuvandikov.scancardnfc.model.NfcErrorType
import io.github.kuvandikov.scancardnfc.model.NfcScanResult
import io.github.kuvandikov.scancardnfc.parser.CardBlockedException
import io.github.kuvandikov.scancardnfc.parser.EmvException
import io.github.kuvandikov.scancardnfc.parser.EmvParser
import io.github.kuvandikov.scancardnfc.provider.CardConnectionLostException
import io.github.kuvandikov.scancardnfc.provider.CardProvider
import kotlin.coroutines.cancellation.CancellationException

/** Terminal parameters sent to the card. Defaults describe a terminal in Uzbekistan. */
public data class EmvReaderConfig(
    /** ISO 3166-1 numeric country code (tag 9F1A). 860 = Uzbekistan. */
    val terminalCountryCode: Int = 860,
    /** ISO 4217 numeric currency code (tag 5F2A). 860 = UZS. */
    val transactionCurrencyCode: Int = 860,
    /** I/O timeout for a single APDU exchange, where the platform supports it. */
    val transceiveTimeoutMillis: Int = 5_000,
    /** Upper bounds for the record scan used when the card returns no AFL. */
    val maxFallbackSfi: Int = 10,
    val maxFallbackRecords: Int = 10,
)

/**
 * Platform-independent entry point of the EMV engine.
 *
 * Use it directly when you already have a connected card, e.g. with a custom [CardProvider]:
 *
 * ```kotlin
 * val result = EmvReader(myProvider).read()
 * ```
 *
 * On Android prefer `AndroidNfcScanner` from the `scan-card-nfc` artifact.
 */
public class EmvReader(
    private val provider: CardProvider,
    private val config: EmvReaderConfig = EmvReaderConfig(),
) {
    /** Reads the card. Never throws except for coroutine cancellation. */
    public suspend fun read(): NfcScanResult = try {
        NfcScanResult.Success(EmvParser(provider, config).readCard())
    } catch (e: CancellationException) {
        throw e
    } catch (e: CardConnectionLostException) {
        NfcScanResult.CardMovedTooFast
    } catch (e: CardBlockedException) {
        NfcScanResult.CardBlocked
    } catch (e: EmvException) {
        NfcScanResult.Error(e.type, e.message)
    } catch (e: Exception) {
        NfcScanResult.Error(NfcErrorType.UNKNOWN, e.message)
    }
}

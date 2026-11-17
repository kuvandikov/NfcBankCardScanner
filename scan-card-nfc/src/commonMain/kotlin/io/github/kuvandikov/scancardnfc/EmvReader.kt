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
 * On Android and iOS prefer the ready-made `NfcCardScanner` implementations.
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

package io.github.kuvandikov.scancardnfc.scanner

import io.github.kuvandikov.scancardnfc.EmvReader
import io.github.kuvandikov.scancardnfc.EmvReaderConfig
import io.github.kuvandikov.scancardnfc.NfcCardScanner
import io.github.kuvandikov.scancardnfc.model.NfcErrorType
import io.github.kuvandikov.scancardnfc.model.NfcScanResult
import io.github.kuvandikov.scancardnfc.provider.IosCardProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import platform.CoreNFC.NFCPollingISO14443
import platform.CoreNFC.NFCReaderSession
import platform.CoreNFC.NFCTagProtocol
import platform.CoreNFC.NFCTagReaderSession
import platform.CoreNFC.NFCTagReaderSessionDelegateProtocol
import platform.Foundation.NSError
import platform.darwin.NSObject

/**
 * [NfcCardScanner] based on CoreNFC's `NFCTagReaderSession`.
 *
 * Requirements in the host app:
 * - Entitlement `com.apple.developer.nfc.readersession.formats` = `TAG`
 * - `NFCReaderUsageDescription` in Info.plist
 * - `com.apple.developer.nfc.readersession.iso7816.select-identifiers` listing the PPSE
 *   (`325041592E5359532E4444463031`) and every payment AID you want to read.
 */
public class IosNfcScanner(
    private val config: EmvReaderConfig = EmvReaderConfig(),
    private val messages: Messages = Messages(),
) : NfcCardScanner {

    /** Texts shown on the system NFC sheet. */
    public data class Messages(
        val prompt: String = "Hold your card near the top of the iPhone",
        val reading: String = "Reading card…",
        val success: String = "Card read",
        val failure: String = "Could not read the card. Try again.",
    )

    // CoreNFC keeps only a weak reference to the delegate.
    private var activeDelegate: TagReaderDelegate? = null

    override val isSupported: Boolean get() = NFCReaderSession.readingAvailable

    override val isEnabled: Boolean get() = isSupported

    override suspend fun scan(): NfcScanResult {
        if (!NFCReaderSession.readingAvailable) return NfcScanResult.NfcNotSupported

        val result = CompletableDeferred<NfcScanResult>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val delegate = TagReaderDelegate(config, messages, scope, result)
        activeDelegate = delegate

        val session = NFCTagReaderSession(
            pollingOption = NFCPollingISO14443,
            delegate = delegate,
            queue = null,
        )
        session.alertMessage = messages.prompt
        session.beginSession()

        return try {
            result.await()
        } catch (e: CancellationException) {
            session.invalidateSession()
            throw e
        } finally {
            scope.cancel()
            activeDelegate = null
        }
    }
}

private class TagReaderDelegate(
    private val config: EmvReaderConfig,
    private val messages: IosNfcScanner.Messages,
    private val scope: CoroutineScope,
    private val result: CompletableDeferred<NfcScanResult>,
) : NSObject(), NFCTagReaderSessionDelegateProtocol {

    override fun tagReaderSessionDidBecomeActive(session: NFCTagReaderSession) = Unit

    override fun tagReaderSession(session: NFCTagReaderSession, didInvalidateWithError: NSError) {
        // Called for every invalidation, including our own; only the first result counts.
        val outcome = when (didInvalidateWithError.code) {
            ERROR_USER_CANCELED -> NfcScanResult.Cancelled
            ERROR_SESSION_TIMEOUT -> NfcScanResult.Error(NfcErrorType.TIMEOUT, didInvalidateWithError.localizedDescription)
            else -> NfcScanResult.Error(NfcErrorType.UNKNOWN, didInvalidateWithError.localizedDescription)
        }
        result.complete(outcome)
    }

    override fun tagReaderSession(session: NFCTagReaderSession, didDetectTags: List<*>) {
        val tag = didDetectTags.firstOrNull() as? NFCTagProtocol
        val iso7816 = tag?.asNFCISO7816Tag()
        if (tag == null || iso7816 == null) {
            session.alertMessage = messages.failure
            session.restartPolling()
            return
        }

        session.connectToTag(tag) { error ->
            if (error != null) {
                session.invalidateSessionWithErrorMessage(messages.failure)
                result.complete(NfcScanResult.CardMovedTooFast)
                return@connectToTag
            }
            session.alertMessage = messages.reading
            scope.launch {
                val outcome = EmvReader(IosCardProvider(iso7816), config).read()
                result.complete(outcome)
                if (outcome is NfcScanResult.Success) {
                    session.alertMessage = messages.success
                    session.invalidateSession()
                } else {
                    session.invalidateSessionWithErrorMessage(messages.failure)
                }
            }
        }
    }
}

// NFCReaderError codes (CoreNFC/NFCError.h). Top-level because a companion of an ObjC subclass cannot hold fields.
private const val ERROR_USER_CANCELED = 200L
private const val ERROR_SESSION_TIMEOUT = 201L

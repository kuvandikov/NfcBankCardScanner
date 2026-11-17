package io.github.kuvandikov.scancardnfc.scanner

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import io.github.kuvandikov.scancardnfc.EmvReader
import io.github.kuvandikov.scancardnfc.EmvReaderConfig
import io.github.kuvandikov.scancardnfc.NfcCardScanner
import io.github.kuvandikov.scancardnfc.model.NfcErrorType
import io.github.kuvandikov.scancardnfc.model.NfcScanResult
import io.github.kuvandikov.scancardnfc.provider.AndroidCardProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * [NfcCardScanner] based on [NfcAdapter.enableReaderMode] (foreground reader mode).
 *
 * Reader mode is only active while [scan] is suspended, and only while [activity] is in the
 * foreground. Create the scanner in `onCreate` and call [scan] from a lifecycle-aware scope.
 */
public class AndroidNfcScanner(
    private val activity: Activity,
    private val config: EmvReaderConfig = EmvReaderConfig(),
    /** Play the system sound when a tag is detected. */
    private val playPlatformSound: Boolean = true,
) : NfcCardScanner {

    private val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)

    override val isSupported: Boolean get() = adapter != null

    override val isEnabled: Boolean get() = adapter?.isEnabled == true

    override suspend fun scan(): NfcScanResult {
        val adapter = adapter ?: return NfcScanResult.NfcNotSupported
        if (!adapter.isEnabled) return NfcScanResult.NfcDisabled

        val detected = CompletableDeferred<Tag>()
        withContext(Dispatchers.Main) {
            adapter.enableReaderMode(activity, { tag -> detected.complete(tag) }, readerFlags(), readerExtras())
        }
        try {
            val tag = detected.await()
            val isoDep = IsoDep.get(tag)
                ?: return NfcScanResult.Error(NfcErrorType.UNSUPPORTED_TAG, "Tag does not support ISO-DEP")
            return readIsoDep(isoDep)
        } finally {
            withContext(NonCancellable + Dispatchers.Main) {
                adapter.disableReaderMode(activity)
            }
        }
    }

    private suspend fun readIsoDep(isoDep: IsoDep): NfcScanResult = withContext(Dispatchers.IO) {
        try {
            isoDep.connect()
            isoDep.timeout = config.transceiveTimeoutMillis
            EmvReader(AndroidCardProvider(isoDep), config).read()
        } catch (e: IOException) {
            NfcScanResult.CardMovedTooFast
        } finally {
            runCatching { isoDep.close() }
        }
    }

    private fun readerFlags(): Int {
        var flags = NfcAdapter.FLAG_READER_NFC_A or
            NfcAdapter.FLAG_READER_NFC_B or
            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
        if (!playPlatformSound) flags = flags or NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS
        return flags
    }

    private fun readerExtras() = Bundle().apply {
        putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250)
    }
}

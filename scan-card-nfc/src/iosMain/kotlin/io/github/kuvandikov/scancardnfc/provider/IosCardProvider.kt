package io.github.kuvandikov.scancardnfc.provider

import io.github.kuvandikov.scancardnfc.util.toByteArray
import io.github.kuvandikov.scancardnfc.util.toNSData
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreNFC.NFCISO7816APDU
import platform.CoreNFC.NFCISO7816TagProtocol
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * [CardProvider] backed by a CoreNFC [NFCISO7816TagProtocol] that is already connected
 * through an `NFCTagReaderSession`.
 *
 * CoreNFC returns data and SW1/SW2 separately; they are joined back into a single
 * response APDU to satisfy the [CardProvider] contract.
 */
public class IosCardProvider(private val tag: NFCISO7816TagProtocol) : CardProvider {

    override suspend fun transceive(command: ByteArray): ByteArray = suspendCancellableCoroutine { cont ->
        val apdu = command.toNfcApdu()
        tag.sendCommandWithAPDU(apdu) { data, sw1, sw2, error ->
            if (error != null) {
                cont.resumeWithException(CardConnectionLostException(IllegalStateException(error.localizedDescription)))
            } else {
                val body = data?.toByteArray() ?: ByteArray(0)
                cont.resume(body + byteArrayOf(sw1.toByte(), sw2.toByte()))
            }
        }
    }

    /** Splits a short command APDU (`CLA INS P1 P2 [Lc data] [Le]`) into CoreNFC fields. */
    private fun ByteArray.toNfcApdu(): NFCISO7816APDU {
        require(size >= 4) { "APDU must contain at least a header" }
        val lc = if (size > 5) this[4].toInt() and 0xFF else 0
        val data = if (lc > 0) copyOfRange(5, 5 + lc) else ByteArray(0)
        val le: Long = when {
            size == 4 -> -1
            size == 5 -> (this[4].toInt() and 0xFF).let { if (it == 0) 256L else it.toLong() }
            size == 5 + lc + 1 -> (this[size - 1].toInt() and 0xFF).let { if (it == 0) 256L else it.toLong() }
            else -> -1
        }
        return NFCISO7816APDU(
            instructionClass = this[0].toUByte(),
            instructionCode = this[1].toUByte(),
            p1Parameter = this[2].toUByte(),
            p2Parameter = this[3].toUByte(),
            data = data.toNSData(),
            expectedResponseLength = le,
        )
    }
}

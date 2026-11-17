package io.github.kuvandikov.scancardnfc.provider

import android.nfc.TagLostException
import android.nfc.tech.IsoDep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/** [CardProvider] backed by an already connected [IsoDep] tag. */
public class AndroidCardProvider(private val isoDep: IsoDep) : CardProvider {

    override suspend fun transceive(command: ByteArray): ByteArray = withContext(Dispatchers.IO) {
        try {
            isoDep.transceive(command)
        } catch (e: TagLostException) {
            throw CardConnectionLostException(e)
        } catch (e: IOException) {
            throw CardConnectionLostException(e)
        }
    }
}

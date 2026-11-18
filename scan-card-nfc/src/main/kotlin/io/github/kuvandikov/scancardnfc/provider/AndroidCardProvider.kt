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

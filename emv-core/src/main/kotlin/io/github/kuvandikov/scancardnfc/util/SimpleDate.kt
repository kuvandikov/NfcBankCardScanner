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

package io.github.kuvandikov.scancardnfc.util

import java.util.Calendar

internal data class SimpleDate(val year: Int, val month: Int, val day: Int)

/**
 * Local calendar date of the device, used for the Transaction Date (9A) in the PDOL.
 *
 * `java.util.Calendar` instead of `java.time`: `java.time` needs API 26 or core library
 * desugaring in the consuming app, and the library supports older devices.
 */
internal fun currentDate(): SimpleDate {
    val c = Calendar.getInstance()
    return SimpleDate(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
}

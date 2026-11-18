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

package io.github.kuvandikov.scancardnfc.settings

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Opens the system NFC settings so the user can switch NFC on.
 * Falls back to the wireless settings screen on devices without a dedicated NFC page.
 */
public fun Context.openNfcSettings() {
    val flags = if (this is Activity) 0 else Intent.FLAG_ACTIVITY_NEW_TASK
    try {
        startActivity(Intent(Settings.ACTION_NFC_SETTINGS).addFlags(flags))
    } catch (e: ActivityNotFoundException) {
        startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS).addFlags(flags))
    }
}

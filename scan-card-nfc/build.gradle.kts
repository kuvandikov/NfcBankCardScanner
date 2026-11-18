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

plugins {
    alias(libs.plugins.scancardnfc.android.library)
    alias(libs.plugins.scancardnfc.library.published)
}

android {
    namespace = "io.github.kuvandikov.scancardnfc"
}

dependencies {
    api(projects.emvCore)
    // ComponentActivity is part of the public API (AndroidNfcScanner constructor).
    api(libs.androidx.activity)

    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(projects.internalTestutils)
    testImplementation(libs.kotlinx.coroutines.test)
}

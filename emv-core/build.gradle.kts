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
    alias(libs.plugins.scancardnfc.jvm.library)
    alias(libs.plugins.scancardnfc.library.published)
}

// Pure Kotlin/JVM: no Android dependency is allowed here. This keeps the EMV engine testable
// without an emulator and ready to move to Kotlin Multiplatform if iOS becomes possible.
dependencies {
    testImplementation(projects.internalTestutils)
    testImplementation(libs.kotlinx.coroutines.test)
}

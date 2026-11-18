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

import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

/**
 * Everything a module needs to become a public artifact (google/accompanist style):
 *
 * - Strict explicit API mode for main sources (tests are exempt).
 * - Binary Compatibility Validator: `apiDump` writes `api/<module>.api`, `apiCheck` fails the
 *   build when the public API changes without an updated dump.
 * - Dokka for the API reference published to GitHub Pages.
 * - `com.vanniktech.maven.publish` for Maven Central. Coordinates and POM come from
 *   `gradle.properties` (root + module).
 *
 * Apply after `scancardnfc.android.library` or `scancardnfc.jvm.library`.
 */
class LibraryPublishedConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "org.jetbrains.kotlinx.binary-compatibility-validator")
            apply(plugin = "org.jetbrains.dokka")
            apply(plugin = "com.vanniktech.maven.publish")

            tasks.withType<KotlinCompilationTask<*>>().configureEach {
                if (!name.contains("Test")) {
                    compilerOptions.freeCompilerArgs.add("-Xexplicit-api=strict")
                }
            }

            extensions.configure<MavenPublishBaseExtension> {
                publishToMavenCentral()
                // Signing keys only exist in CI (GitHub Secrets); local publishToMavenLocal stays unsigned.
                if (providers.gradleProperty("signingInMemoryKey").isPresent) {
                    signAllPublications()
                }
            }
        }
    }
}

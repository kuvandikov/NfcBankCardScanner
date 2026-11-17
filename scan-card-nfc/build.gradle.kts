@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.mavenPublish)
}

kotlin {
    // Strict API mode: every public declaration needs an explicit visibility and type.
    explicitApi()

    applyDefaultHierarchyTemplate()

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    android {
        namespace = "io.github.kuvandikov.scancardnfc"
        compileSdk {
            version = release(libs.versions.android.compileSdk.get().toInt()) {
                minorApiLevel = libs.versions.android.compileSdkMinor.get().toInt()
            }
        }
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions.jvmTarget = JvmTarget.JVM_11
    }

    // Plain JVM target: fast host tests for the EMV engine, and lets desktop
    // apps reuse the parser with their own CardProvider (e.g. PC/SC readers).
    jvm()

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
        }
    }

    jvmToolchain(21)
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }

    // Coordinates come from GROUP / POM_ARTIFACT_ID / VERSION_NAME in gradle.properties.

    pom {
        name.set("NFC Card Reader for Kotlin Multiplatform")
        description.set("Reads public EMV data (PAN, expiry, scheme) from contactless bank cards on Android and iOS.")
        inceptionYear.set("2024")
        url.set("https://github.com/kuvandikov/NfcBankCardScanner")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("kuvandikov")
                name.set("Anvarbek Kuvandikov")
                url.set("https://github.com/kuvandikov")
            }
        }
        scm {
            url.set("https://github.com/kuvandikov/NfcBankCardScanner")
            connection.set("scm:git:git://github.com/kuvandikov/NfcBankCardScanner.git")
            developerConnection.set("scm:git:ssh://git@github.com/kuvandikov/NfcBankCardScanner.git")
        }
    }
}

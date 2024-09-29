plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.vanniktech.maven.publish)
}

android {
    namespace = "io.github.kuvandikov.scan_card_nfc"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}


dependencies {
    // For NFC
    implementation(libs.commons.io)
    implementation(libs.android.logger)
    implementation(libs.commons.collections4)
    implementation(libs.commons.lang3)
    implementation(libs.bit.lib4j)
    implementation(libs.play.services.location) {
        exclude("com.android.support")
    }

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.espresso.core)
}
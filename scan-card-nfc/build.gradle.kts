plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.vanniktech.maven.publish)
}

android {
    namespace = "io.github.kuvandikov.scan_card_nfc"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
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
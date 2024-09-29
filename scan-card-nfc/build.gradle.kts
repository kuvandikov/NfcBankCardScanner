plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    `maven-publish`
    signing
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

publishing {
    publications {
        create<MavenPublication>("maven") {

            groupId = "io.github.kuvandikov"
            artifactId = "scan-card-nfc"
            version = "1.1"

            pom {
                name.set("My Library")
                description.set("This is a description of my library.")
                url.set("https://github.com/kuvandikov/NfcBankCardScanner")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("anvarbek")
                        name.set("Anvarbek Kuvandikov")
                        email.set("loving.uz0000@gmail.com")
                    }
                }
                scm {
//                    git@github.com:kuvandikov/NfcBankCardScanner.git
                    connection.set("scm:git:git://github.com:kuvandikov/NfcBankCardScanner.git")
                    developerConnection.set("scm:git:ssh://github.com:kuvandikov/NfcBankCardScanner.git")
                    url.set("https://github.com/kuvandikov/NfcBankCardScanner")
                }
            }
        }
    }
    repositories {
        maven {
            name = "maven"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = findProperty("ossrhToken") as String
                password = findProperty("ossrhTokenPassword") as String
            }
        }
    }
}

signing {
    useGpgCmd() // This ensures the use of GPG command line
    sign(publishing.publications["maven"])
}


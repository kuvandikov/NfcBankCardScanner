plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    `maven-publish`
    signing
}

android {
    namespace = "io.github.kuvandikov.scan_card_nfc"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}


dependencies {
    // For NFC
    implementation("commons-io:commons-io:2.11.0")
    implementation("com.noveogroup.android:android-logger:1.3.5")
    implementation("org.apache.commons:commons-collections4:4.4")
    implementation("org.apache.commons:commons-lang3:3.9")
    implementation("com.github.devnied:bit-lib4j:1.5.0")
    implementation("com.google.android.gms:play-services-location:16.0.0") {
        exclude("com.android.support")
    }
    
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
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
                url.set("https://github.com/AnvarbekKuvandikov/NfcBankCardScanner")

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
//                    git@github.com:AnvarbekKuvandikov/NfcBankCardScanner.git
                    connection.set("scm:git:git://github.com:AnvarbekKuvandikov/NfcBankCardScanner.git")
                    developerConnection.set("scm:git:ssh://github.com:AnvarbekKuvandikov/NfcBankCardScanner.git")
                    url.set("https://github.com/AnvarbekKuvandikov/NfcBankCardScanner.git")
                }
            }
        }
    }
    repositories {
        maven {
            url = uri("https://oss.sonatype.org/repository/releases/")
            credentials {
                username = findProperty("sonatypeUsername") as String? ?: ""
                password = findProperty("sonatypePassword") as String? ?: ""
            }
        }
    }
}

signing {
    useGpgCmd() // This ensures the use of GPG command line
    sign(publishing.publications["maven"])
}


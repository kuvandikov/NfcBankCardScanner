plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    `maven-publish`
    signing
}

android {
    namespace = "com.kuvandikov.mylibrary"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

publishing {
    publications {
        create<MavenPublication>("maven") {

            groupId = "com.kuvandikov"
            artifactId = "mylibrary"
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

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "de.putzalarm.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "de.putzalarm.app"
        minSdk = 26
        targetSdk = 35
        // Auf dem PC bleibt es 1. Der GitHub-Build setzt die Laufnummer ein, damit jede neue
        // Version über die alte installiert werden kann.
        versionCode = System.getenv("PUTZALARM_VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = "0.1.0"
    }

    // Fester Schlüssel nur für den GitHub-Build (Releases fürs zweite Tablet). Ohne die
    // Umgebungsvariable (z. B. auf dem PC mit "Putzalarm aufs Tablet.bat") bleibt alles beim
    // normalen Debug-Schlüssel, sonst gingen die Daten auf dem ersten Tablet verloren.
    val releaseKeystore = System.getenv("PUTZALARM_KEYSTORE")?.let { file(it) }?.takeIf { it.exists() }
    if (releaseKeystore != null) {
        signingConfigs {
            create("putzalarm") {
                storeFile = releaseKeystore
                storePassword = System.getenv("PUTZALARM_KEYSTORE_PASSWORD") ?: "putzalarm"
                keyAlias = "putzalarm"
                keyPassword = System.getenv("PUTZALARM_KEYSTORE_PASSWORD") ?: "putzalarm"
            }
        }
    }

    buildTypes {
        if (releaseKeystore != null) {
            debug {
                signingConfig = signingConfigs.getByName("putzalarm")
            }
        }
        release {
            isMinifyEnabled = false
            // Bis es einen eigenen Schlüssel gibt, wird auch Release mit dem Debug-Schlüssel signiert.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.04.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    val camerax = "1.4.2"
    implementation("androidx.camera:camera-core:$camerax")
    implementation("androidx.camera:camera-camera2:$camerax")
    implementation("androidx.camera:camera-lifecycle:$camerax")
    implementation("androidx.camera:camera-view:$camerax")

    // Gesichtserkennung läuft komplett auf dem Gerät (gebündeltes Modell, kein Netz nötig).
    implementation("com.google.mlkit:face-detection:16.1.7")
}

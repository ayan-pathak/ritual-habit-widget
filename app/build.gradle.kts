plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// google-services.json is per-project configuration and is not in the
// repository. Without it the app still builds and still works — it simply has
// no cloud to reach, which is the same path a signed-out device takes.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.ayan.ritual"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ayan.ritual"
        minSdk = 26
        targetSdk = 35
        // Play rejects a second upload at the same versionCode, so CI passes
        // its run number in. A local build has no reason to care.
        versionCode = (findProperty("ritualVersionCode") as String?)?.toInt() ?: 1
        versionName = (findProperty("ritualVersionName") as String?) ?: "1.0"
    }

    // Google Sign-In matches on package name plus signing fingerprint, and a
    // runner generates a fresh debug keystore on every build — a different
    // fingerprint each time, so sign-in would fail on every CI build. This key
    // is checked in so the fingerprint is fixed and can be registered once.
    //
    // It is a development key and nothing more. It is not secret, it must
    // never sign a Play release, and Play App Signing gives its own
    // fingerprint to register alongside this one when the app ships.
    signingConfigs {
        create("dev") {
            storeFile = rootProject.file("keystore/ritual-dev.jks")
            storePassword = "ritualdev"
            keyAlias = "ritual"
            keyPassword = "ritualdev"
        }

        // The key Play is told to trust for uploads. It cannot be the dev key:
        // that one is in a public repository, so anyone could sign an artifact
        // with it. CI writes this out of a secret and it never lands on disk
        // anywhere else. Without the secret there is no such config, and the
        // bundle falls back to the dev key — fine for a first look at the
        // billing flow, not fine for anything that stays.
        val uploadStore = rootProject.file("keystore/upload.jks")
        if (uploadStore.exists()) {
            create("upload") {
                storeFile = uploadStore
                storePassword = System.getenv("UPLOAD_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("UPLOAD_KEY_ALIAS") ?: "upload"
                keyPassword = System.getenv("UPLOAD_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("dev")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("upload")
                ?: signingConfigs.getByName("dev")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("com.android.billingclient:billing-ktx:7.1.1")
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    // Google sign-in goes through Credential Manager rather than the retired
    // GoogleSignIn client: one sheet, and it offers passkeys and saved
    // passwords in the same place.
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
}

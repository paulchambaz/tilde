plugins {
    id("com.android.application") version "8.4.0"
    id("org.jetbrains.kotlin.android") version "2.0.0"
}

android {
    namespace = "xyz.chambaz.testandroid"
    compileSdk = 35

    defaultConfig {
        applicationId = "xyz.chambaz.testandroid"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
}

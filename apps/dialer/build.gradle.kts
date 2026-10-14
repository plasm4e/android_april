import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "org.privacyrom.dialer"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.privacyrom.dialer"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        // Same switch as the keyboard: -PdevScreenshots=true disables FLAG_SECURE for dev shots.
        val secure = (findProperty("devScreenshots") as String?) != "true"
        buildConfigField("boolean", "SECURE_WINDOWS", secure.toString())
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}

plugins {
    // AGP 9 provides built-in Kotlin support; the kotlin-android plugin must NOT be applied.
    alias(libs.plugins.android.application)
}

description = "Korge Application – Android entry point"
group = "org.korge.application"
version = rootProject.libs.versions.korge.get()

android {
    namespace = "org.korge.application.android"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.korge.application"
        // Korge is large; minSdk >= 21 enables native multidex (method count exceeds 64K).
        minSdk = maxOf(21, libs.versions.minSdk.get().toInt())
        targetSdk = libs.versions.compileSdk.get().toInt()
        versionCode = 1
        versionName = version.toString()
        multiDexEnabled = true
    }
}

dependencies {
    implementation(project(":korge"))
    implementation(project(":korge-sandbox:shared"))
}

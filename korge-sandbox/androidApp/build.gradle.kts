plugins {
    // AGP 9 provides built-in Kotlin support; the kotlin-android plugin must NOT be applied.
    alias(libs.plugins.android.application)
}

description = "Korge Sandbox – Android entry point"
group = "org.korge.sandbox"
version = rootProject.libs.versions.korge.get()

android {
    namespace = "org.korge.sandbox.android"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.korge.sandbox"
        // Korge is large; minSdk >= 21 enables native multidex (method count exceeds 64K).
        minSdk = maxOf(21, libs.versions.minSdk.get().toInt())
        targetSdk = libs.versions.compileSdk.get().toInt()
        versionCode = 1
        versionName = version.toString()
        multiDexEnabled = true
    }
}

dependencies {
    implementation(projects.korgeSandbox.shared)
}

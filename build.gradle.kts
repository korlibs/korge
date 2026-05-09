plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.multiplatform.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.vanniktech.mavenPublish) apply false
    alias(libs.plugins.kotlinx.benchmark) apply false
    alias(libs.plugins.kotlin.allopen) apply false
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
    }
}

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.korge.library) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

allprojects {
    repositories {
        mavenLocal()
        maven { url = uri("https://central.sonatype.com/repository/maven-snapshots") }
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

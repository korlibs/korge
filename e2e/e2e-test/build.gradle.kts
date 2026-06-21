group = "org.korge.e2e"
version = libs.versions.korge.get()

allprojects {
    repositories {
        mavenLocal {
            content {
                // Load gradle plugin from maven local explicitly in e2e tests
                includeGroup("org.korge.gradleplugins")
                includeGroup("org.korge.engine")
            }
        }
        maven { url = uri("https://central.sonatype.com/repository/maven-snapshots") }
        mavenCentral()
        google()
        maven { url = uri("https://plugins.gradle.org/m2/") }
    }
}

plugins {
    alias(libs.plugins.korge) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.application) apply false
}

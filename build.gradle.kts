
import com.soywiz.korlibs.root.*

buildscript {
    val kotlinVersion: String = libs.versions.kotlin.get()

    val androidBuildGradleVersion =
        if (System.getProperty("java.version").startsWith("1.8") || System.getProperty("java.version").startsWith("9")) {
            "4.2.0"
        } else {
            libs.versions.android.build.gradle.get()
        }

    repositories {
        mavenLocal()
        mavenCentral()
        google()
        maven { url = uri("https://plugins.gradle.org/m2/") }
        if (kotlinVersion.contains("eap") || kotlinVersion.contains("-")) {
            maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/temporary")
            maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
            maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven")
        }
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
    }
    dependencies {
        classpath(libs.gradle.publish.plugin)
        classpath("com.android.tools.build:gradle:$androidBuildGradleVersion")
    }
}

plugins {
	java
    kotlin("multiplatform")
    signing
    `maven-publish`
}

RootKorlibsPlugin.doInit(rootProject)

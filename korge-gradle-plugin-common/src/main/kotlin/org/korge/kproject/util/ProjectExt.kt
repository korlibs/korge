package org.korge.kproject.util

import org.gradle.api.*

fun Project.defineStandardRepositories() {
    repositories.apply {
        mavenLocal()
        maven { it.url = uri("https://central.sonatype.com/repository/maven-snapshots") }
        mavenCentral()
        google()
        gradlePluginPortal()
        maven { it.url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev") }
        maven { it.url = uri("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental") }
    }
}

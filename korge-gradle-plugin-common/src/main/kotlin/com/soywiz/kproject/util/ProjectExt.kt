package com.soywiz.kproject.util

import org.gradle.api.*

fun Project.defineStandardRepositories() {
    repositories.apply {
        mavenLocal()
        mavenCentral()
        google()
        gradlePluginPortal()
        maven { it.url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev") }
        maven { it.url = uri("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental") }
    }
}

package org.korge.kproject.util

import org.gradle.api.*

fun Project.defineStandardRepositories() {
    repositories.apply {
        mavenLocal()
        maven { url = uri("https://central.sonatype.com/repository/maven-snapshots") }
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

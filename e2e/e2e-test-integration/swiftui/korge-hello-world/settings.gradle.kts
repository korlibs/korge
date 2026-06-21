pluginManagement {
    repositories {
        mavenLocal()
        maven { url = uri("https://central.sonatype.com/repository/maven-snapshots") }
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

plugins {
    id("org.korge.engine.settings") version "7.0.0-SNAPSHOT"
}

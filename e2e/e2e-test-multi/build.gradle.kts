plugins {
    alias(libs.plugins.korge) apply false
}

allprojects { repositories { mavenLocal(); mavenCentral(); google(); gradlePluginPortal() } }

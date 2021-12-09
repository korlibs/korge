import java.util.Properties

plugins {
    `kotlin-dsl`
    publishing
    `maven-publish`
    signing
    //kotlin("multiplatform")
    //kotlin("gradle-plugin")
}

val gradleProperties = Properties().also { it.load(File(rootDir, "../gradle.properties").readText().reader()) }

//Properties().also { it.load() }

//val kotlinVersion = "1.5.31"
//val kotlinVersion = "1.6.0"
//val androidToolsBuildGradleVersion = "4.2.0"

val kotlinVersion = gradleProperties["kotlinVersion"].toString()
val androidBuildGradleVersion = when {
    System.getProperty("java.version").startsWith("1.8") || System.getProperty("java.version").startsWith("9") -> "4.2.0"
    else -> gradleProperties["androidBuildGradleVersion"].toString()
}

dependencies {
    implementation("com.android.tools.build:gradle:$androidBuildGradleVersion")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("com.google.code.gson:gson:2.8.6")
}


repositories {
    mavenLocal()
    mavenCentral()
    google()
}

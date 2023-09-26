package korlibs.korge.gradle

import org.gradle.api.*

val Project.jnaVersion get() = findProperty("jnaVersion") ?: BuildVersions.JNA
val Project.korgeVersion get() = findProperty("korgeVersion") ?: BuildVersions.KORGE
val Project.kotlinVersion: String get() = findProperty("kotlinVersion")?.toString() ?: BuildVersions.KOTLIN
val Project.kotlinVersionIsDev: Boolean get() = kotlinVersion.contains("-release") || kotlinVersion.contains("-eap") || kotlinVersion.contains("-M") || kotlinVersion.contains("-RC")
val Project.androidBuildGradleVersion get() = findProperty("androidBuildGradleVersion") ?: BuildVersions.ANDROID_BUILD
val Project.coroutinesVersion get() = findProperty("coroutinesVersion") ?: BuildVersions.COROUTINES

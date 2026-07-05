package korlibs.korge.gradle

import org.gradle.api.Project
import org.korge.gradle.BuildVersions

@Deprecated(
    message = "Versions are will be managed in version catalog. Do not use this value.",
    replaceWith = ReplaceWith("libs.versions.jna"),
)
val Project.jnaVersion get() = findProperty("jnaVersion") ?: BuildVersions.JNA

@Deprecated(
    message = "Versions are will be managed in version catalog. Do not use this value.",
    replaceWith = ReplaceWith("libs.versions.korge"),
)
val Project.korgeVersion get() = findProperty("korgeVersion") ?: BuildVersions.KORGE

@Deprecated(
    message = "Versions are will be managed in version catalog. Do not use this value.",
    replaceWith = ReplaceWith("libs.versions.kotlin"),
)
val Project.kotlinVersion: String get() = findProperty("kotlinVersion")?.toString() ?: BuildVersions.KOTLIN

@Deprecated(
    message = "Versions are will be managed in version catalog. Do not use this value.",
    replaceWith = ReplaceWith("libs.versions.kotlin"),
)
val Project.kotlinVersionIsDev: Boolean get() = kotlinVersion.contains("-release") || kotlinVersion.contains("-eap") || kotlinVersion.contains("-M") || kotlinVersion.contains("-RC")

@Deprecated(
    message = "Versions are will be managed in version catalog. Do not use this value.",
    replaceWith = ReplaceWith("libs.versions.agp"),
)
val Project.androidBuildGradleVersion get() = findProperty("androidBuildGradleVersion") ?: BuildVersions.ANDROID_BUILD

@Deprecated(
    message = "Versions are will be managed in version catalog. Do not use this value.",
    replaceWith = ReplaceWith("libs.versions.kotlinx.coroutines"),
)
val Project.coroutinesVersion get() = findProperty("coroutinesVersion") ?: BuildVersions.COROUTINES

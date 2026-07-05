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
    replaceWith = ReplaceWith("libs.versions.agp"),
)
val Project.androidBuildGradleVersion get() = findProperty("androidBuildGradleVersion") ?: BuildVersions.ANDROID_BUILD

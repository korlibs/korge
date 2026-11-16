plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.multiplatform.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.vanniktech.mavenPublish) apply false
    alias(libs.plugins.kotlinx.benchmark) apply false
    alias(libs.plugins.kotlin.allopen) apply false
}

val resolvedVersion: String = System.getenv("FORCED_VERSION").let { forced ->
    when {
        forced.isNullOrBlank() -> libs.versions.korge.get()
        forced.startsWith("refs/tags/v") -> forced.removePrefix("refs/tags/v")
        forced.startsWith("refs/tags/") -> forced.removePrefix("refs/tags/")
        forced.startsWith("refs/heads/") -> libs.versions.korge.get()
        else -> forced
    }
}
version = resolvedVersion

allprojects {
    version = rootProject.version
    repositories {
        mavenLocal()
        maven {
            name = "Central Portal Snapshots"
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
            content {
                // Only consume org.korge.korlibs snapshots
                includeGroup("org.korge.korlibs")
            }
        }
        mavenCentral()
        google()
    }
}

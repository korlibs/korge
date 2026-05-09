import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlinx.kover)
    alias(libs.plugins.dokka)
    alias(libs.plugins.vanniktech.mavenPublish)
}

description = "Korge IPC – inter-process communication utilities for the Korge game engine"
group = "org.korge.engine"
version = rootProject.libs.versions.korge.get()

mavenPublishing {
    configure(JavaLibrary(javadocJar = JavadocJar.Empty(), sourcesJar = true))
    publishToMavenCentral()

    coordinates(group.toString(), "korge-ipc", version.toString())

    pom {
        name.set("korge-ipc")
        description.set("Korge IPC – inter-process communication utilities for the Korge game engine")
        url.set("https://github.com/korlibs/korge")
        licenses {
            license {
                name.set("MIT")
                url.set("https://raw.githubusercontent.com/korlibs/korge/main/LICENSE")
            }
        }
        developers {
            developer {
                id.set("korge")
                name.set("KorGE Team")
                email.set("info@korge.org")
            }
        }
        scm {
            url.set("https://github.com/korlibs/korge")
        }
    }
}

kotlin {
    @OptIn(ExperimentalAbiValidation::class)
    abiValidation {
        enabled.set(true)
    }
}

java {
    setSourceCompatibility(libs.versions.javaSourceCompatibility.get())
    setTargetCompatibility(libs.versions.javaTargetCompatibility.get())
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.bundles.kotlin.test)
}

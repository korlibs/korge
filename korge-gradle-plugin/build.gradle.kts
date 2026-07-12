plugins {
    id("java")
    id("java-gradle-plugin")
    `kotlin-dsl`
    alias(libs.plugins.vanniktech.mavenPublish)
}

description = "Multiplatform Game Engine written in Kotlin"
group = "org.korge.gradleplugins"
version = libs.versions.korge.get()

// Pin the published bytecode/metadata to the project's target Java version so the plugin is
// consumable by builds running that JVM, regardless of which JDK performs the publish.
java {
    setSourceCompatibility(libs.versions.javaSourceCompatibility.get())
    setTargetCompatibility(libs.versions.javaTargetCompatibility.get())
}

gradlePlugin {
    website.set("https://korge.org/")
    vcsUrl.set("https://github.com/korlibs/korge")
    //tags = ["korge", "game", "engine", "game engine", "multiplatform", "kotlin"]

    plugins {
        register("korge") {
            id = "org.korge.engine"
            displayName = "Korge Game Engine"
            description = "Multiplatform Game Engine for Kotlin"
            implementationClass = "korlibs.korge.gradle.KorgeGradlePlugin"
        }

        register("korge-library") {
            id = "org.korge.engine.library"
            displayName = "Korge Library"
            description = "Multiplatform Game Engine for Kotlin"
            implementationClass = "korlibs.korge.gradle.KorgeLibraryGradlePlugin"
        }
    }
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin.api)
    implementation(libs.kotlin.gradle.dsl)
    implementation(libs.kotlin.serialization)

    implementation(libs.proguard.gradle)
    implementation(libs.gson)
    implementation(libs.vanniktech.maven.publish)

    implementation(libs.kotlinx.kover.gradle.plugin)
    implementation(libs.dokka)

    implementation(libs.android.build.gradle)

    implementation(gradleApi())
    implementation(localGroovy())

    testImplementation(libs.bundles.kotlin.test)
}

// Ensure the vanniktech maven-publish extension creates the Maven Central publish tasks
mavenPublishing {
    publishToMavenCentral()
}

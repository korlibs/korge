plugins {
    id("java")
    id("java-gradle-plugin")
    `kotlin-dsl`
    alias(libs.plugins.vanniktech.mavenPublish)
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

description = "Multiplatform Game Engine written in Kotlin"
group = "org.korge.gradleplugins"
version = resolvedVersion

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
    // Only sign when a key is available (e.g. not for publishToMavenLocal on CI)
    val hasSigningKey = project.providers.gradleProperty("signingInMemoryKey").isPresent ||
        System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKey") != null
    if (hasSigningKey) {
        signAllPublications()
    }

    coordinates(group.toString(), "korge-gradle-plugin", version.toString())

    pom {
        name.set("korge-gradle-plugin")
        description.set("Korge Gradle Plugin – Multiplatform Game Engine Gradle plugin")
        inceptionYear.set("2020")
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
            connection.set("scm:git:https://github.com/korlibs/korge.git")
            developerConnection.set("scm:git:git@github.com:korlibs/korge.git")
            url.set("https://github.com/korlibs/korge")
        }
    }
}

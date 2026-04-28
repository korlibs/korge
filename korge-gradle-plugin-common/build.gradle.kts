import korlibs.*
import korlibs.korge.gradle.targets.android.*
import korlibs.root.*
import org.gradle.kotlin.dsl.kotlin

plugins {
    id("java")
    id("maven-publish")
    id("com.gradle.plugin-publish")
    id("org.jetbrains.kotlin.jvm")
    id("com.github.gmazzo.buildconfig") version "5.3.5"
    id("com.vanniktech.maven.publish")
}

description = "Multiplatform Game Engine written in Kotlin"
group = RootKorlibsPlugin.KORGE_GRADLE_PLUGIN_GROUP

dependencies {
    //implementation(gradleApi())
    //implementation(localGroovy())
    //implementation("org.eclipse.jgit:org.eclipse.jgit:6.3.0.202209071007-r")
    implementation(libs.jgit)
    //implementation(libs.korlibs.serialization)
    testImplementation(libs.bundles.kotlin.test)
}

val jversion = GRADLE_JAVA_VERSION_STR

java {
    setSourceCompatibility(jversion)
    setTargetCompatibility(jversion)
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class).all {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(jversion))
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.fromVersion("2.1"))
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.fromVersion("2.1"))
    }
}

val srcgen = File(project.buildDir, "srcgen")
kotlin.sourceSets.maybeCreate("main").kotlin.srcDirs(srcgen)
val KProjectVersionKt = File(srcgen, "KProjectVersion.kt")
val KProjectVersionContent = """
package org.korge.kproject.version

object KProjectVersion {
    val VERSION = "${version}"
}
"""
if (!KProjectVersionKt.exists() || KProjectVersionKt.text != KProjectVersionContent) {
    srcgen.mkdirs()
    KProjectVersionKt.text = KProjectVersionContent
}

mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL, automaticRelease = false)

    coordinates(group.toString(), "korge-gradle-plugin-common", version.toString())

    pom {
        name.set("korge-gradle-plugin-common")
        description.set("Multiplatform Game Engine written in Kotlin – Gradle Plugin Common")
        url.set("https://github.com/korlibs/korge")
        licenses {
            license {
                name.set("MIT")
                url.set("https://raw.githubusercontent.com/korlibs/korge/master/LICENSE")
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

korlibs.NativeTools.groovyConfigureSigning(project)

tasks {
    val publishJvmPublicationToMavenLocal = creating(Task::class) {
        group = "publishing"
        dependsOn("publishToMavenLocal")
    }
}

afterEvaluate {
    if (tasks.findByName("publishAllPublicationsToMavenRepository") != null) {
        tasks.register("publishJvmPublicationToMavenRepository", Task::class) {
            group = "publishing"
            dependsOn("publishAllPublicationsToMavenRepository")
        }
    }
}

tasks { val jvmTest by creating { dependsOn("test") } }

buildConfig {
    packageName("korlibs.korge.gradle.common")
    buildConfigField("String", "KOTLIN_PLUGIN_VERSION", "\"${project.version}\"")
}

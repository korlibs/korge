import korlibs.*
import korlibs.korge.gradle.targets.android.*
import korlibs.root.*
import org.gradle.kotlin.dsl.kotlin

plugins {
    id("java")
    id("java-gradle-plugin")
    id("maven-publish")
    id("com.gradle.plugin-publish")
    id("org.jetbrains.kotlin.jvm")
}

description = "Multiplatform Game Engine written in Kotlin"
group = RootKorlibsPlugin.KORGE_GRADLE_PLUGIN_GROUP

dependencies {
    //implementation(gradleApi())
    //implementation(localGroovy())
    //implementation("org.eclipse.jgit:org.eclipse.jgit:6.3.0.202209071007-r")
    implementation(libs.jgit)
}

val jversion = GRADLE_JAVA_VERSION_STR

java {
    setSourceCompatibility(jversion)
    setTargetCompatibility(jversion)
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class).all {
    kotlinOptions {
        jvmTarget = jversion
        apiVersion = "1.7"
        languageVersion = "1.7"
    }
}

val srcgen = File(project.buildDir, "srcgen")
kotlin.sourceSets.maybeCreate("main").kotlin.srcDirs(srcgen)
val KProjectVersionKt = File(srcgen, "KProjectVersion.kt")
val KProjectVersionContent = """
package com.soywiz.kproject.version

object KProjectVersion {
    val VERSION = "${version}"
}
"""
if (!KProjectVersionKt.exists() || KProjectVersionKt.text != KProjectVersionContent) {
    srcgen.mkdirs()
    KProjectVersionKt.text = KProjectVersionContent
}

korlibs.NativeTools.groovyConfigurePublishing(project, false)
korlibs.NativeTools.groovyConfigureSigning(project)

val publishJvmPublicationToMavenLocal = tasks.register("publishJvmPublicationToMavenLocal", Task::class) {
    group = "publishing"
    dependsOn("publishMavenPublicationToMavenLocal")
}

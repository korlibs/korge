import korlibs.korge.gradle.targets.android.*
import korlibs.root.*

plugins {
    //id "kotlin" version "1.6.21"
    id("kotlin")
    //id "org.jetbrains.kotlin.jvm"
    id("maven-publish")
    //alias(libs.plugins.conventions.jvm)
    //alias(libs.plugins.compiler.specific.module)
    id("com.github.gmazzo.buildconfig") version "5.3.5"
}

//name = "korge-kotlin-plugin"
description = "Multiplatform Game Engine written in Kotlin"
group = RootKorlibsPlugin.KORGE_RELOAD_AGENT_GROUP

val jversion = GRADLE_JAVA_VERSION_STR

java {
    setSourceCompatibility(jversion)
    setTargetCompatibility(jversion)
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class).all {
    kotlinOptions {
        jvmTarget = jversion
        apiVersion = "1.8"
        languageVersion = "1.8"
        suppressWarnings = true
    }
}

publishing {
    publications {
        val maven by creating(MavenPublication::class) {
            groupId = group.toString()
            artifactId = "korge-kotlin-plugin"
            version = version
            from(components["kotlin"])
        }
    }
}

val publishJvmPublicationToMavenLocal = tasks.register("publishJvmPublicationToMavenLocal", Task::class) {
    group = "publishing"
    dependsOn("publishMavenPublicationToMavenLocal")
}

afterEvaluate {
    if (tasks.findByName("publishMavenPublicationToMavenRepository") != null) {
        tasks.register("publishJvmPublicationToMavenRepository", Task::class) {
            group = "publishing"
            dependsOn("publishMavenPublicationToMavenRepository")
        }
    }
}

korlibs.NativeTools.groovyConfigurePublishing(project, false)
korlibs.NativeTools.groovyConfigureSigning(project)

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable")
    testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable")
    testImplementation(libs.bundles.kotlin.test)
}

tasks { val jvmTest by creating { dependsOn("test") } }

buildConfig {
    packageName("korlibs.korge.kotlin.plugin")
    buildConfigField("String", "KOTLIN_PLUGIN_ID", "\"com.soywiz.korge.korge-kotlin-plugin\"")
}

afterEvaluate {
    tasks.getByName("sourceJar").dependsOn("generateBuildConfig")
}

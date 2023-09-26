import korlibs.korge.gradle.targets.android.*
import korlibs.root.*

plugins {
    //id "kotlin" version "1.6.21"
    id("kotlin")
    //id "org.jetbrains.kotlin.jvm"
    id("maven-publish")
}

description = "Multiplatform Game Engine written in Kotlin"
group = RootKorlibsPlugin.KORGE_RELOAD_AGENT_GROUP

tasks.jar {
    manifest {
        attributes(
            "Agent-Class" to "korlibs.korge.reloadagent.KorgeReloadAgent",
            "Premain-Class" to "korlibs.korge.reloadagent.KorgeReloadAgent",
            "Can-Redefine-Classes" to true,
            "Can-Retransform-Classes" to true
        )
    }
}

val jversion = GRADLE_JAVA_VERSION_STR

java {
    setSourceCompatibility(jversion)
    setTargetCompatibility(jversion)
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class).all {
    kotlinOptions {
        jvmTarget = jversion
        apiVersion = "1.6"
        languageVersion = "1.6"
        suppressWarnings = true
    }
}

publishing {
    publications {
        maybeCreate("maven", MavenPublication::class).apply {
            groupId = group.toString()
            artifactId = "korge-reload-agent"
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

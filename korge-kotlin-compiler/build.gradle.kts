import korlibs.korge.gradle.targets.android.*
import korlibs.root.*

plugins {
    //id "kotlin" version "1.6.21"
    id("kotlin")
    //id "org.jetbrains.kotlin.jvm"
    id("maven-publish")
    //alias(libs.plugins.conventions.jvm)
    //alias(libs.plugins.compiler.specific.module)
}

//name = "korge-kotlin-plugin"
description = "Multiplatform Game Engine written in Kotlin"
group = RootKorlibsPlugin.KORGE_GROUP

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
            artifactId = project.name
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
    implementation("org.jetbrains.kotlin:kotlin-build-tools-impl")
    compileOnly("org.jetbrains.kotlin:kotlin-build-tools-api")
    //api("org.jetbrains.kotlin:kotlin-compiler-embeddable")
    //api("org.jetbrains.kotlin:kotlin-compiler-client-embeddable")
    //api("org.jetbrains.kotlin:kotlin-daemon-embeddable")
    //api("org.jetbrains.kotlin:kotlin-gradle-plugin")
    testImplementation(libs.bundles.kotlin.test)
}

tasks { val jvmTest by creating { dependsOn("test") } }

plugins {
    //id "kotlin" version "1.6.21"
    id("kotlin")
    //id "org.jetbrains.kotlin.jvm"
    id("maven-publish")
}

description = "Multiplatform Game Engine written in Kotlin"
group = "com.soywiz.korlibs.korge.reloadagent"

tasks {
    jar {
        manifest {
            attributes(mapOf(
                "Agent-Class" to "korlibs.korge.reloadagent.KorgeReloadAgent",
                "Premain-Class" to "korlibs.korge.reloadagent.KorgeReloadAgent",
                "Can-Redefine-Classes" to true,
                "Can-Retransform-Classes" to true,
            ))
        }
    }
}

val jversion = libs.versions.javaVersion
//def jversion = korlibs.korge.gradle.targets.android.AndroidKt.GRADLE_JAVA_VERSION_STR

java {
    setSourceCompatibility(jversion.get())
    setTargetCompatibility(jversion.get())
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class).all {
    kotlinOptions {
        jvmTarget = jversion.get()
        //sourceCompatibility = "1.8"
        apiVersion = "1.6"
        languageVersion = "1.6"
        suppressWarnings = true
        //jvmTarget = "1.6"
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

afterEvaluate {
    if (tasks.findByName("publishMavenPublicationToMavenRepository") != null) {
        tasks.register("publishJvmPublicationToMavenRepository", Task::class) {
            group = "publishing"
            dependsOn("publishMavenPublicationToMavenRepository")
        }
    }
}

tasks {
    val publishJvmPublicationToMavenLocal by registering {
        group = "publishing"
        dependsOn("publishMavenPublicationToMavenLocal")
    }

    val jvmTest by registering {
        dependsOn("test")
    }

    val publishJvmLocal by creating {
        dependsOn("publishJvmPublicationToMavenLocal")
    }
}


/*
korlibs.NativeTools.groovyConfigurePublishing(project, false)
korlibs.NativeTools.groovyConfigureSigning(project)
*/

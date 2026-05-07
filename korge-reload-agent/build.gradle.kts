import korlibs.korge.gradle.targets.android.*
import korlibs.root.*
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import com.vanniktech.maven.publish.*

plugins {
    id("kotlin")
    id("com.vanniktech.maven.publish")
}

description = "Korge Reload Agent – JVM hot-reload instrumentation agent"
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
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(jversion))
        apiVersion.set(KotlinVersion.fromVersion("2.0"))
        languageVersion.set(KotlinVersion.fromVersion("2.0"))
        suppressWarnings.set(true)
    }
}

mavenPublishing {
    configure(JavaLibrary(javadocJar = JavadocJar.Empty(), sourcesJar = true))
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = false)

    coordinates(group.toString(), "korge-reload-agent", version.toString())

    pom {
        name.set("korge-reload-agent")
        description.set("Korge Reload Agent – JVM hot-reload instrumentation agent")
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

dependencies {
    testImplementation(libs.bundles.kotlin.test)
}

tasks { val jvmTest by creating { dependsOn("test") } }

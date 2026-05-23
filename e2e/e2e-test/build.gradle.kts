import korlibs.korge.gradle.*
import korlibs.korge.gradle.targets.jvm.*
import org.jetbrains.kotlin.gradle.tasks.*
import java.net.*

buildscript {
    val korgePluginVersion: String by project

    repositories {
        mavenLocal {
            content {
                // Load gradle plugin from maven local explicitly in e2e tests
                includeGroup("org.korge.gradleplugins")
            }
        }
        maven { url = uri("https://central.sonatype.com/repository/maven-snapshots") }
        mavenCentral()
        google()
        maven { url = uri("https://plugins.gradle.org/m2/") }
    }
    dependencies {
        classpath("org.korge.gradleplugins:korge-gradle-plugin:$korgePluginVersion")
    }
}

apply<KorgeGradlePlugin>()

korge {
    id = "com.sample.demo"

// To enable all targets at once

    //targetAll()

// To enable targets based on properties/environment variables
    //targetDefault()

// To selectively enable targets

    targetJvm()
    targetJs()
    // TODO Fix gradle plugins to support ios targets easier
    // targetIos()
    targetAndroid()

    jvmMainClassName = "RefMainKt"
}

// Use MESA OpenGL 32 since Windows Server doesn't have a proper OpenGL implementation on GitHub Actions
// @see: https://amiralizadeh9480.medium.com/how-to-run-opengl-based-tests-on-github-actions-60f270b1ea2c
tasks {
    val localOpengl32X64ZipFile = file("opengl32-x64.zip")
    val downloadOpenglMesaForWindows by creating(Task::class) {
        onlyIf { !localOpengl32X64ZipFile.exists() }
        doLast {
            val url = URL("https://github.com/korlibs/mesa-dist-win/releases/download/21.2.3/opengl32-x64.zip")
            localOpengl32X64ZipFile.writeBytes(url.readBytes())
        }
    }
    val unzipOpenglX64 by creating(Task::class) {
        dependsOn(downloadOpenglMesaForWindows)
        doLast {
            if (!file("opengl32.dll").exists()) {
                copy {
                    from(zipTree(localOpengl32X64ZipFile))
                    into(".")
                }
            }
        }
    }

    val runJvm = getByName("runJvm") as KorgeJavaExec
    runJvm.dependsOn(unzipOpenglX64)
    runJvm.workingDir = File(buildDir, "bin/jvm").also { it.mkdirs() }
    runJvm.environment("OUTPUT_DIR", File(buildDir, "screenshots/jvm"))

    val checkReferencesJvm by creating(Task::class) {
        doLast {
            CheckReferences.main(project.projectDir)
        }
        dependsOn("runJvm")
    }

    val updateReferencesJvm by creating(Task::class) {
        doLast {
            CheckReferences.main(project.projectDir, update = true)
        }
        dependsOn("runJvm")
    }

    // e2e-test is not a published library — register no-op stubs so that running
    // publish tasks from this directory (or from a parent build that discovers this
    // project) silently does nothing instead of failing with "task not found".
    listOf(
        "publishToMavenCentral",
        "publishToMavenLocal",
        "publishAllPublicationsToMavenRepository",
        "publishJvmPublicationToMavenRepository",
    ).forEach { taskName ->
        register(taskName) {
            group = "publishing"
            description = "No-op: e2e-test is not a published artifact."
            logger.lifecycle("Skipping '$taskName' for e2e-test – this project is not published.")
        }
    }
}

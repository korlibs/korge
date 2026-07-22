import java.net.URI
import korlibs.korge.gradle.korge
import korlibs.korge.gradle.targets.jvm.KorgeJavaExec

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.korge.application)
}

korge {
    id = "org.korge.e2e.test.jvm"

    targetJvm()

    jvmMainClassName = "org.korge.e2e.jvm.RefMainKt"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared)
            implementation(libs.korge)
        }
    }
}

// TODO Remove this task configuration and make it a CI/CD workflow step instead
// Use MESA OpenGL 32 since Windows Server doesn't have a proper OpenGL implementation on GitHub Actions
// @see: https://amiralizadeh9480.medium.com/how-to-run-opengl-based-tests-on-github-actions-60f270b1ea2c
tasks {
    val localOpengl32X64ZipFile = file("opengl32-x64.zip")
    val downloadOpenglMesaForWindows by registering(Task::class) {
        description = "Download opengl32-x64.zip"
        onlyIf { !localOpengl32X64ZipFile.exists() }
        doLast {
            val url = URI("https://github.com/korlibs/mesa-dist-win/releases/download/21.2.3/opengl32-x64.zip").toURL()
            localOpengl32X64ZipFile.writeBytes(url.readBytes())
        }
    }
    val unzipOpenglX64 by registering(Task::class) {
        description = "Unzip opengl32.dll"
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
    runJvm.workingDir = layout.buildDirectory.dir("bin/jvm").map { it.asFile.mkdirs(); it }.get().asFile
    runJvm.environment("OUTPUT_DIR", layout.buildDirectory.dir("screenshots/jvm").get().asFile)

    registering(Task::class) {
        description = "Check references"
        doLast {
            CheckReferences.main(project.projectDir)
        }
        dependsOn("runJvm")
    }

    registering(Task::class) {
        description = "Check and update references"
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

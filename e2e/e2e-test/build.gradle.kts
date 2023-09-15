import korlibs.korge.gradle.*
import korlibs.korge.gradle.targets.jvm.*
import org.jetbrains.kotlin.gradle.tasks.*
import java.net.*

buildscript {
    val korgePluginVersion: String by project

    repositories {
        mavenLocal()
        mavenCentral()
        google()
        maven { url = uri("https://plugins.gradle.org/m2/") }
        maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap") }
        maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/temporary") }
        maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev") }
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven") }
        maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental") }
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }

    }
    dependencies {
        classpath("com.soywiz.korlibs.korge.plugins:korge-gradle-plugin:$korgePluginVersion")
    }
}

apply<KorgeGradlePlugin>()

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

korge {
    id = "com.sample.demo"

// To enable all targets at once

    //targetAll()

// To enable targets based on properties/environment variables
    //targetDefault()

// To selectively enable targets

    targetJvm()
    targetJs()
    targetIos()
    //targetAndroidIndirect() // targetAndroidDirect()
    targetAndroid()

    jvmMainClassName = "RefMainKt"

    //entrypoint("CheckReferences", "CheckReferences")
    //entrypoint("HelloWorld", "HelloWorld")
}

//println(BuildVersions.KORGE)

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

    val runJvm = getByName("runJvm") as KorgeJavaExec
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
}

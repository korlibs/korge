import com.soywiz.korge.gradle.*
import com.soywiz.korge.gradle.targets.jvm.*
import org.jetbrains.kotlin.gradle.tasks.*
import java.net.*

buildscript {
    val korgePluginVersion: String by project

    repositories {
        mavenLocal()
        mavenCentral()
        google()
        maven { url = uri("https://plugins.gradle.org/m2/") }
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

//	    useNewMemoryModel = true

// To enable all targets at once

    //targetAll()

// To enable targets based on properties/environment variables
    //targetDefault()

// To selectively enable targets

    targetJvm()
    targetJs()
    targetDesktop()
    targetIos()
    targetAndroidIndirect() // targetAndroidDirect()

    enableKorgeResourceProcessing = false

    //entrypoint("CheckReferences", "CheckReferences")
    //entrypoint("HelloWorld", "HelloWorld")
}

//println(BuildVersions.KORGE)

// Use MESA OpenGL 32 since Windows Server doesn't have a proper OpenGL implementation on GitHub Actions
// @see: https://amiralizadeh9480.medium.com/how-to-run-opengl-based-tests-on-github-actions-60f270b1ea2c
tasks {
    val localOpengl32X64ZipFile = file("opengl32-x64.zip")
    val linkDebugExecutableMingwX64 = findByName("linkDebugExecutableMingwX64") as? KotlinNativeLink?
    val downloadOpenglMesaForWindows by creating(Task::class) {
        onlyIf { !localOpengl32X64ZipFile.exists() }
        doLast {
            val url = URL("https://github.com/korlibs/mesa-dist-win/releases/download/21.2.3/opengl32-x64.zip")
            localOpengl32X64ZipFile.writeBytes(url.readBytes())
        }
    }
    if (linkDebugExecutableMingwX64 != null) {
        val upzipOpenglMingwX64 by creating(Copy::class) {
            dependsOn(downloadOpenglMesaForWindows)
            from(zipTree(localOpengl32X64ZipFile))
            into(linkDebugExecutableMingwX64.binary.outputDirectory)
        }

        linkDebugExecutableMingwX64.dependsOn(upzipOpenglMingwX64)
    }
    val runJvm = getByName("runJvm") as KorgeJavaExec
    runJvm.workingDir = File(buildDir, "bin/jvm").also { it.mkdirs() }
    runJvm.environment("OUTPUT_DIR", File(buildDir, "screenshots/jvm"))

    val checkReferencesNative by creating(Task::class) {
        doLast {
            CheckReferences.main(project.projectDir)
        }
    }

    val checkReferencesJvm by creating(Task::class) {
        doLast {
            CheckReferences.main(project.projectDir)
        }
        dependsOn("runJvm")
    }

    afterEvaluate {
        val isArm = com.soywiz.kmem.Platform.arch == com.soywiz.kmem.Arch.ARM64
        for (target in listOf("mingwX64", "linuxX64", if (isArm) "macosArm64" else "macosX64")) {
        //for (target in listOf("mingwX64", "linuxX64", "macosX64")) {
            val runTask = (findByName("runNative${target.capitalize()}Debug") as? Exec?) ?: continue
            runTask.environment("OUTPUT_DIR", File(buildDir, "screenshots/${target.toLowerCase()}"))
            checkReferencesNative.dependsOn(runTask)
        }
    }
}

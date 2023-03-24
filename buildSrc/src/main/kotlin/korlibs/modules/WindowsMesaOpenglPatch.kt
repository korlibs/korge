package korlibs.modules

import korlibs.korge.gradle.util.*
import org.gradle.api.*
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.tasks.*
import java.io.*
import java.net.*

val Project.localOpengl32X64ZipFile: File get() = File(rootProject.buildDir, "opengl32-x64.zip")

fun Project.downloadOpenglMesaForWindows(): Task {
    if (rootProject.tasks.findByName("downloadOpenglMesaForWindows") == null) {
        rootProject.tasks.createThis<Task>("downloadOpenglMesaForWindows") {
            onlyIf { !localOpengl32X64ZipFile.exists() }
            doLast {
                val url = URL("https://github.com/korlibs/mesa-dist-win/releases/download/21.2.3/opengl32-x64.zip")
                localOpengl32X64ZipFile.writeBytes(url.readBytes())
            }
        }
    }
    return rootProject.tasks.findByName("downloadOpenglMesaForWindows")!!
}

fun Project.attachMesaOpenglPatchToLink(linkTask: KotlinNativeLink) {
    val unzipOpenglMingwX64 = tasks.createThis<Copy>("${linkTask.name}UnzipOpenglMingwX64") {
        dependsOn(downloadOpenglMesaForWindows())
        from(zipTree(localOpengl32X64ZipFile))
        into(linkTask.binary.outputDirectory)
    }

    linkTask.dependsOn(unzipOpenglMingwX64)
}

fun Project.configureMingwX64TestWithMesa() {
    //val mingwX64Test = tasks.findByName("mingwX64Test")
    val linkTask = tasks.findByName("linkDebugTestMingwX64") as? KotlinNativeLink?
    //println("linkTask=$linkTask")
    linkTask?.let { attachMesaOpenglPatchToLink(it) }
}
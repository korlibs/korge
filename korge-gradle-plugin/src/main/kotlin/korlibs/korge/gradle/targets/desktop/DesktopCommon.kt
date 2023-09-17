package korlibs.korge.gradle.targets.desktop

import korlibs.korge.gradle.gkotlin
import korlibs.korge.gradle.korge
import korlibs.korge.gradle.kotlin
import korlibs.korge.gradle.targets.isArm
import korlibs.korge.gradle.targets.isLinux
import korlibs.korge.gradle.targets.isMacos
import korlibs.korge.gradle.targets.isWindows
import korlibs.korge.gradle.util.Indenter
import korlibs.korge.gradle.util.createOnce
import org.gradle.api.Project
import org.gradle.api.Task
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinNativeTargetPreset
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTestsPreset
import java.io.File

val Project.prepareKotlinNativeBootstrap: Task get() = tasks.createOnce("prepareKotlinNativeBootstrap") {
    val output = nativeDesktopBootstrapFile
    outputs.file(output)
    doLast {
        output.parentFile.mkdirs()

        val text = Indenter {
            //line("package korge.bootstrap")
            line("import ${korge.realEntryPoint}")
            line("fun main(args: Array<String> = arrayOf()): Unit = RootGameMain.runMain(args)")
            line("object RootGameMain") {
                line("fun runMain() = runMain(arrayOf())")
                line("@Suppress(\"UNUSED_PARAMETER\") fun runMain(args: Array<String>): Unit = korlibs.io.Korio { ${korge.realEntryPoint}() }")
            }
        }
        if (!output.exists() || output.readText() != text) output.writeText(text)
    }
}

val Project.nativeDesktopBootstrapFile get() = File(buildDir, "platforms/native-desktop/bootstrap.kt")

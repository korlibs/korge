package korlibs.korge.gradle.targets.desktop

import korlibs.korge.gradle.*
import korlibs.korge.gradle.util.*
import org.gradle.api.*
import java.io.*

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

package korlibs.korge.gradle.targets.desktop

import korlibs.korge.gradle.*
import korlibs.korge.gradle.util.*
import org.gradle.api.*
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.*

open class PrepareKotlinNativeBootstrapTask : DefaultTask() {
    @get:OutputFile
    val output = project.nativeDesktopBootstrapFile

    private var realEntryPoint: String = "InvalidClass"

    init {
        project.afterEvaluate {
            realEntryPoint = project.korge.realEntryPoint
        }
    }

    @TaskAction
    fun run() {
        output.parentFile.mkdirs()

        val text = Indenter {
            //line("package korge.bootstrap")
            line("import $realEntryPoint")
            line("fun main(args: Array<String> = arrayOf()): Unit = RootGameMain.runMain(args)")
            line("object RootGameMain") {
                line("fun runMain() = runMain(arrayOf())")
                line("@Suppress(\"UNUSED_PARAMETER\") fun runMain(args: Array<String>): Unit = korlibs.io.Korio { ${realEntryPoint}() }")
            }
        }
        if (!output.exists() || output.readText() != text) output.writeText(text)
    }
}

val Project.prepareKotlinNativeBootstrap: Task get() {
    val taskName = "prepareKotlinNativeBootstrap"
    return tasks.findByName(taskName) ?: tasks.createThis<PrepareKotlinNativeBootstrapTask>(taskName)
}

val Project.nativeDesktopBootstrapFile get() = File(buildDir, "platforms/native-desktop/bootstrap.kt")

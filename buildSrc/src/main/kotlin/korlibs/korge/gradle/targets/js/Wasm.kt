package korlibs.korge.gradle.targets.js

import korlibs.korge.gradle.gkotlin
import korlibs.korge.gradle.kotlin
import korlibs.korge.gradle.targets.*
import korlibs.korge.gradle.targets.wasm.*
import korlibs.korge.gradle.util.*
import org.gradle.api.*
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.targets.js.npm.*
import java.io.*

fun Project.configureWasm(projectType: ProjectType, binaryen: Boolean = false) {
    if (gkotlin.targets.findByName("wasm") != null) return

    configureWasmTarget(executable = true, binaryen = binaryen)

    if (projectType.isExecutable) {

        val wasmJsCreateIndex = project.tasks.createThis<WasmJsCreateIndexTask>("wasmJsCreateIndex") {
        }
        //:compileDevelopmentExecutableKotlinWasmJs
        //project.tasks.findByName("wasmJsBrowserDevelopmentRun")?.apply {
        project.tasks.findByName("compileDevelopmentExecutableKotlinWasmJs")?.apply {
            dependsOn(wasmJsCreateIndex)
        }
        project.tasks.findByName("compileProductionExecutableKotlinWasmJs")?.apply {
            dependsOn(wasmJsCreateIndex)
        }
        project.tasks.createThis<Task>("runWasmJs") {
            dependsOn("wasmJsRun")
        }
    }
}

open class WasmJsCreateIndexTask : DefaultTask() {
    private val npmDir: File = project.kotlin.wasmJs().compilations["main"]!!.npmProject.dir

    @TaskAction
    fun run() {
        File(npmDir, "kotlin/index.html").also { it.parentFile.mkdirs() }.writeText(
            """
            <html>
                <script type = 'module'>
                    import module from "./${npmDir.name}.mjs"
                    console.log(module)
                    //instantiate();
                </script>
            </html>
        """.trimIndent()
        )
    }
}

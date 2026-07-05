package korlibs.korge.gradle.targets.js

import java.io.File
import korlibs.korge.gradle.gkotlin
import korlibs.korge.gradle.kotlin
import korlibs.korge.gradle.targets.ProjectType
import korlibs.korge.gradle.targets.jvm.ensureSourceSetsConfigure
import korlibs.korge.gradle.targets.wasm.configureWasmTarget
import korlibs.korge.gradle.util.createThis
import korlibs.korge.gradle.util.get
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject

fun Project.configureWasm(projectType: ProjectType, binaryen: Boolean = false) {
    if (gkotlin.targets.findByName("wasm") != null) return
    ensureSourceSetsConfigure("common", "wasmJs")

    configureWasmTarget(executable = true, binaryen = binaryen)

    if (projectType.isExecutable) {

        val wasmJsCreateIndex = project.tasks.createThis<WasmJsCreateIndexTask>("wasmJsCreateIndex") {
        }
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

@DisableCachingByDefault
open class WasmJsCreateIndexTask : DefaultTask() {
    private val npmDir: File = project.kotlin.wasmJs().compilations["main"].npmProject.dir.get().asFile

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

package korlibs.korge.gradle.targets.js

import korlibs.korge.gradle.gkotlin
import korlibs.korge.gradle.kotlin
import korlibs.korge.gradle.targets.*
import korlibs.korge.gradle.targets.wasm.*
import korlibs.korge.gradle.util.*
import org.gradle.api.*
import org.jetbrains.kotlin.gradle.targets.js.npm.*
import java.io.*

fun Project.configureWasm(projectType: ProjectType, binaryen: Boolean = false) {
    if (gkotlin.targets.findByName("wasm") != null) return

    configureWasm(executable = true, binaryen = binaryen)

    if (projectType.isExecutable) {

        project.tasks.createThis<Task>("wasmJsCreateIndex") {
            doFirst {
                wasmJsCreateIndex(project)
            }
        }
        //:compileDevelopmentExecutableKotlinWasmJs
        project.tasks.findByName("wasmJsBrowserDevelopmentRun")?.apply {
            dependsOn("wasmJsCreateIndex")
            doFirst { wasmJsCreateIndex(project) }
        }
        project.tasks.createThis<Task>("runWasmJs") {
            dependsOn("wasmJsRun")
        }
    }
}

fun wasmJsCreateIndex(project: Project) {
    val compilation = project.kotlin.wasm().compilations["main"]!!
    val npmDir = compilation.npmProject.dir
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

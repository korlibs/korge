package korlibs.korge.gradle.util

import org.gradle.api.*
import java.io.*

open class SpawnExtension {
    open fun spawn(dir: File, command: List<String>) {
        ProcessBuilder(command).inheritIO().redirectErrorStream(true).directory(dir).start()
    }
    open fun execLogger(projectDir: File, vararg params: String, filter: Process.(line: String) -> String? = { it }) {
        println("EXEC: ${params.joinToString(" ")}")
        val process = ProcessBuilder(*params).redirectErrorStream(true).directory(projectDir).start()
        try {
            val reader = process.inputStream.reader()
            reader.forEachLine {
                filter(process, it)?.let { println(it) }
            }
        } catch (e: IOException) {
            // Steam closed is fine if the filter closed the process
        }
        process.waitFor()
    }

    open fun execOutput(projectDir: File, vararg params: String): String {
        return ProcessBuilder(*params).redirectErrorStream(true).directory(projectDir).start().inputStream.readBytes().toString(Charsets.UTF_8)
    }
}

var Project.spawnExt: SpawnExtension by projectExtension { SpawnExtension() }

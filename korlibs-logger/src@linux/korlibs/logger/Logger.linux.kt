@file:OptIn(ExperimentalForeignApi::class)

package korlibs.logger

import kotlinx.cinterop.*

actual object Console : BaseConsole()

actual object DefaultLogOutput : Logger.Output {
    override fun output(logger: Logger, level: Logger.Level, msg: Any?) = Logger.ConsoleLogOutput.output(logger, level, msg)
}

internal actual val miniEnvironmentVariables: Map<String, String> by lazy {
    getEnvs()
}

private fun getEnvs(): Map<String, String> {
    val out = LinkedHashMap<String, String>()
    val env = platform.posix.__environ
    var n = 0
    while (true) {
        val line = env?.get(n++)?.toKString()
        if (line == null || line.isNullOrBlank()) break
        val parts = line.split('=', limit = 2)
        out[parts[0]] = parts.getOrElse(1) { parts[0] }
    }
    return out
}

@file:OptIn(ExperimentalForeignApi::class)

package korlibs.logger

import kotlinx.cinterop.*
import platform.windows.*

actual object Console : BaseConsole()

actual object DefaultLogOutput : Logger.Output {
    actual override fun output(logger: Logger, level: Logger.Level, msg: Any?) = Logger.ConsoleLogOutput.output(logger, level, msg)
}

internal actual val miniEnvironmentVariables: Map<String, String> by lazy { getEnvs() }

private fun readStringsz(ptr: CPointer<WCHARVar>?): List<String> {
    if (ptr == null) return listOf()
    var n = 0
    var lastP = 0
    val out = arrayListOf<String>()
    while (true) {
        val c: Int = ptr[n++].toInt()
        if (c == 0) {
            val startPtr: CPointer<WCHARVar> = (ptr + lastP)!!
            val str = startPtr.toKString()
            if (str.isEmpty()) break
            out += str
            lastP = n
        }
    }
    return out
}

private fun getEnvs(): Map<String, String> {
    val envs = GetEnvironmentStringsW() ?: return mapOf()
    val lines = readStringsz(envs)
    FreeEnvironmentStringsW(envs)
    return lines.map {
        val parts = it.split('=', limit = 2)
        parts[0] to parts.getOrElse(1) { parts[0] }
    }.toMap()
}

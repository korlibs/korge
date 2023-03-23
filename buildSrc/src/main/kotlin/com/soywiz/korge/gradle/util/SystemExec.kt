package com.soywiz.korge.gradle.util

import java.io.*

fun executeSystemCommand(command: Array<String>, cwd: File? = null, envs: Map<String, String>? = null): SystemExecResult {
    val output = StringBuilder()
    var exitCode = -1
    val processBuilder = ProcessBuilder(*command)
    cwd?.let { processBuilder.directory(it) }
    envs?.let { processBuilder.environment().putAll(it) }
    processBuilder.redirectErrorStream(true)
    val process = processBuilder.start()
    BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            output.append(line).append("\n")
            println(line)
        }
        exitCode = process.waitFor()
    }
    return SystemExecResult(exitCode, output.toString())
}
class SystemExecResult(var exitCode: Int, var stdout: String) {
    val success get() = exitCode == 0
    val failed get() = !success
}

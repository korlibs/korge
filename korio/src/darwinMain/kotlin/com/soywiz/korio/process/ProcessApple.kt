package com.soywiz.korio.process

import com.soywiz.kds.concurrent.*
import com.soywiz.klock.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.posix.*

// @TODO: Use a separate thread
actual suspend fun posixExec(
    path: String, cmdAndArgs: List<String>, env: Map<String, String>, handler: VfsProcessHandler
): Int {
    println("@WARNING: this exec implementation is not setting env variables, or the current path, and not reading stderr")

    // @TODO: place environment variables like ENV=value ENV2=value2 cd path; command
    // @TODO: does it work on windows? only posix?
    val commandLine = ShellArgs.buildShellExecCommandLineForPopen(cmdAndArgs)

    //println("[MAIN] BEFORE WORKER: commandLine=$commandLine")

    val stdoutQueue = ConcurrentDeque<ByteArray>()
    val stderrQueue = ConcurrentDeque<ByteArray>()
    val resultQueue = ConcurrentDeque<Int>()
    withContext(Dispatchers.CIO) {
        memScoped {
            val f = popen(commandLine, "r")
            //println("[WORKER] OPENED ${info.commandLine}")
            val temp = ByteArray(1024)
            temp.usePinned { pin ->
                val tempAddress = pin.addressOf(0)
                while (true) {
                    val result = fread(tempAddress, 1, temp.size.convert(), f).toInt()
                    //println("[WORKER] fread result $result")
                    if (result <= 0) break
                    stdoutQueue.add(temp.copyOf(result))
                }
            }
            val exitCode = pclose(f)
            //println("[WORKER] pclose $exitCode")
            resultQueue.add(exitCode)
        }
    }

    var exitCode: Int? = null

    //println("[MAIN] START WAIT")

    while (exitCode == null) {
        stdoutQueue.consume()?.let {
            //println("[MAIN] ON OUT: ${it.size}")
            handler.onOut(it)
        }
        stderrQueue.consume()?.let {
            //println("[MAIN] ON ERR: ${it.size}")
            handler.onErr(it)
        }
        exitCode = resultQueue.consume()
        if (exitCode != null) {
            //println("[MAIN] ON EXIT: $exitCode")
        }
        delay(1.milliseconds)
    }

    //println("[MAIN] END WAIT")

    //println("[MAIN] END WAIT 2")
    return exitCode
}

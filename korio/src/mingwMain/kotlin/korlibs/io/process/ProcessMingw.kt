package korlibs.io.process

import korlibs.datastructure.concurrent.*
import korlibs.time.*
import korlibs.io.async.*
import korlibs.io.file.*
import korlibs.io.file.std.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.posix.*
import kotlin.native.concurrent.*

actual suspend fun posixExec(
    path: String, cmdAndArgs: List<String>, env: Map<String, String>, handler: VfsProcessHandler
): Int {
    println("@WARNING: this exec implementation is not escaping, not setting env variables, or the current path, and not reading stderr")

    // @TODO: place environment variables like ENV=value ENV2=value2 cd path; command
    // @TODO: does it work on windows? only posix?
    val commandLine = ShellArgs.buildShellExecCommandLineForPopen(cmdAndArgs)

    //println("[MAIN] BEFORE WORKER: commandLine=$commandLine")

    val stdoutQueue = ConcurrentDeque<ByteArray>()
    val stderrQueue = ConcurrentDeque<ByteArray>()
    val resultQueue = ConcurrentDeque<Int>()

    withContext(Dispatchers.CIO) {
        memScoped {
            val f = _popen(commandLine, "r")
            //println("[WORKER] OPENED ${info.commandLine}")
            val temp = ByteArray(1024)
            temp.usePinned { pin ->
                val tempAddress = pin.addressOf(0)
                while (true) {
                    val result = fread(tempAddress, 1, temp.size.convert(), f).toLong()
                    //println("[WORKER] fread result $result")
                    if (result <= 0L) break
                    stdoutQueue.add(temp.copyOf(result.toInt()))
                }
            }
            val exitCode = _pclose(f)
            //println("[WORKER] pclose $exitCode")
            resultQueue.add(exitCode)
            Unit
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
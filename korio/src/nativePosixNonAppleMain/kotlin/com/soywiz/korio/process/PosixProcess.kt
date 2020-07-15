package com.soywiz.korio.process

import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import kotlinx.cinterop.*
import platform.posix.*
import kotlin.collections.*
import kotlin.text.*
import kotlin.*

actual suspend fun posixExec(
    path: String, cmdAndArgs: List<String>, env: Map<String, String>, handler: VfsProcessHandler
): Int = memScoped {
    val fd = alloc<fd_set>()
    val timeval = alloc<timeval>()
    timeval.tv_sec = 0
    timeval.tv_usec = 1
    val (f, pid) = sopen(*cmdAndArgs.toTypedArray(), cwd = path, envs = env)
    val fn = fileno(f)
    val bufSize = 10240
    ByteArray(bufSize).usePinned { tmp ->
        val bufArray = tmp.get()
        val buf = tmp.startAddressOf
        loop@while (true) {
            while (true) {
                posix_FD_SET(fn, fd.ptr)
                select(fileno(f) + 1, fd.ptr, null, null, timeval.ptr)
                if (posix_FD_ISSET(fn, fd.ptr) != 0) {
                    val res = read(fn, buf, bufSize.convert()).toInt()
                    if (res <= 0) break@loop
                    handler.onOut(bufArray.copyOf(res))
                } else {
                    break // No more data available
                }
            }
            delay(1.milliseconds)
        }
    }
    val status = alloc<IntVar>()
    waitpid(pid.convert(), status.ptr, 0.convert())
    status.value
}

fun sopen(vararg cmds: String, cwd: String, envs: Map<String, String> = mapOf()): Pair<CPointer<FILE>?, Long> = memScoped {
    val fds = allocArray<IntVar>(2)
    if (socketpair(AF_UNIX, SOCK_STREAM, 0, fds) < 0) {
        return null to 0L
    }
    val pid = fork()
    when (pid) {
        -1 -> {
            close(fds[0])
            close(fds[1])
            return null to 0L
        }
        0 -> { // child
            //printf("CHILD!\n");
            close(fds[0])
            dup2(fds[0], STDIN_FILENO)
            dup2(fds[1], STDOUT_FILENO)
            close(fds[1])
            chdir(cwd)
            for ((k ,v) in envs) putenv("$k=$v".cstr)
            execl("/bin/sh", "sh", "-c", cmds.joinToString(" ") { "'" + it.replace("'", "\\'") + "'" }, null)
            _exit(127);
        }
    }
    //printf("GO!\n");
    /* parent */
    close(fds[1]);
    return fdopen(fds[0], "r+") to pid.toLong()
}

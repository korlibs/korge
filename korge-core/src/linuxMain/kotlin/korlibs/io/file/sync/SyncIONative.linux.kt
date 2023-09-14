package korlibs.io.file.sync

import korlibs.io.stream.*
import kotlinx.cinterop.*
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
internal actual fun syncExecNative(
    commands: List<String>,
    envs: Map<String, String>,
    cwd: String
): SyncExecProcess {
    val mem = Arena()
    val pipeStdin = mem.allocArray<IntVar>(2)
    val pipeStdout = mem.allocArray<IntVar>(2)
    val pipeStderr = mem.allocArray<IntVar>(2)

    // Create pipes
    if (pipe(pipeStdin) == -1 || pipe(pipeStdout) == -1 || pipe(pipeStderr) == -1) {
        perror("pipe")
        exit(EXIT_FAILURE)
    }

    val childPid = fork()
    if (childPid == -1) {
        perror("fork")
        exit(EXIT_FAILURE)
    }

    if (childPid == 0) {  // Child process
        dup2(pipeStdin[0], STDIN_FILENO)
        dup2(pipeStdout[1], STDOUT_FILENO)
        dup2(pipeStderr[1], STDERR_FILENO)

        close(pipeStdin[1])
        close(pipeStdout[0])
        close(pipeStderr[0])

        memScoped {
            fun MemScope.allocArrayOfStrings(strs: List<String>): CValuesRef<CPointerVar<ByteVar>> {
                val out = allocArray<CPointerVar<ByteVar>>(strs.size + 1)
                for (n in strs.indices) out[n] = strs[n].cstr.ptr
                return out
            }
            execve(
                commands[0],
                allocArrayOfStrings(commands.drop(1)),
                allocArrayOfStrings(envs.map { "${it.key}=${it.value}" }),
            )
        }

        perror("execlp")
        exit(EXIT_FAILURE)
    }


    close(pipeStdin[0])
    close(pipeStdout[1])
    close(pipeStderr[1])

    fun toSyncInputStream(fd: Int): SyncInputStream {
        return object : SyncInputStream {
            override fun read(buffer: ByteArray, offset: Int, len: Int): Int {
                if (len == 0) return 0
                val read = buffer.usePinned {
                    read(fd, it.addressOf(offset), len.convert())
                }
                return read.toInt()
            }
        }
    }

    fun toSyncOutputStream(fd: Int): SyncOutputStream {
        return object : SyncOutputStream {
            override fun write(buffer: ByteArray, offset: Int, len: Int) {
                if (len == 0) return
                buffer.usePinned {
                    write(fd, it.addressOf(offset), len.convert())
                }
            }
        }
    }

    return object : SyncExecProcess(
        toSyncOutputStream(pipeStdin[1]),
        toSyncInputStream(pipeStdout[0]),
        toSyncInputStream(pipeStderr[0]),
    ) {
        override val exitCode: Int get() = memScoped {
            val status = alloc<IntVar>()
            waitpid(childPid, status.ptr, SIGTERM)
            return status.value
        }

        override fun destroy() {
            kill(childPid, SIGTERM)
        }

        private var closed = false
        override fun close() {
            if (closed) return
            closed = true
            close(pipeStdin[1])
            close(pipeStdout[0])
            close(pipeStderr[0])
            mem.clear()
        }
    }
}

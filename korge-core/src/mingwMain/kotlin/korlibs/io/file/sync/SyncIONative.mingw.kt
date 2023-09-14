package korlibs.io.file.sync

import korlibs.io.stream.*
import kotlinx.cinterop.*
import platform.windows.*

@OptIn(ExperimentalForeignApi::class)
internal actual fun syncExecNative(
    commands: List<String>,
    envs: Map<String, String>,
    cwd: String
): SyncExecProcess {
    val mem = Arena()
    val sa = mem.alloc<SECURITY_ATTRIBUTES>()
    sa.nLength = sizeOf<SECURITY_ATTRIBUTES>().convert()
    sa.lpSecurityDescriptor = null
    sa.bInheritHandle = 1

    val hInputRead = mem.alloc<HANDLEVar>()
    val hInputWrite = mem.alloc<HANDLEVar>()
    val hOutputRead = mem.alloc<HANDLEVar>()
    val hOutputWrite = mem.alloc<HANDLEVar>()
    val hErrorRead = mem.alloc<HANDLEVar>()
    val hErrorWrite = mem.alloc<HANDLEVar>()

    CreatePipe(hOutputRead.ptr, hOutputWrite.ptr, sa.ptr, 0u)
    CreatePipe(hErrorRead.ptr, hErrorWrite.ptr, sa.ptr, 0u)
    CreatePipe(hInputRead.ptr, hInputWrite.ptr, sa.ptr, 0u)

    val pi = mem.alloc<PROCESS_INFORMATION>()
    val si = mem.alloc<STARTUPINFOW>()
    si.cb = sizeOf<STARTUPINFOW>().convert()
    si.dwFlags = STARTF_USESTDHANDLES.convert()
    si.hStdOutput = hOutputWrite.value
    si.hStdInput = hInputRead.value
    si.hStdError = hErrorWrite.value

    memScoped {
        if (CreateProcess!!(null, "cmd.exe".wcstr.ptr, null, null, TRUE, 0.convert(), null, null, si.ptr, pi.ptr) == 0) {
            error("CreateProcess failed (${GetLastError()})")
        }
    }

    //CloseHandle(hOutputWrite)
    //CloseHandle(hInputRead)
    //CloseHandle(hErrorWrite)

    fun HANDLE?.toSyncInputStream(): SyncInputStream {
        val handle = this
        return object : SyncInputStream {
            override fun read(buffer: ByteArray, offset: Int, len: Int): Int = memScoped {
                if (buffer.isEmpty()) return@memScoped 0
                val readCount = alloc<DWORDVar>()
                readCount.value = 0u
                buffer.usePinned {
                    ReadFile(handle, it.addressOf(offset), len.convert(), readCount.ptr, null)
                }
                return readCount.value.convert()
            }
        }
    }

    fun HANDLE?.toSyncOutputStream(): SyncOutputStream {
        val handle = this
        return object : SyncOutputStream {
            override fun write(buffer: ByteArray, offset: Int, len: Int): Unit = memScoped {
                if (buffer.isEmpty()) return@memScoped
                val writeCount = alloc<DWORDVar>()
                writeCount.value = 0u
                buffer.usePinned {
                    WriteFile(handle, it.addressOf(offset), len.convert(), writeCount.ptr, null)
                }
                //return writeCount.value.convert()
            }
        }
    }

    return object : SyncExecProcess(
        hInputWrite.value.toSyncOutputStream(),
        hOutputRead.value.toSyncInputStream(),
        hErrorRead.value.toSyncInputStream(),
    ) {
        override val exitCode: Int get() {
            WaitForSingleObject(pi.hProcess, INFINITE)
            return memScoped {
                val exitCode = alloc<DWORDVar>()
                GetExitCodeProcess(pi.hProcess, exitCode.ptr)
                exitCode.value.convert()
            }
        }

        override fun destroy() {
            TerminateProcess(pi.hProcess, (-1).toUInt())
            close()
        }

        private var closed = false
        override fun close() {
            if (closed) return
            closed = true
            CloseHandle(hOutputWrite.value)
            CloseHandle(hInputRead.value)
            CloseHandle(hErrorWrite.value)
            CloseHandle(hInputWrite.value)
            CloseHandle(hOutputRead.value)
            CloseHandle(hErrorRead.value)
            CloseHandle(pi.hProcess)
            CloseHandle(pi.hThread)
            mem.clear()
        }
    }
}

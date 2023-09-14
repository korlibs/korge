package korlibs.io.file.sync

import korlibs.io.stream.*
import kotlinx.cinterop.*
import platform.Foundation.*
import platform.IOBluetooth.*
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
internal actual fun syncExecNative(
    commands: List<String>,
    envs: Map<String, String>,
    cwd: String
): SyncExecProcess {
    val task = NSTask()
    task.arguments = commands
    task.environment = envs.toMap()
    task.currentDirectoryPath = cwd
    task.launch()

    return object : SyncExecProcess(
        task.standardInput.toSyncOutputStream(),
        task.standardOutput.toSyncInputStream(),
        task.standardError.toSyncInputStream(),
    ) {
        override val exitCode: Int get() = task.waitUntilExit().let { task.terminationStatus }

        override fun destroy() {
            task.interrupt()
        }
    }
}

private fun Any?.toFileHandleFor(read: Boolean): NSFileHandle = when (this) {
    is NSFileHandle -> this
    is NSPipe -> if (read) this.fileHandleForReading else this.fileHandleForWriting
    else -> TODO("$this")
}

@ExperimentalForeignApi
private fun Any?.toSyncInputStream(): SyncInputStream {
    val handle = this.toFileHandleFor(read = true)
    return object : SyncInputStream {
        override fun read(buffer: ByteArray, offset: Int, len: Int): Int {
            val data = handle.readDataUpToLength(len.convert(), null) ?: return -1
            buffer.usePinned {
                memcpy(it.addressOf(offset), data.bytes, data.length.convert())
            }
            return data.length.convert()
        }
    }
}

@ExperimentalForeignApi
private fun Any?.toSyncOutputStream(): SyncOutputStream {
    val handle = this.toFileHandleFor(read = false)
    return object : SyncOutputStream {
        override fun write(buffer: ByteArray, offset: Int, len: Int) {
            if (len >= UShort.SIZE_BYTES) {
                var pos = offset
                var len = len
                while (len > 0) {
                    val clen = minOf(len, UShort.SIZE_BYTES)
                    write(buffer, pos, clen)
                    pos += clen
                    len -= clen
                }
            } else {
                buffer.usePinned {
                    handle.write(it.addressOf(offset), len.convert())
                }
            }
        }
    }
}

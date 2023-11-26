package korlibs.io.file.sync

import korlibs.io.posix.*
import korlibs.memory.*
import kotlinx.cinterop.*
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
actual fun platformSyncIO(caseSensitive: Boolean): SyncIO = object : SyncIO {
    override fun realpath(path: String): String = posixRealpath(path)
    override fun readlink(path: String): String? = posixReadlink(path)

    override fun open(path: String, mode: String): SyncIOFD {
        val fd = posixFopen(path, mode)
        posixFseek(fd, 0L, SEEK_END)
        var fdlength = posixFtell(fd).toLong()
        posixFseek(fd, 0L, SEEK_SET)
        return object : SyncIOFD {
            override var length: Long
                get() = fdlength
                set(value) {
                    fdlength = value
                    posixTruncate(path, fdlength)
                }
            override var position: Long
                get() = posixFtell(fd).toLong()
                set(value) { posixFseek(fd, value, SEEK_SET) }

            override fun write(data: ByteArray, offset: Int, size: Int): Int {
                data.usePinned {
                    posixFwrite(it.startAddressOf + offset, 1, size.convert(), fd)
                }
                return size
            }
            override fun read(data: ByteArray, offset: Int, size: Int): Int {
                return data.usePinned {
                    posixFread(it.startAddressOf + offset, 1, size.convert(), fd).toInt()
                }
            }
            override fun close() {
                posixFclose(fd)
            }
        }
    }

    override fun stat(path: String): SyncIOStat? = posixStat(path)?.let { SyncIOStat(path, it.isDirectory, it.size) }
    override fun mkdir(path: String): Boolean = posixMkdir(path, "777".toInt(8)) != 0
    override fun rmdir(path: String): Boolean = platform.posix.rmdir(path) != 0
    override fun delete(path: String): Boolean = platform.posix.unlink(path) != 0
    override fun list(path: String): List<String> {
        val dir = opendir(path)
        val out = arrayListOf<String>()
        try {
            if (dir != null) {
                while (true) {
                    val res = readdir(dir) ?: break
                    val name = res.pointed.d_name.toKString()
                    out += name
                }
            }
        } finally {
            if (dir != null) closedir(dir)
        }
        return out
    }

    override fun exec(commands: List<String>, envs: Map<String, String>, cwd: String): SyncExecProcess {
        return syncExecNative(commands, envs, cwd)
    }
}

internal expect fun syncExecNative(commands: List<String>, envs: Map<String, String>, cwd: String): SyncExecProcess

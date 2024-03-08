package korlibs.io.file.std

import korlibs.datastructure.closeable.Closeable
import korlibs.io.async.*
import korlibs.io.file.*
import korlibs.io.stream.*
import korlibs.io.util.*
import korlibs.time.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.*
import java.nio.file.*
import java.nio.file.Path
import java.nio.file.attribute.*
import kotlin.collections.*
import kotlin.io.NoSuchFileException

internal open class BaseLocalVfsJvm : LocalVfs() {
    val that = this
    override val absolutePath: String = ""

    override suspend fun chmod(path: String, mode: UnixPermissions): Unit = executeIo {
        val file = resolveFileCaseSensitive(path)
        Files.setPosixFilePermissions(file.toPath(), mode.toSet())
    }

    fun resolve(path: String): String = path
    fun resolvePath(path: String): Path = resolveFile(path).toPath()
    fun resolveFile(path: String): File = File(resolve(path))
    fun resolveFileCaseSensitive(path: String): File = resolveFile(path).caseSensitiveOrThrow()

    override suspend fun exec(
        path: String,
        cmdAndArgs: List<String>,
        env: Map<String, String>,
        handler: VfsProcessHandler
    ): Int = executeIo {
        checkExecFolder(path, cmdAndArgs)
        val actualCmd = ShellArgs.buildShellExecCommandLineArrayForProcessBuilder(cmdAndArgs)
        val pb = ProcessBuilder(actualCmd)
        pb.environment().putAll(LinkedHashMap())
        pb.directory(resolveFile(path))

        val p = pb.start()
        var closing = false
        while (true) {
            val o = p.inputStream.readAvailableChunk(readRest = closing)
            val e = p.errorStream.readAvailableChunk(readRest = closing)
            if (o.isNotEmpty()) handler.onOut(o)
            if (e.isNotEmpty()) handler.onErr(e)
            if (closing) break
            if (o.isEmpty() && e.isEmpty() && !p.isAliveJre7) {
                closing = true
                continue
            }
            delay(1L)
        }
        p.waitFor()
        //handler.onCompleted(p.exitValue())
        p.exitValue()
    }

    private fun InputStream.readAvailableChunk(readRest: Boolean): ByteArray {
        val out = ByteArrayOutputStream()
        while (if (readRest) true else available() > 0) {
            val c = this.read()
            if (c < 0) break
            out.write(c)
        }
        return out.toByteArray()
    }

    private fun InputStreamReader.readAvailableChunk(i: InputStream, readRest: Boolean): String {
        val out = java.lang.StringBuilder()
        while (if (readRest) true else i.available() > 0) {
            val c = this.read()
            if (c < 0) break
            out.append(c.toChar())
        }
        return out.toString()
    }

    //override suspend fun readRange(path: String, range: LongRange): ByteArray = executeIo {
    //    RandomAccessFile(resolveFile(path), "r").use { raf ->
    //        val fileLength = raf.length()
    //        val start = min(range.start, fileLength)
    //        val end = min(range.endInclusive, fileLength - 1) + 1
    //        val totalRead = (end - start).toInt()
    //        val out = ByteArray(totalRead)
    //        raf.seek(start)
    //        val read = raf.read(out)
    //        if (read != totalRead) out.copyOf(read) else out
    //    }
    //}

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun open(path: String, mode: VfsOpenMode): AsyncStream =
        open(this, resolveFile(path), mode, path)

    override suspend fun setSize(path: String, size: Long): Unit = executeIo {
        val file = resolveFile(path)
        FileOutputStream(file, true).channel.use { outChan ->
            outChan.truncate(size)
        }
        Unit
    }

    override suspend fun stat(path: String): VfsStat = executeIo {
        val file = resolveFile(path)
        stat(this, file, "$path/${file.name}")
    }


    override suspend fun listFlow(path: String): kotlinx.coroutines.flow.Flow<VfsFile> = flow {
        val file = resolveFile(path).caseSensitiveOrThrow()
        if (!file.isDirectory) throw NoSuchFileException(file)
        for (it in (file.listFiles() ?: emptyArray<File>())) {
            emit(that.file("$path/${it.name}"))
        }
    }.flowOn(Dispatchers.CIO)

    override suspend fun mkdir(path: String, attributes: List<Attribute>): Boolean =
        executeIo { resolveFile(path).mkdir() }

    override suspend fun mkdirs(path: String, attributes: List<Attribute>): Boolean =
        executeIo { resolveFile(path).mkdirs() }

    override suspend fun touch(path: String, time: DateTime, atime: DateTime): Unit =
        executeIo { resolveFile(path).setLastModified(time.unixMillisLong); Unit }

    override suspend fun delete(path: String): Boolean = executeIo { resolveFileCaseSensitive(path).delete() }
    override suspend fun rmdir(path: String): Boolean = executeIo { resolveFileCaseSensitive(path).delete() }
    override suspend fun rename(src: String, dst: String): Boolean =
        executeIo { resolveFileCaseSensitive(src).renameTo(resolveFile(dst)) }

    protected open fun watchModifiers(path: String): Array<WatchEvent.Modifier> = emptyArray()

    override suspend fun watch(path: String, handler: (FileEvent) -> Unit): Closeable {
        var running = true
        val fs = FileSystems.getDefault()
        val watcher = fs.newWatchService()

        val registeredKey = fs.getPath(path).register(
            watcher,
            arrayOf(
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY,
            ),
            *watchModifiers(path)
        )

        GlobalScope.launch(Dispatchers.CIO) {
            while (running) {
                val key = watcher.take()

                for (e in key.pollEvents()) {
                    val kind = e.kind()
                    val filepath = e.context() as Path
                    val rfilepath = fs.getPath(path, filepath.toString())
                    val file = rfilepath.toFile()
                    val absolutePath = file.absolutePath
                    val vfsFile = file(absolutePath)
                    withContext(coroutineContext) {
                        when (kind) {
                            StandardWatchEventKinds.OVERFLOW -> {
                                println("Overflow WatchService")
                            }

                            StandardWatchEventKinds.ENTRY_CREATE -> {
                                handler(
                                    FileEvent(
                                        FileEvent.Kind.CREATED,
                                        vfsFile
                                    )
                                )
                            }

                            StandardWatchEventKinds.ENTRY_MODIFY -> {
                                handler(
                                    FileEvent(
                                        FileEvent.Kind.MODIFIED,
                                        vfsFile
                                    )
                                )
                            }

                            StandardWatchEventKinds.ENTRY_DELETE -> {
                                handler(
                                    FileEvent(
                                        FileEvent.Kind.DELETED,
                                        vfsFile
                                    )
                                )
                            }
                        }
                    }
                }

                if (!key.reset()) {
                    key.cancel()
                    registeredKey.cancel()
                    break
                }
            }
        }

        return Closeable {
            running = false
            registeredKey.cancel()
        }
    }

    protected suspend fun <T> executeIo(callback: suspend () -> T): T = jvmExecuteIo(callback)

    companion object {
        suspend fun open(vfs: Vfs, file: File, mode: VfsOpenMode, path: String): AsyncStream {
            try {
                val raf = jvmExecuteIo {
                    val exists = file.existsCaseSensitive()
                    if (exists && (mode == VfsOpenMode.CREATE_NEW)) {
                        throw IOException("File $file already exists")
                    }
                    if (!exists && !mode.createIfNotExists) {
                        throw IOException("File $file doesn't exist")
                    }
                    RandomAccessFile(
                        file, when (mode) {
                            VfsOpenMode.READ -> "r"
                            VfsOpenMode.WRITE -> "rw"
                            VfsOpenMode.APPEND -> "rw"
                            VfsOpenMode.CREATE -> "rw"
                            VfsOpenMode.CREATE_NEW -> "rw"
                            VfsOpenMode.CREATE_OR_TRUNCATE -> "rw"
                        }
                    ).apply {
                        if (mode.truncate) {
                            setLength(0L)
                        }
                        if (mode == VfsOpenMode.APPEND) {
                            seek(length())
                        }
                    }
                }

                return object : AsyncStreamBase() {
                    override suspend fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int = jvmExecuteIo {
                        raf.seek(position)
                        raf.read(buffer, offset, len)
                    }

                    override suspend fun write(position: Long, buffer: ByteArray, offset: Int, len: Int) = jvmExecuteIo {
                        if (!mode.append) raf.seek(position)
                        raf.write(buffer, offset, len)
                    }

                    override suspend fun setLength(value: Long): Unit = jvmExecuteIo {
                        raf.setLength(value)
                    }

                    override suspend fun getLength(): Long = jvmExecuteIo { raf.length() }
                    override suspend fun close() = raf.close()

                    override fun toString(): String = "$vfs($path)"
                }.toAsyncStream(raf.filePointer)
            } catch (e: java.nio.file.NoSuchFileException) {
                throw FileNotFoundException(e.message)
            }
        }

        suspend fun stat(root: Vfs, file: File, fullpath: String = file.absolutePath): VfsStat = jvmExecuteIo {
            if (file.existsCaseSensitive()) {
                val lastModified = DateTime.fromUnixMillis(file.lastModified())
                root.createExistsStat(
                    fullpath,
                    isDirectory = file.isDirectory,
                    size = file.length(),
                    createTime = lastModified,
                    modifiedTime = lastModified,
                    lastAccessTime = lastModified,
                    mode = kotlin.runCatching {
                        Files.getPosixFilePermissions(file.toPath()).toUnixPermissionsAttribute().bits
                    }.getOrNull() ?: 511
                )
            } else {
                root.createNonExistsStat(fullpath)
            }
        }

        fun UnixPermissions.toSet(): Set<PosixFilePermission> = buildList<PosixFilePermission> {
            val it = this@toSet
            if (it.owner.writable) add(PosixFilePermission.OWNER_WRITE)
            if (it.owner.readable) add(PosixFilePermission.OWNER_READ)
            if (it.owner.executable) add(PosixFilePermission.OWNER_EXECUTE)
            if (it.other.writable) add(PosixFilePermission.OTHERS_WRITE)
            if (it.other.readable) add(PosixFilePermission.OTHERS_READ)
            if (it.other.executable) add(PosixFilePermission.OTHERS_EXECUTE)
            if (it.group.writable) add(PosixFilePermission.GROUP_WRITE)
            if (it.group.readable) add(PosixFilePermission.GROUP_READ)
            if (it.group.executable) add(PosixFilePermission.GROUP_EXECUTE)
        }.toSet()

        fun Set<PosixFilePermission>.toUnixPermissionsAttribute(): UnixPermissions {
            return UnixPermissions(
                owner = UnixPermission(
                    contains(PosixFilePermission.OWNER_READ),
                    contains(PosixFilePermission.OWNER_WRITE),
                    contains(PosixFilePermission.OWNER_EXECUTE)
                ),
                group = UnixPermission(
                    contains(PosixFilePermission.GROUP_READ),
                    contains(PosixFilePermission.GROUP_WRITE),
                    contains(PosixFilePermission.GROUP_EXECUTE)
                ),
                other = UnixPermission(
                    contains(PosixFilePermission.OTHERS_READ),
                    contains(PosixFilePermission.OTHERS_WRITE),
                    contains(PosixFilePermission.OTHERS_EXECUTE)
                ),
            )
        }

    }

    override fun toString(): String = "LocalVfs"
}

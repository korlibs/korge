package com.soywiz.korio.file.std

import com.soywiz.klock.DateTime
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.file.VfsOpenMode
import com.soywiz.korio.file.VfsProcessHandler
import com.soywiz.korio.file.VfsStat
import com.soywiz.korio.lang.Closeable
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.AsyncStreamBase
import com.soywiz.korio.stream.toAsyncStream
import com.soywiz.korio.util.isAliveJre7
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.RandomAccessFile
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent

internal open class BaseLocalVfsJvm : LocalVfs() {
    val that = this
    override val absolutePath: String = ""

    protected suspend fun <T> executeIo(callback: suspend CoroutineScope.() -> T): T = withContext(Dispatchers.IO, callback)
    protected suspend fun <T> doIo(callback: suspend () -> T): T = withContext(Dispatchers.IO) { callback() }
    //private suspend inline fun <T> executeIo(callback: suspend () -> T): T = callback()

    fun resolve(path: String) = path
    fun resolvePath(path: String) = Paths.get(resolve(path))
    fun resolveFile(path: String) = File(resolve(path))

    override suspend fun exec(
        path: String,
        cmdAndArgs: List<String>,
        env: Map<String, String>,
        handler: VfsProcessHandler
    ): Int = executeIo {
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
            Thread.sleep(1L)
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
    override suspend fun open(path: String, mode: VfsOpenMode): AsyncStream {
        try {
            val raf = executeIo {
                val file = resolveFile(path)
                if (file.exists() && (mode == VfsOpenMode.CREATE_NEW)) {
                    throw IOException("File $file already exists")
                }
                RandomAccessFile(file, when (mode) {
                    VfsOpenMode.READ -> "r"
                    VfsOpenMode.WRITE -> "rw"
                    VfsOpenMode.APPEND -> "rw"
                    VfsOpenMode.CREATE -> "rw"
                    VfsOpenMode.CREATE_NEW -> "rw"
                    VfsOpenMode.CREATE_OR_TRUNCATE -> "rw"
                }).apply {
                    if (mode.truncate) {
                        setLength(0L)
                    }
                    if (mode == VfsOpenMode.APPEND) {
                        seek(length())
                    }
                }
            }

            return object : AsyncStreamBase() {
                override suspend fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int = executeIo {
                    raf.seek(position)
                    raf.read(buffer, offset, len)
                }

                override suspend fun write(position: Long, buffer: ByteArray, offset: Int, len: Int) = executeIo {
                    raf.seek(position)
                    raf.write(buffer, offset, len)
                }

                override suspend fun setLength(value: Long): Unit = executeIo {
                    raf.setLength(value)
                }

                override suspend fun getLength(): Long = executeIo { raf.length() }
                override suspend fun close() = raf.close()

                override fun toString(): String = "$that($path)"
            }.toAsyncStream()
        } catch (e: java.nio.file.NoSuchFileException) {
            throw FileNotFoundException(e.message)
        }
    }

    override suspend fun setSize(path: String, size: Long): Unit = executeIo {
        val file = resolveFile(path)
        FileOutputStream(file, true).channel.use { outChan ->
            outChan.truncate(size)
        }
        Unit
    }

    override suspend fun stat(path: String): VfsStat = executeIo {
        val file = resolveFile(path)
        val fullpath = "$path/${file.name}"
        if (file.exists()) {
            val lastModified = DateTime.fromUnix(file.lastModified())
            createExistsStat(
                fullpath,
                isDirectory = file.isDirectory,
                size = file.length(),
                createTime = lastModified,
                modifiedTime = lastModified,
                lastAccessTime = lastModified
            )
        } else {
            createNonExistsStat(fullpath)
        }
    }

    /*
    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun listFlow(path: String): kotlinx.coroutines.flow.Flow<VfsFile> = flow {
        for (it in (File(path).listFiles() ?: emptyArray<File>())) {
            emit(that.file("$path/${it.name}"))
        }
    }.flowOn(IOContext)
    */

    override suspend fun listFlow(path: String): kotlinx.coroutines.flow.Flow<VfsFile> = flow {
        for (it in (File(path).listFiles() ?: emptyArray<File>())) {
            emit(that.file("$path/${it.name}"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun mkdir(path: String, attributes: List<Attribute>): Boolean =
        executeIo { resolveFile(path).mkdir() }
    override suspend fun mkdirs(path: String, attributes: List<Attribute>): Boolean =
        executeIo { resolveFile(path).mkdirs() }

    override suspend fun touch(path: String, time: DateTime, atime: DateTime): Unit =
        executeIo { resolveFile(path).setLastModified(time.unixMillisLong); Unit }

    override suspend fun delete(path: String): Boolean = executeIo { resolveFile(path).delete() }
    override suspend fun rmdir(path: String): Boolean = executeIo { resolveFile(path).delete() }
    override suspend fun rename(src: String, dst: String): Boolean =
        executeIo { resolveFile(src).renameTo(resolveFile(dst)) }

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

        GlobalScope.launch(Dispatchers.IO) {
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

    override fun toString(): String = "LocalVfs"
}

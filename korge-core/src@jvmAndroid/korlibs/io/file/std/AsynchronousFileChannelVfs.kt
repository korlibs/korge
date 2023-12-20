package korlibs.io.file.std

import korlibs.io.async.*
import korlibs.io.file.*
import korlibs.io.stream.*
import korlibs.io.util.*
import java.io.*
import java.nio.*
import java.nio.channels.*
import java.nio.file.*
import kotlin.collections.buildList
import kotlin.coroutines.*
import kotlin.io.NoSuchFileException
import kotlin.io.path.*

// Requires JVM 7, and Android API Level 26 (Android Oreo 8.0)
internal open class AsynchronousFileChannelVfs : BaseLocalVfsJvm() {
    //override suspend fun readRange(path: String, range: LongRange): ByteArray {
    //    return openAsynchronousFileChannel(path, VfsOpenMode.READ).use { afc ->
    //        val size = executeIo { afc.size() }
    //        val requested = range.length
    //        val available = size - range.first
    //        val toRead = min(requested, available).toInt()
    //        val buffer = ByteBuffer.allocate(toRead)
    //        val read = nioSuspendCompletion<Int> { afc.read(buffer, range.first, Unit, it) }
    //        ByteArray(read).also { buffer.reset().get(it) }
    //    }
    //}

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun open(path: String, mode: VfsOpenMode): AsyncStream {
        if (coroutineContext.preferSyncIo) return super.open(path, mode)

        val raf = openAsynchronousFileChannel(path, mode)
        return object : AsyncStreamBase() {
            override suspend fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int =
                nioSuspendCompletion { raf.read(ByteBuffer.wrap(buffer, offset, len), position, Unit, it) }

            override suspend fun write(position: Long, buffer: ByteArray, offset: Int, len: Int) {
                nioSuspendCompletion { raf.write(ByteBuffer.wrap(buffer, offset, len), position, Unit, it) }
            }

            override suspend fun hasLength(): Boolean = getLength() >= 0L
            override suspend fun setLength(value: Long): Unit = raf.truncateSuspend(value)
            override suspend fun getLength(): Long = raf.sizeSuspend()
            override suspend fun close() = raf.close()

            override fun toString(): String = "$that($path)"
        }.toAsyncStream(if (mode.append) raf.sizeSuspend() else 0L)
    }

    suspend fun openAsynchronousFileChannel(path: String, mode: VfsOpenMode): AsynchronousFileChannel {
        return try {
            val options: List<OpenOption> = buildList {
                add(StandardOpenOption.READ)
                if (mode.write) add(StandardOpenOption.WRITE)
                //if (mode == VfsOpenMode.APPEND) add(StandardOpenOption.APPEND) // Unsupported
                if (mode.createIfNotExists) add(StandardOpenOption.CREATE)
                if (mode == VfsOpenMode.CREATE_NEW) add(StandardOpenOption.CREATE_NEW)
                if (mode.truncate) add(StandardOpenOption.TRUNCATE_EXISTING)
            }
            //println("path=$path, mode=$mode, options=$options")
            executeIo { AsynchronousFileChannel.open(Path(path).caseSensitiveOrThrow(), *options.toTypedArray()) }
        } catch (e: java.nio.file.NoSuchFileException) {
            throw FileNotFoundException(e.message)
        }
    }

    suspend fun AsynchronousFileChannel.sizeSuspend(): Long = executeIo { size() }
    suspend fun AsynchronousFileChannel.truncateSuspend(size: Long): Unit = executeIo { truncate(size) }
}

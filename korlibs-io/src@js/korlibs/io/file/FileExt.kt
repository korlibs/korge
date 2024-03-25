package korlibs.io.file

import korlibs.memory.*
import korlibs.io.stream.*
import korlibs.io.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import org.khronos.webgl.*
import org.w3c.files.*
import kotlin.math.*

fun Blob.openAsync(): AsyncStream = BlobAsyncBaseStream(this).toAsyncStream().buffered(0x10_000, 0x10)

external class BlobExt {
    fun slice(start: Number = definedExternally, end: Number = definedExternally, contentType: String = definedExternally): Blob
}

class BlobAsyncBaseStream(val blob: Blob) : AsyncStreamBase() {
    override suspend fun close() {
    }

    override suspend fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
        val deferred = CompletableDeferred<Int>()
        val reader = FileReader()
        reader.onload = {
            val ab = reader.result.unsafeCast<ArrayBuffer>()
            val minLen = min(ab.byteLength, len)
            arraycopy(Int8Array(ab).toByteArray(), 0, buffer, offset, minLen)
            deferred.complete(minLen)
        }
        reader.onerror = {
            deferred.completeExceptionally(Throwable("${reader.error}"))
        }
        reader.readAsArrayBuffer(blob.unsafeCast<BlobExt>().slice(position.toDouble(), position.toDouble() + len.toDouble()))
        return deferred.await()
    }

    override suspend fun getLength(): Long {
        return blob.size.toLong()
    }
}

fun File.toVfs(): VfsFile {
    val file = this
    return object : Vfs() {
        override val absolutePath: String = file.name
        // @TODO: Check path
        override suspend fun open(path: String, mode: VfsOpenMode): AsyncStream = file.openAsync()
        override suspend fun listFlow(path: String): Flow<VfsFile> {
            return if (path == "/" || path == "") {
                listOf(this[file.name]).asFlow()
            } else {
                listOf<VfsFile>().asFlow()
            }
        }
    }[file.name]
}

package com.soywiz.korio.file

import com.soywiz.kmem.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.internal.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.*
import kotlinx.coroutines.*
import org.khronos.webgl.*
import org.w3c.files.*

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
            val minLen = min2(ab.size, len)
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
        override suspend fun listSimple(path: String): List<VfsFile> {
            return if (path == "/" || path == "") {
                listOf(this[file.name])
            } else {
                listOf()
            }
        }
    }[file.name]
}

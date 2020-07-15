package com.soywiz.korio.stream

import com.soywiz.korio.lang.*
import java.io.*

fun InputStream.toAsync(length: Long? = null): AsyncInputStream {
	val syncIS = this
	if (length != null) {
		return object : AsyncInputStream, AsyncLengthStream {
			override suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int = syncIS.read(buffer, offset, len)
			override suspend fun read(): Int = syncIS.read()
			override suspend fun close() = syncIS.close()
			override suspend fun setLength(value: Long) {
				unsupported("Can't set length")
			}

			override suspend fun getLength(): Long = length
		}
	} else {
		return object : AsyncInputStream {
			override suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int = syncIS.read(buffer, offset, len)
			override suspend fun read(): Int = syncIS.read()
			override suspend fun close() = run { syncIS.close() }
		}
	}
}

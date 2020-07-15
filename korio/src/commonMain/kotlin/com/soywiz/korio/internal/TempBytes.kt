package com.soywiz.korio.internal

import com.soywiz.kds.*
import com.soywiz.korio.lang.*

@PublishedApi
internal const val BYTES_TEMP_SIZE = 0x10000

@PublishedApi
internal val bytesTempPool by threadLocal { Pool(preallocate = 1) { ByteArray(BYTES_TEMP_SIZE) } }

@PublishedApi
internal val smallBytesPool by threadLocal { Pool(preallocate = 16) { ByteArray(16) } }

@PublishedApi
internal inline fun <T, R> Pool<T>.alloc2(callback: (T) -> R): R {
	val temp = alloc()
	try {
		return callback(temp)
	} finally {
		free(temp)
	}
}

@PublishedApi
internal inline fun <T, R> Pool<T>.allocThis(callback: T.() -> R): R {
	val temp = alloc()
	try {
		return callback(temp)
	} finally {
		free(temp)
	}
}

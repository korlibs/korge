package com.soywiz.korio.internal

import com.soywiz.kds.*
import com.soywiz.korio.lang.*

@PublishedApi
internal const val BYTES_TEMP_SIZE = 0x10000

@PublishedApi
internal val bytesTempPool by threadLocal { Pool(preallocate = 1) { ByteArray(BYTES_TEMP_SIZE) } }

@PublishedApi
internal val smallBytesPool by threadLocal { Pool(preallocate = 16) { ByteArray(16) } }

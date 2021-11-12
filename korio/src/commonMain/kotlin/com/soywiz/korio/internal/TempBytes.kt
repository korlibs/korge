package com.soywiz.korio.internal

import com.soywiz.kds.*
import com.soywiz.korio.lang.*
import kotlin.native.concurrent.*

@PublishedApi
internal const val BYTES_TEMP_SIZE = 0x10000

@PublishedApi
@ThreadLocal
internal val bytesTempPool by threadLocal { Pool(preallocate = 1) { ByteArray(BYTES_TEMP_SIZE) } }

@PublishedApi
@ThreadLocal
internal val smallBytesPool by threadLocal { Pool(preallocate = 16) { ByteArray(16) } }

package com.soywiz.korio.posix

import kotlinx.cinterop.*
import platform.posix.*

internal expect fun posixFread(__ptr: CValuesRef<*>?, __size: Long, __nitems: Long, __stream: CValuesRef<FILE>?): ULong
internal expect fun posixFwrite(__ptr: CValuesRef<*>?, __size: Long, __nitems: Long, __stream: CValuesRef<FILE>?): ULong
internal expect fun posixFseek(file: CValuesRef<FILE>?, offset: Long, whence: Int): Int
internal expect fun posixFtell(file: CValuesRef<FILE>?): ULong
internal expect fun ioctlFionRead(sockfd: Int, ptr: CValuesRef<IntVar>)

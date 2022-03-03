package com.soywiz.korio.posix

import kotlinx.cinterop.*
import platform.posix.*

data class PosixStatInfo(val size: Long, val isDirectory: Boolean)

expect fun posixFread(__ptr: CValuesRef<*>?, __size: Long, __nitems: Long, __stream: CValuesRef<FILE>?): ULong
expect fun posixFwrite(__ptr: CValuesRef<*>?, __size: Long, __nitems: Long, __stream: CValuesRef<FILE>?): ULong
expect fun posixFseek(file: CValuesRef<FILE>?, offset: Long, whence: Int): Int
expect fun posixFtell(file: CValuesRef<FILE>?): ULong
expect fun posixFopen(filename: String, mode: String): CPointer<FILE>?
expect fun posixFclose(file: CPointer<FILE>?): Int
expect fun posixTruncate(file: String, size: Long): Int
expect fun posixStat(rpath: String): PosixStatInfo?
expect fun posixReadlink(path: String): String?
expect fun posixRealpath(path: String): String
expect fun posixMkdir(path: String, attr: Int): Int
expect fun ioctlSocketFionRead(sockfd: Int): Int

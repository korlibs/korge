package com.soywiz.korio.posix

import platform.posix.*
import kotlinx.cinterop.*

internal actual fun posixFread(__ptr: CValuesRef<*>?, __size: Long, __nitems: Long, __stream: CValuesRef<FILE>?): ULong {
    return fread(__ptr, __size.convert(), __nitems.convert(), __stream).convert()
}

internal actual fun posixFwrite(__ptr: CValuesRef<*>?, __size: Long, __nitems: Long, __stream: CValuesRef<FILE>?): ULong {
    return fwrite(__ptr, __size.convert(), __nitems.convert(), __stream).convert()
}

internal actual fun posixFseek(file: CValuesRef<FILE>?, offset: Long, whence: Int): Int {
    return fseek(file, offset.convert(), whence.convert())
}

internal actual fun posixFtell(file: CValuesRef<FILE>?): ULong {
    return ftell(file).convert()
}

internal actual fun ioctlFionRead(sockfd: Int, ptr: CValuesRef<IntVar>) {
    ioctl(sockfd, FIONREAD, ptr)
}

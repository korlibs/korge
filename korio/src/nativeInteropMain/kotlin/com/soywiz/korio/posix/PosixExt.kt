package com.soywiz.korio.posix

import platform.posix.*
import kotlinx.cinterop.*

actual fun posixFread(__ptr: CValuesRef<*>?, __size: Long, __nitems: Long, __stream: CValuesRef<FILE>?): ULong {
    return fread(__ptr, __size.convert(), __nitems.convert(), __stream).convert()
}

actual fun posixFwrite(__ptr: CValuesRef<*>?, __size: Long, __nitems: Long, __stream: CValuesRef<FILE>?): ULong {
    return fwrite(__ptr, __size.convert(), __nitems.convert(), __stream).convert()
}

actual fun posixFseek(file: CValuesRef<FILE>?, offset: Long, whence: Int): Int {
    return fseek(file, offset.convert(), whence.convert())
}

actual fun posixFtell(file: CValuesRef<FILE>?): ULong {
    return ftell(file).convert()
}

actual fun ioctlFionRead(sockfd: Int, ptr: CValuesRef<IntVar>) {
    ioctl(sockfd, FIONREAD, ptr)
}

actual fun posixFclose(file: CPointer<FILE>?): Int {
    return fclose(file)
}

actual fun posixTruncate(file: String, size: Long): Int {
    return truncate(file, size.convert())
}

actual fun posixStat(rpath: String): PosixStatInfo? {
    memScoped {
        val s = alloc<stat>()
        if (platform.posix.stat(rpath, s.ptr) == 0) {
            val size: Long = s.st_size.toLong()
            val isDirectory = (s.st_mode.toInt() and S_IFDIR) != 0
            return PosixStatInfo(size = size, isDirectory = isDirectory)
        }
    }
    return null
}

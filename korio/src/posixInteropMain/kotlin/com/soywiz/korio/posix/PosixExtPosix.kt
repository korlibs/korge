package com.soywiz.korio.posix

import kotlinx.cinterop.*
import platform.posix.*

actual fun posixFopen(filename: String, mode: String): CPointer<FILE>? {
    return fopen(filename, mode)
}

actual fun posixReadlink(path: String): String? = memScoped {
    val addr = allocArray<ByteVar>(PATH_MAX)
    val finalSize = readlink(path, addr, PATH_MAX.convert()).toInt()
    if (finalSize < 0) null else addr.toKString()
}

actual fun posixRealpath(path: String): String = memScoped {
    val temp = allocArray<ByteVar>(PATH_MAX)
    realpath(path, temp)
    temp.toKString()
}

actual fun posixGetcwd(): String = memScoped {
    val temp = allocArray<ByteVar>(PATH_MAX + 1)
    getcwd(temp, PATH_MAX)
    temp.toKString()
}

actual fun posixMkdir(path: String, attr: Int): Int {
    return platform.posix.mkdir(path, attr.convert())
}

actual fun ioctlSocketFionRead(sockfd: Int): Int {
    val v = intArrayOf(0)
    ioctl(sockfd.convert(), FIONREAD, v.refTo(0))
    return v[0]
}

package com.soywiz.korio.posix

import kotlinx.cinterop.*
import platform.posix.*

actual fun posixFopen(filename: String, mode: String): CPointer<FILE>? {
    return memScoped {
        //setlocale(LC_ALL, ".UTF-8") // On Windows 10 : https://docs.microsoft.com/en-us/cpp/c-runtime-library/reference/setlocale-wsetlocale?redirectedfrom=MSDN&view=msvc-160#utf-8-support
        platform.posix._wfopen(filename.wcstr, mode.wcstr)
    }
}

actual fun posixReadlink(path: String): String? = null

actual fun posixRealpath(path: String): String = path

actual fun posixGetcwd(): String = memScoped {
    val temp = allocArray<ByteVar>(PATH_MAX + 1)
    getcwd(temp, PATH_MAX)
    temp.toKString()
}

actual fun posixMkdir(path: String, attr: Int): Int {
    return platform.posix.mkdir(path)
}

actual fun ioctlSocketFionRead(sockfd: Int): Int {
    val v = uintArrayOf(0u)
    ioctlsocket(sockfd.convert(), FIONREAD, v.refTo(0))
    return v[0].toInt()
}

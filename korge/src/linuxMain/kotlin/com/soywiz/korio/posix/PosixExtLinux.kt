package com.soywiz.korio.posix

actual val POSIX: BasePosix = LinuxPosixPosix()

open class LinuxPosixPosix : BasePosixPosix() {
}

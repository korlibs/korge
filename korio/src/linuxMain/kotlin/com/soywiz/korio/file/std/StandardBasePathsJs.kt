package com.soywiz.korio.file.std

import com.soywiz.korio.getCurrentExeFolder
import com.soywiz.korio.posix.posixReadlink
import com.soywiz.korio.posix.posixRealpath

actual object StandardPaths : StandardBasePathsNative(), StandardPathsBase {
    override val executableFile: String
        get() = posixReadlink("/proc/self/exe")
            ?: posixReadlink("/proc/curproc/file")
            ?: posixReadlink("/proc/self/path/a.out")
            ?: "./a.out"
    override val executableFolder: String get() = getCurrentExeFolder() ?: posixRealpath(".") ?: "."
}

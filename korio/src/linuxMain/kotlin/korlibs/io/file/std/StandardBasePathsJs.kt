package korlibs.io.file.std

import korlibs.io.getCurrentExeFolder
import korlibs.io.posix.posixReadlink
import korlibs.io.posix.posixRealpath

actual object StandardPaths : StandardBasePathsNative(), StandardPathsBase {
    override val executableFile: String
        get() = posixReadlink("/proc/self/exe")
            ?: posixReadlink("/proc/curproc/file")
            ?: posixReadlink("/proc/self/path/a.out")
            ?: "./a.out"
    override val executableFolder: String get() = getCurrentExeFolder() ?: posixRealpath(".") ?: "."
}
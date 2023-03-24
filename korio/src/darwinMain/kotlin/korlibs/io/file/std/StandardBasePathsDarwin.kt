package korlibs.io.file.std

import korlibs.io.file.PathInfo
import korlibs.io.file.parent
import korlibs.io.posix.posixRealpath

open class StandardBasePathsDarwin : StandardBasePathsNative() {
    override val executableFile: String get() = kotlinx.cinterop.autoreleasepool { platform.Foundation.NSBundle.mainBundle.executablePath ?: "./a.out" }
    override val executableFolder: String get() = PathInfo(executableFile).parent.fullPath
    override val resourcesFolder: String get() = kotlinx.cinterop.autoreleasepool { platform.Foundation.NSBundle.mainBundle.resourcePath ?: posixRealpath(".") ?: "." }
}

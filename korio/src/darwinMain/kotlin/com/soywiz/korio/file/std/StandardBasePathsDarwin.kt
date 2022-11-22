package com.soywiz.korio.file.std

import com.soywiz.korio.file.PathInfo
import com.soywiz.korio.file.parent
import com.soywiz.korio.posix.posixRealpath

open class StandardBasePathsDarwin : StandardBasePathsNative() {
    override val executableFile: String get() = kotlinx.cinterop.autoreleasepool { platform.Foundation.NSBundle.mainBundle.executablePath ?: "./a.out" }
    override val executableFolder: String get() = PathInfo(executableFile).parent.fullPath
    override val resourcesFolder: String get() = kotlinx.cinterop.autoreleasepool { platform.Foundation.NSBundle.mainBundle.resourcePath ?: posixRealpath(".") ?: "." }
}

package korlibs.io

import korlibs.io.posix.*
import kotlinx.cinterop.*
import platform.posix.*

actual fun nativeCwd(): String = kotlinx.cinterop.autoreleasepool { platform.Foundation.NSBundle.mainBundle.resourcePath ?: posixRealpath(".") ?: "." }

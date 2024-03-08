package korlibs.io

import korlibs.io.posix.*
import kotlinx.cinterop.*
import platform.Foundation.*
import platform.posix.*

fun nativeCwdOrNull(): String? = kotlinx.cinterop.autoreleasepool {
    platform.Foundation.NSBundle.mainBundle.resourcePath
}
actual fun nativeCwd(): String = nativeCwdOrNull() ?: posixRealpath(".") ?: "."


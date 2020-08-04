package com.soywiz.korio

import kotlinx.cinterop.*
import platform.posix.*

actual fun nativeCwd(): String = kotlinx.cinterop.autoreleasepool { platform.Foundation.NSBundle.mainBundle.resourcePath ?: realpath(".") ?: "." }

package com.soywiz.korio

import com.soywiz.korio.posix.*
import kotlinx.cinterop.*
import platform.posix.*

actual fun nativeCwd(): String = kotlinx.cinterop.autoreleasepool { platform.Foundation.NSBundle.mainBundle.resourcePath ?: posixRealpath(".") ?: "." }

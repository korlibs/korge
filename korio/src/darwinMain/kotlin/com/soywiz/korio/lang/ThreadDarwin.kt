package com.soywiz.korio.lang

import kotlinx.cinterop.objcPtr
import platform.Foundation.NSThread

actual val currentThreadId: Long get() = NSThread.currentThread.objcPtr().toLong()

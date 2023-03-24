package korlibs.io.lang

import kotlinx.cinterop.objcPtr
import platform.Foundation.NSThread

actual val currentThreadId: Long get() = NSThread.currentThread.objcPtr().toLong()
actual val currentThreadName: String? get() = "Thread-$currentThreadId"
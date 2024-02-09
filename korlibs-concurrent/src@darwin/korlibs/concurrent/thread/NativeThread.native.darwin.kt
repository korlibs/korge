package korlibs.concurrent.thread

import kotlinx.cinterop.*

actual val __currentThreadId: Long get() = NSThread.currentThread.objcPtr().toLong()

package korlibs.concurrent.thread

import kotlinx.cinterop.*
import platform.Foundation.*

actual val __currentThreadId: Long get() = NSThread.currentThread.objcPtr().toLong()

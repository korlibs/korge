package korlibs.concurrent.thread

import kotlinx.cinterop.*
import platform.posix.*

actual val __currentThreadId: Long get() = pthread_self().toLong()

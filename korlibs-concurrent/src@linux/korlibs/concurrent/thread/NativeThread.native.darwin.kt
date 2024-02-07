package korlibs.concurrent.thread

import platform.posix.*

actual val __currentThreadId: Long get() = pthread_self().toLong()

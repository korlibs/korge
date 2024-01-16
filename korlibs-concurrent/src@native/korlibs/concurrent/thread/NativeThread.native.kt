@file:Suppress("PackageDirectoryMismatch")
package korlibs.concurrent.thread

import platform.posix.*
import kotlin.native.concurrent.*
import kotlin.native.runtime.*
import kotlin.time.*

actual class NativeThread actual constructor(val code: (NativeThread) -> Unit) {
    actual var isDaemon: Boolean = false
    actual var userData: Any? = null

    actual var threadSuggestRunning: Boolean = true
    var worker: Worker? = null

    actual var priority: Int = 0
    actual var name: String? = null

    actual fun start() {
        threadSuggestRunning = true
        worker = Worker.start()
        worker?.executeAfter {
            try {
                code(this)
            } finally {
                worker?.requestTermination()
                worker = null
            }
        }
    }

    actual fun interrupt() {
        // No operation
        threadSuggestRunning = false
        worker = null
    }

    actual companion object {
        actual val isSupported: Boolean get() = true
        actual val currentThreadId: Long get() = korlibs.concurrent.thread.__currentThreadId
        actual val currentThreadName: String? get() = "Thread-$currentThreadId"

        @OptIn(NativeRuntimeApi::class)
        actual fun gc(full: Boolean) {
            GC.schedule()
        }

        actual fun sleep(time: Duration): Unit {
            //platform.posix.nanosleep()
            platform.posix.usleep(time.inWholeMicroseconds.toUInt())

        }
        actual inline fun spinWhile(cond: () -> Boolean): Unit {
            while (cond()) {
                // @TODO: try to improve performance like: Thread.onSpinWait() or SpinWait.SpinUntil
                Unit
            }
        }
    }
}

internal expect val __currentThreadId: Long

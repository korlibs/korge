@file:Suppress("PackageDirectoryMismatch")

package korlibs.datastructure.thread

import korlibs.datastructure.*
import korlibs.time.*
import kotlinx.cinterop.*
import platform.Foundation.*
import kotlin.native.concurrent.*
import kotlin.native.runtime.*

actual class NativeThread actual constructor(val code: (NativeThread) -> Unit) : Extra by Extra.Mixin() {
    actual var isDaemon: Boolean = false

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
        actual val currentThreadId: Long get() = NSThread.currentThread.objcPtr().toLong()
        actual val currentThreadName: String? get() = "Thread-$currentThreadId"

        @OptIn(NativeRuntimeApi::class)
        actual fun gc(full: Boolean) {
            GC.schedule()
        }

        actual fun sleep(time: TimeSpan): Unit {
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

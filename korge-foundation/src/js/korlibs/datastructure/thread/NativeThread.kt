package korlibs.datastructure.thread

import korlibs.time.*
import kotlin.time.*

actual class NativeThread actual constructor(val code: () -> Unit) {
    actual var isDaemon: Boolean = false

    actual fun start() {
        TODO()
    }

    actual fun interrupt() {
        TODO()
    }

    actual companion object {
        actual val isSupported: Boolean get() = false

        val warnSleep by lazy {
            println("!!! Sync sleeping on JS")
        }

        actual fun gc(full: Boolean) {
        }

        actual fun sleep(time: TimeSpan) {
            warnSleep
            val start = TimeSource.Monotonic.markNow()
            spinWhile { start.elapsedNow() < time }
        }

        actual inline fun spinWhile(cond: () -> Boolean): Unit {
            while (cond()) {
                // @TODO: try to improve performance like: Thread.onSpinWait() or SpinWait.SpinUntil
                Unit
            }
        }

        actual val currentThreadId: Long get() = 1L
        actual val currentThreadName: String? get() = "Thread-$currentThreadId"
    }
}

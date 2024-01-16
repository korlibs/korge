package korlibs.concurrent.thread

import kotlin.time.*

actual class NativeThread actual constructor(val code: (NativeThread) -> Unit) {
    actual var userData: Any? = null
    actual var isDaemon: Boolean = false
    actual var threadSuggestRunning = true

    actual fun start() {
        threadSuggestRunning = true
        TODO()
    }

    actual fun interrupt() {
        threadSuggestRunning = false
        TODO()
    }

    actual companion object {
        actual val isSupported: Boolean get() = false

        val warnSleep by lazy {
            println("!!! Sync sleeping on JS")
        }

        actual fun gc(full: Boolean) {
        }

        actual fun sleep(time: Duration) {
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

    actual var priority: Int
        get() = 0
        set(value) {}
    actual var name: String? = "Thread-JS"
}

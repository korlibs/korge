package korlibs.concurrent.thread

import kotlin.time.*
import kotlin.time.Duration.Companion.seconds

private fun Duration.toMillisNanos(): Pair<Long, Int> {
    val nanoSeconds = inWholeNanoseconds
    val millis = (nanoSeconds / 1_000_000L)
    val nanos = (nanoSeconds % 1_000_000L).toInt()
    return millis to nanos
}

actual class NativeThread actual constructor(val code: (NativeThread) -> Unit) {
    val thread = Thread { code(this) }
    actual var userData: Any? = null

    actual var threadSuggestRunning = true

    actual var priority: Int by thread::priority
    actual var name: String? by thread::name

    actual var isDaemon: Boolean
        get() = thread.isDaemon
        set(value) { thread.isDaemon = value }

    actual fun start() {
        threadSuggestRunning = true
        thread.start()
    }

    actual fun interrupt() {
        threadSuggestRunning = false
        // No operation
        thread.interrupt()
    }

    actual companion object {
        actual val isSupported: Boolean get() = true

        actual val currentThreadId: Long get() = Thread.currentThread().id
        actual val currentThreadName: String? get() = Thread.currentThread().name

        private val java_lang_Thread = Class.forName("java.lang.Thread")
        @PublishedApi internal val onSpinWait = runCatching { java_lang_Thread.getMethod("onSpinWait") }.getOrNull()

        actual fun gc(full: Boolean) {
            System.gc()
        }

        actual fun sleep(time: Duration) {
            //val gcTime = measureTime { System.gc() }
            //val compensatedTime = time - gcTime
            val compensatedTime = time
            if (compensatedTime > 0.seconds) {
                val (millis, nanos) = compensatedTime.toMillisNanos()
                Thread.sleep(millis, nanos)
            }
        }
        actual inline fun spinWhile(cond: () -> Boolean): Unit {
            //println("onSpinWait=$onSpinWait")
            while (cond()) {
                onSpinWait?.invoke(null)
            }

        }
    }
}

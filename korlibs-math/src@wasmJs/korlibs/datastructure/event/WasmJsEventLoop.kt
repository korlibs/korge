package korlibs.datastructure.event

import korlibs.datastructure.closeable.*
import korlibs.time.*
import kotlinx.browser.*

actual fun createPlatformEventLoop(precise: Boolean): SyncEventLoop =
    LocalJsEventLoop(precise)

open class LocalJsEventLoop(
    precise: Boolean = false,
    immediateRun: Boolean = false,
) : SyncEventLoop(precise, immediateRun) {
    private var closeable: Closeable? = null

    override fun start() {
        if (closeable != null) return

        closeable = JsEventLoop.setIntervalFrame {
            runAvailableNextTask()
        }
    }

    override fun stop() {
        closeable?.close()
        closeable = null
    }

    override fun close() {
        stop()
    }
}

object JsEventLoop : BaseEventLoop() {
    override var paused: Boolean = false

    override fun close() = Unit
    override fun setImmediate(task: () -> Unit) {
        window.setTimeout({ task(); null }, 0)
    }

    override fun setTimeout(time: TimeSpan, task: () -> Unit): Closeable {
        val id = window.setTimeout({ task(); null }, time.millisecondsInt)
        return Closeable { window.clearTimeout(id) }
    }

    override fun setInterval(time: TimeSpan, task: () -> Unit): Closeable {
        val id = window.setInterval({ task(); null }, time.millisecondsInt)
        return Closeable { window.clearInterval(id) }
    }

    override fun setIntervalFrame(task: () -> Unit): Closeable {
        var running = true
        var gen: (() -> Unit) ? = null
        gen = {
            if (running) {
                task()
                window.requestAnimationFrame { gen?.invoke() }
            }
        }
        gen()
        return Closeable { running = false }
    }
}

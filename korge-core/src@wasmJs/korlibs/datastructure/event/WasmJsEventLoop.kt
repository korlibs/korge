package korlibs.datastructure.event

import korlibs.time.*
import kotlinx.browser.*

actual fun createPlatformEventLoop(precise: Boolean): SyncEventLoop =
    LocalJsEventLoop()

open class LocalJsEventLoop(
    immediateRun: Boolean = false,
) : SyncEventLoop(immediateRun) {
    private var closeable: AutoCloseable? = null

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

    override fun setTimeout(time: TimeSpan, task: () -> Unit): AutoCloseable {
        val id = window.setTimeout({ task(); null }, time.millisecondsInt)
        return AutoCloseable { window.clearTimeout(id) }
    }

    override fun setInterval(time: TimeSpan, task: () -> Unit): AutoCloseable {
        val id = window.setInterval({ task(); null }, time.millisecondsInt)
        return AutoCloseable { window.clearInterval(id) }
    }

    override fun setIntervalFrame(task: () -> Unit): AutoCloseable {
        var running = true
        var gen: (() -> Unit)? = null
        gen = {
            if (running) {
                task()
                window.requestAnimationFrame { gen?.invoke() }
            }
        }
        gen()
        return AutoCloseable { running = false }
    }
}

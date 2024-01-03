package korlibs.datastructure.event

import korlibs.datastructure.closeable.*
import korlibs.datastructure.internal.platform.jsGlobalThis
import korlibs.time.*

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
        jsGlobalThis.setTimeout({ task() }, 0)
    }

    override fun setTimeout(time: TimeSpan, task: () -> Unit): Closeable {
        val id = jsGlobalThis.setTimeout({ task() }, time.millisecondsInt)
        return Closeable { jsGlobalThis.clearTimeout(id) }
    }

    override fun setInterval(time: TimeSpan, task: () -> Unit): Closeable {
        val id = jsGlobalThis.setInterval({ task() }, time.millisecondsInt)
        return Closeable { jsGlobalThis.clearInterval(id) }
    }

    override fun setIntervalFrame(task: () -> Unit): Closeable {
        val globalThisDyn = jsGlobalThis.asDynamic()
        if (!globalThisDyn.requestAnimationFrame) return super.setIntervalFrame(task)
        var running = true
        var gen: (() -> Unit) ? = null
        gen = {
            if (running) {
                task()
                globalThisDyn.requestAnimationFrame(gen)
            }
        }
        gen()
        return Closeable { running = false }
    }
}

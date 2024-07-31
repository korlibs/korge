package korlibs.datastructure.event

import korlibs.datastructure.closeable.*
import korlibs.platform.*
import korlibs.time.*

actual fun createPlatformEventLoop(precise: Boolean): SyncEventLoop =
    LocalJsEventLoop(precise)

open class LocalJsEventLoop(
    precise: Boolean = false,
    immediateRun: Boolean = false,
) : SyncEventLoop(precise) {
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
        jsGlobalThis.setTimeout({ task() }, 0)
    }

    override fun setTimeout(time: TimeSpan, task: () -> Unit): AutoCloseable {
        val id = jsGlobalThis.setTimeout({ task() }, time.millisecondsInt)
        return AutoCloseable { jsGlobalThis.clearTimeout(id) }
    }

    override fun setInterval(time: TimeSpan, task: () -> Unit): AutoCloseable {
        val id = jsGlobalThis.setInterval({ task() }, time.millisecondsInt)
        return AutoCloseable { jsGlobalThis.clearInterval(id) }
    }

    override fun setIntervalFrame(task: () -> Unit): AutoCloseable {
        val globalThisDyn = jsGlobalThis.asDynamic()
        if (!globalThisDyn.requestAnimationFrame) return super.setIntervalFrame(task)
        var running = true
        var gen: (() -> Unit)? = null
        gen = {
            if (running) {
                task()
                globalThisDyn.requestAnimationFrame(gen)
            }
        }
        gen()
        return AutoCloseable { running = false }
    }
}

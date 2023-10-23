package korlibs.datastructure.event

import korlibs.datastructure.closeable.*
import korlibs.platform.*
import korlibs.time.*

object JsEventLoop : BaseEventLoop() {
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
}

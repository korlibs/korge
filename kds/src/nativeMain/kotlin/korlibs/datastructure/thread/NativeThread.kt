package korlibs.datastructure.thread

import kotlin.native.concurrent.*

actual class NativeThread actual constructor(val code: () -> Unit) {
    actual var isDaemon: Boolean = false

    actual fun start() {
        val worker = Worker.start()
        worker.executeAfter {
            try {
                code()
            } finally {
                worker.requestTermination()
            }
        }
    }

    actual fun interrupt() {
        // No operation
    }
}
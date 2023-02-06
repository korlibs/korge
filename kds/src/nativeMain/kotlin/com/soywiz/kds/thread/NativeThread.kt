package com.soywiz.kds.thread

import kotlin.native.concurrent.*

class NativeThread(val code: () -> Unit) {
    var isDaemon: Boolean = false

    fun start() {
        val worker = Worker.start()
        worker.executeAfter {
            try {
                code()
            } finally {
                worker.requestTermination()
            }
        }
    }

    fun interrupt() {
        // No operation
    }
}

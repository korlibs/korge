package korlibs.io.async

import korlibs.time.*

// @TODO: Use select when waiting for sockets, and integrate it in the eventloop?
suspend fun <T> kotlin.native.concurrent.Future<T>.await(): T {
	var n = 0
    var delayCount = 0
    val awaitTime = measureTime {
        while (this.state != kotlin.native.concurrent.FutureState.COMPUTED) {
            when (this.state) {
                kotlin.native.concurrent.FutureState.INVALID -> error("Error in worker")
                kotlin.native.concurrent.FutureState.CANCELLED -> kotlinx.coroutines.CancellationException("cancelled")
                kotlin.native.concurrent.FutureState.THROWN -> error("Worker thrown exception")
                else -> {
                    kotlinx.coroutines.delay(((n++).toDouble() / 3.0).toLong())
                    delayCount++
                }
            }
        }
    }
    //println("Future.await: delayCount=$delayCount, time=$awaitTime")
	return this.result
}

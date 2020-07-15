package com.soywiz.korio.async

suspend fun <T> kotlin.native.concurrent.Future<T>.await(): T {
	var n = 0
	while (this.state != kotlin.native.concurrent.FutureState.COMPUTED) {
		when (this.state) {
			kotlin.native.concurrent.FutureState.INVALID -> error("Error in worker")
			kotlin.native.concurrent.FutureState.CANCELLED -> kotlinx.coroutines.CancellationException("cancelled")
			kotlin.native.concurrent.FutureState.THROWN -> error("Worker thrown exception")
			else -> kotlinx.coroutines.delay(((n++).toDouble() / 3.0).toLong())
		}
	}
	return this.result
}

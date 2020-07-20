package com.soywiz.korio.async

import com.soywiz.klock.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

suspend fun delay(time: TimeSpan): Unit = kotlinx.coroutines.delay(time.millisecondsLong)
suspend fun CoroutineContext.delay(time: TimeSpan) = kotlinx.coroutines.delay(time.millisecondsLong)

suspend fun <T> withTimeout(time: TimeSpan, block: suspend CoroutineScope.() -> T): T {
	return if (time == TimeSpan.NIL) {
		block(CoroutineScope(coroutineContext))
	} else {
		kotlinx.coroutines.withTimeout(time.millisecondsLong, block)
	}
}

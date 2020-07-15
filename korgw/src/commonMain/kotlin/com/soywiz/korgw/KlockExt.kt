package com.soywiz.korgw

import com.soywiz.klock.hr.HRTimeSpan
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.coroutineContext

suspend fun delay(time: HRTimeSpan) {
    if (time.microsecondsDouble <= 0.0) return // don't delay
    val dispatcher = coroutineContext[ContinuationInterceptor] as? GameWindowCoroutineDispatcher
    if (dispatcher != null) {
        return suspendCancellableCoroutine sc@ { cont: CancellableContinuation<Unit> ->
            dispatcher.scheduleResumeAfterDelay(time, cont)
        }
    } else {
        // Use default delay dispatcher
        delay(time.millisecondsDouble.toLong())
    }
}

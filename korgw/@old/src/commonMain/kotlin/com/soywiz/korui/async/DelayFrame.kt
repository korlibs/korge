package com.soywiz.korui.async

import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

interface DelayFrame {
    fun delayFrame(continuation: CancellableContinuation<Unit>) {
        //continuation.context.get(ContinuationInterceptor) as? Delay ?: DefaultDelay
        launchImmediately(continuation.context) {
            delay(16)
            continuation.resume(Unit)
        }
    }
}

val DefaultDelayFrame: DelayFrame = object : DelayFrame {}
val CoroutineContext.delayFrame: DelayFrame get() = get(ContinuationInterceptor) as? DelayFrame ?: DefaultDelayFrame

suspend fun DelayFrame.delayFrame() = suspendCancellableCoroutine<Unit> { c -> delayFrame(c) }
suspend fun delayFrame() = coroutineContext.delayFrame.delayFrame()

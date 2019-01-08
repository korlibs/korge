package com.soywiz.korge.async

import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import com.soywiz.korui.async.*
import kotlinx.coroutines.*

internal fun CoroutineScope.animationFrameLoopKorge(callback: suspend (Closeable) -> Unit): Closeable {
    var job: Job? = null
    val close = Closeable {
        job?.cancel()
    }
    job = launchImmediately {
        while (true) {
            callback(close)
            delayFrame()
        }
    }
    return  close
}

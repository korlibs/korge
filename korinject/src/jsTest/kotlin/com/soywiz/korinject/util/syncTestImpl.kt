package com.soywiz.korinject.util

import kotlin.coroutines.*

internal val _global = js("((typeof global !== 'undefined') ? global : window)")

actual fun syncTestImpl(ignoreJs: Boolean, block: suspend () -> Unit) {
    if (ignoreJs) return

    _global.testPromise = kotlin.js.Promise<Unit> { resolve, reject ->
        block.startCoroutine(object : Continuation<Unit> {
            override val context: CoroutineContext = EmptyCoroutineContext

            override fun resumeWith(result: Result<Unit>) {
                val exception = result.exceptionOrNull()
                if (exception != null) {
                    reject(exception)
                } else {
                    resolve(result.getOrThrow())
                }
            }
        })
    }
}

package com.soywiz.korte.vertx.internal

import io.vertx.core.buffer.*
import io.vertx.core.http.*
import kotlin.coroutines.*

internal suspend fun HttpClientRequest.readString(): String {
    val data = Buffer.buffer()
    suspendCoroutine<Unit> { c ->
        handler {
            it.handler { data.appendBuffer(it) }
            it.endHandler { c.resume(Unit) }
        }.exceptionHandler {
            c.resumeWithException(it)
        }.end()
    }
    return data.toString(Charsets.UTF_8)
}

internal fun <T> runBlocking(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    callback: suspend () -> T
): T {
    var fresult: Result<T>? = null

    callback.startCoroutine(object : Continuation<T> {
        override val context: CoroutineContext = coroutineContext
        override fun resumeWith(result: Result<T>) = run { fresult = result }
    })

    while (fresult == null) Thread.sleep(1L)

    val mresult = fresult
    if (mresult != null) {
        return mresult.getOrThrow()
    } else {
        throw RuntimeException("Unexpected error")
    }
}

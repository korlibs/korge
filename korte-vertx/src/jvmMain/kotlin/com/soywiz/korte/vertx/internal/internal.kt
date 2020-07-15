package com.soywiz.korte.vertx.internal

import com.soywiz.korte.util.*
import io.vertx.core.*
import kotlin.coroutines.*

internal fun <T> Handler<AsyncResult<T>>.handle(coroutineContext: CoroutineContext, callback: suspend () -> T) {
    val handler = this
    callback.startCoroutine(object : Continuation<T> {
        override val context: CoroutineContext = coroutineContext

        override fun resumeWith(result: Result<T>) {
            handler.handle(VxAsyncResult(result.getOrNull(), result.exceptionOrNull()))
        }
    })
}

internal class VxAsyncResult<T>(val result: T?, val exception: Throwable?) : AsyncResult<T> {
    override fun succeeded(): Boolean = exception == null
    override fun failed(): Boolean = exception != null
    override fun result(): T = result!!
    override fun cause(): Throwable = exception!!
}

internal class DeferredHandler<T> : Handler<AsyncResult<T>> {
    val deferred = KorteDeferred<T>()
    override fun handle(event: AsyncResult<T>) {
        if (event.failed()) {
            deferred.completeExceptionally(event.cause())
        } else {
            deferred.complete(event.result())
        }
    }
}

internal suspend fun <T> vx(callback: (Handler<AsyncResult<T>>) -> Unit): T =
    DeferredHandler<T>().also(callback).deferred.await()

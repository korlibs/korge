package com.soywiz.korio.async

import com.soywiz.korio.experimental.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.js.*

/**
 * An simple interface compatible with JS Promise used for interop. In other cases just use [CompletableDeferred] instead.
 */
@JsName("Promise")
@KorioExperimentalApi
interface Promise<T> {
    @JsName("then")
    fun <S> then(onFulfilled: ((T) -> S)?, onRejected: ((Throwable) -> S)?): Promise<S>
}

/**
 * An simple interface compatible with JS Promise used for interop. In other cases just use [CompletableDeferred] instead.
 */
@KorioExperimentalApi
expect fun <T> Promise(coroutineContext: CoroutineContext = EmptyCoroutineContext, executor: (resolve: (T) -> Unit, reject: (Throwable) -> Unit) -> Unit): Promise<T>

@KorioExperimentalApi
suspend fun <T> SPromise(executor: (resolve: (T) -> Unit, reject: (Throwable) -> Unit) -> Unit): Promise<T> = Promise(coroutineContext, executor)

internal class DeferredPromise<T>(
    val deferred: Deferred<T>,
    val coroutineContext: CoroutineContext
) : Promise<T> {
    override fun <S> then(onFulfilled: ((T) -> S)?, onRejected: ((Throwable) -> S)?): Promise<S> {
        val chainedPromise = DeferredPromise(CompletableDeferred<S>(), coroutineContext)

        launchImmediately(coroutineContext) {
            var result: S? = null
            try {
                result = onFulfilled?.invoke(deferred.await())
            } catch (e: Throwable) {
                result = onRejected?.invoke(e)
            }
            if (result != null) {
                (chainedPromise.deferred as CompletableDeferred<S>).complete(result)
            }
        }

        return chainedPromise
    }
}

fun <T> Deferred<T>.toPromise(coroutineContext: CoroutineContext): Promise<T> = DeferredPromise(this, coroutineContext)
suspend fun <T> Deferred<T>.toPromise(): Promise<T> = toPromise(coroutineContext)

fun Job.toPromise(coroutineContext: CoroutineContext): Promise<Unit> {
    val deferred = CompletableDeferred<Unit>()
    this.invokeOnCompletion {
        if (it != null) {
            deferred.completeExceptionally(it)
        } else {
            deferred.complete(Unit)
        }
    }
    return deferred.toPromise(coroutineContext)
}

suspend fun Job.toPromise(): Promise<Unit> = toPromise(coroutineContext)

fun <T> Promise<T>.toDeferred(): Deferred<T> {
    val out = CompletableDeferred<T>()
    this.then({ out.complete(it) }, { out.completeExceptionally(it) })
    return out
}

suspend fun <T> Promise<T>.await(): T = suspendCoroutine { c ->
    this.then({ c.resume(it) }, { c.resumeWithException(it) })
}

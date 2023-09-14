package korlibs.io.async

import korlibs.io.experimental.KorioExperimentalApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.JsName

/**
 * A simple interface compatible with JS Promise used for interop. In other cases just use [CompletableDeferred] instead.
 */
@JsName("Promise")
@KorioExperimentalApi
@Deprecated("Use CompletableDeferred instead")
interface Promise<T> {
    @JsName("then")
    fun <S> then(onFulfilled: ((T) -> S)?, onRejected: ((Throwable) -> S)?): Promise<S>
}

/**
 * A simple interface compatible with JS Promise used for interop. In other cases just use [CompletableDeferred] instead.
 */
@KorioExperimentalApi
@Deprecated("Use CompletableDeferred instead")
expect fun <T> Promise(coroutineContext: CoroutineContext = EmptyCoroutineContext, executor: (resolve: (T) -> Unit, reject: (Throwable) -> Unit) -> Unit): Promise<T>

@KorioExperimentalApi
@Deprecated("Use CompletableDeferred instead")
suspend fun <T> SPromise(executor: (resolve: (T) -> Unit, reject: (Throwable) -> Unit) -> Unit): Promise<T> = Promise(coroutineContext, executor)

@Deprecated("Use CompletableDeferred instead")
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
                // @TODO: Is this the expected behaviour here?
                if (e is CancellationException) throw e
                result = onRejected?.invoke(e)
            }
            if (result != null) {
                (chainedPromise.deferred as CompletableDeferred<S>).complete(result)
            }
        }

        return chainedPromise
    }
}

@Deprecated("Use CompletableDeferred instead")
fun <T> Deferred<T>.toPromise(coroutineContext: CoroutineContext): Promise<T> = DeferredPromise(this, coroutineContext)
@Deprecated("Use CompletableDeferred instead")
suspend fun <T> Deferred<T>.toPromise(): Promise<T> = toPromise(coroutineContext)

@Deprecated("Use CompletableDeferred instead")
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

@Deprecated("Use CompletableDeferred instead")
suspend fun Job.toPromise(): Promise<Unit> = toPromise(coroutineContext)

@Deprecated("Use CompletableDeferred instead")
fun <T> Promise<T>.toDeferred(): Deferred<T> {
    val out = CompletableDeferred<T>()
    this.then({ out.complete(it) }, { out.completeExceptionally(it) })
    return out
}

@Deprecated("Use CompletableDeferred instead")
suspend fun <T> Promise<T>.await(): T = suspendCancellableCoroutine { c ->
    this.then({ c.resume(it) }, { c.resumeWithException(it) })
    //c.invokeOnCancellation { c.cancel() }
}

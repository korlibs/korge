package korlibs.io.util

import kotlinx.coroutines.*
import java.nio.channels.CompletionHandler
import kotlin.coroutines.*

fun <T> CancellableContinuation<T>.getCompletionHandler(): CompletionHandler<T, Unit> {
    val continuation = this
    var cancelled = false
    invokeOnCancellation {
        cancelled = true
    }
    //println("JvmNioAsyncClient.Started in thread ${Thread.currentThread().id}")

    return object : CompletionHandler<T, Unit> {
        override fun completed(result: T, attachment: Unit?) {
            //println("JvmNioAsyncClient.Completed result=$result in thread ${Thread.currentThread().id}")
            if (!cancelled) continuation.resume(result)
        }
        override fun failed(exc: Throwable, attachment: Unit?) { if (!cancelled) continuation.resumeWithException(exc) }
    }
}

suspend fun <T> nioSuspendCompletion(
    block: (CompletionHandler<T, Unit>) -> Unit
): T = suspendCancellableCoroutine {
    block(it.getCompletionHandler())
}

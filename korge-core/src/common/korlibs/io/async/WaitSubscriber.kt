package korlibs.io.async

import korlibs.io.lang.Cancellable
import korlibs.io.lang.Closeable
import korlibs.io.lang.cancel
import korlibs.io.lang.cancellable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred

suspend fun <T> waitSubscriber(block: ((T) -> Unit) -> Cancellable): T {
    val deferred = CompletableDeferred<T>()
    @Suppress("JoinDeclarationAndAssignment")
    lateinit var cancellable: Cancellable
    cancellable = block {
        cancellable.cancel()
        deferred.complete(it)
    }
    try {
        return deferred.await()
    } catch (e: CancellationException) {
        cancellable.cancel()
        throw e
    }
}

suspend fun <T> waitSubscriberCloseable(block: ((T) -> Unit) -> Closeable): T =
    waitSubscriber { block(it).cancellable() }

package com.soywiz.korio.async

import com.soywiz.korio.lang.Cancellable
import com.soywiz.korio.lang.Closeable
import com.soywiz.korio.lang.cancel
import com.soywiz.korio.lang.cancellable
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

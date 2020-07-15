package com.soywiz.korinject

import kotlinx.coroutines.*
import kotlin.coroutines.*

suspend fun <T> withInjector(injector: AsyncInjector, block: suspend () -> T): T =
    withContext(AsyncInjectorContext(injector)) {
        block()
    }

suspend fun injector(): AsyncInjector =
    coroutineContext[AsyncInjectorContext]?.injector
        ?: error("AsyncInjector not in the context, please call withInjector function")

class AsyncInjectorContext(val injector: AsyncInjector) : CoroutineContext.Element {
    companion object : CoroutineContext.Key<AsyncInjectorContext>

    override val key get() = AsyncInjectorContext
}

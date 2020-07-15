package com.soywiz.korio.async

import kotlinx.coroutines.*
import kotlin.coroutines.*

class AsyncCache {
	@PublishedApi
	internal val promises = LinkedHashMap<String, Deferred<*>>()

	@Suppress("UNCHECKED_CAST")
	suspend operator fun <T> invoke(key: String, gen: suspend () -> T): T {
		return (promises.getOrPut(key) { asyncImmediately(coroutineContext) { gen() } } as Deferred<T>).await()
	}
}

class AsyncCacheGen<T>(private val gen: suspend (key: String) -> T) {
	@PublishedApi
	internal val promises = LinkedHashMap<String, Deferred<*>>()

	@Suppress("UNCHECKED_CAST")
	suspend operator fun invoke(key: String): T {
		return (promises.getOrPut(key) { asyncImmediately(coroutineContext) { gen(key) } } as Deferred<T>).await()
	}
}

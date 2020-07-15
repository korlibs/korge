package com.soywiz.korio.async

import com.soywiz.klock.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.reflect.*

class AsyncInmemoryCache(val timeProvider: TimeProvider = TimeProvider) {
	data class Entry(val timestamp: DateTime, val data: Deferred<Any?>)

	val cache = LinkedHashMap<String, Entry?>()

	fun <T : Any> get(clazz: KClass<T>, key: String, ttl: TimeSpan) = AsyncInmemoryEntry<T>(clazz, this, key, ttl)

	//fun <T : Any?> getTyped(clazz: Class<T>, key: String = clazz, ttl: TimeSpan) = AsyncInmemoryEntry(clazz, this, key, ttl)

	@Suppress("UNCHECKED_CAST")
	suspend fun <T : Any?> get(key: String, ttl: TimeSpan, gen: suspend () -> T): T {
		val entry = cache[key]
		if (entry == null || (timeProvider.now() - entry.timestamp) >= ttl) {
			cache[key] = Entry(timeProvider.now(), asyncImmediately(coroutineContext) { gen() })
		}
		return (cache[key]!!.data as Deferred<T>).await()
	}

	//suspend fun <T : Any?> get(key: String, ttl: TimeSpan, gen: () -> Promise<T>) = await(getAsync(key, ttl, gen))
}

class AsyncInmemoryEntry<T : Any>(
	val clazz: KClass<T>,
	val cache: AsyncInmemoryCache,
	val key: String,
	val ttl: TimeSpan
) {
	//fun getAsync(gen: () -> Promise<T>): Promise<T> = async(coroutineContext) { cache.get(key, ttl, gen) }

	suspend fun get(routine: suspend () -> T) = cache.get(key, ttl, routine)
}

package korlibs.template.internal

import korlibs.template.util.KorteDeferred
import kotlin.coroutines.coroutineContext

internal class AsyncCache {
	private val lock = KorteLock()
	@PublishedApi
	internal val deferreds = LinkedHashMap<String, KorteDeferred<*>>()

	fun invalidateAll() {
		lock { deferreds.clear() }
	}

	@Suppress("UNCHECKED_CAST")
	suspend operator fun <T> invoke(key: String, gen: suspend () -> T): T {
		val deferred = lock { (deferreds.getOrPut(key) { KorteDeferred.asyncImmediately(coroutineContext) { gen() } } as KorteDeferred<T>) }
		return deferred.await()
	}

	suspend fun <T> call(key: String, gen: suspend () -> T): T {
		return invoke(key, gen)
	}
}

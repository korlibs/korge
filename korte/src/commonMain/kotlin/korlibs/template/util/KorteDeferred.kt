package korlibs.template.util

import korlibs.template.internal.KorteLock
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine

class KorteDeferred<T> {
	private val lock = KorteLock()
	private var result: Result<T>? = null
	private val continuations = arrayListOf<Continuation<T>>()

	fun completeWith(result: Result<T>) {
		//println("completeWith: $result")
		lock {
			this.result = result
		}
		resolveIfRequired()
	}

	fun completeExceptionally(t: Throwable) = completeWith(Result.failure(t))
	fun complete(value: T) = completeWith(Result.success(value))

    // @TODO: Cancellable?
	suspend fun await(): T = suspendCoroutine { c ->
		lock {
			continuations += c
		}
		//println("await:$c")
		resolveIfRequired()
	}

	private fun resolveIfRequired() {
		val result = lock { result }
		if (result != null) {
			for (v in lock {
				if (continuations.isEmpty()) emptyList() else continuations.toList().also { continuations.clear() }
			}) {
				//println("resume:$v")
				v.resumeWith(result)
			}
		}
	}

	fun toContinuation(coroutineContext: CoroutineContext) = object : Continuation<T> {
		override val context: CoroutineContext = coroutineContext
		override fun resumeWith(result: Result<T>) = completeWith(result)
	}

	companion object {
		fun <T> asyncImmediately(coroutineContext: CoroutineContext, callback: suspend () -> T): KorteDeferred<T> =
			KorteDeferred<T>().also { deferred ->
				callback.startCoroutine(object : Continuation<T> {
					override val context: CoroutineContext = coroutineContext
					override fun resumeWith(result: Result<T>) = deferred.completeWith(result)
				})
			}
	}
}

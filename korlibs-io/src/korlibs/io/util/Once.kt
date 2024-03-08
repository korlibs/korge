package korlibs.io.util

import korlibs.io.async.asyncImmediately
import kotlinx.coroutines.Deferred
import kotlin.coroutines.coroutineContext

class Once {
	var completed = false

	inline operator fun invoke(callback: () -> Unit) {
		if (!completed) {
			completed = true
			callback()
		}
	}
}

class SyncOnce<T> {
    var value: T? = null

    operator fun invoke(callback: () -> T): T {
        if (value == null) {
            value = callback()
        }
        return value!!
    }
}

class AsyncOnce<T> {
	var promise: Deferred<T>? = null

	suspend operator fun invoke(callback: suspend () -> T): T {
        if (promise == null) {
            promise = asyncImmediately(coroutineContext) { callback() }
        }
		return promise!!.await()
	}
}

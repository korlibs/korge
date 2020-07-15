package com.soywiz.korio.util

import com.soywiz.korio.async.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

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

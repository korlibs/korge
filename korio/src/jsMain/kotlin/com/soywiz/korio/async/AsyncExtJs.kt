package com.soywiz.korio.async

import kotlinx.coroutines.*
import kotlin.coroutines.*

/*
actual fun asyncEntryPoint(callback: suspend () -> Unit): dynamic = kotlin.js.Promise<dynamic> { resolve, reject ->
	callback.startCoroutine(object : Continuation<Unit> {
		override val context: CoroutineContext = Dispatchers.Default
		override fun resumeWith(result: Result<Unit>) {
			val exception = result.exceptionOrNull()
			if (exception != null) {
                println("WARNING:: EntryPoint exception")
				reject(exception)
			} else {
				//resolve(undefined)
				resolve(Unit)
			}
		}
	})
}
*/
actual fun asyncEntryPoint(callback: suspend () -> Unit): dynamic = GlobalScope.promise { callback() }

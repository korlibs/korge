package korlibs.io.async

import korlibs.io.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.js.Promise

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
actual fun asyncEntryPoint(callback: suspend () -> Unit) {
    //promise(EmptyCoroutineContext) { callback() }
    launchImmediately(EmptyCoroutineContext) {
    //runBlockingNoJs(EmptyCoroutineContext) {
        callback()
    }
}
actual fun asyncTestEntryPoint(callback: suspend () -> Unit) {
    println("!!! asyncTestEntryPoint without returning a promise")
    //runBlockingNoJs(EmptyCoroutineContext) {
    launchImmediately(EmptyCoroutineContext) {
        callback()
    }
}

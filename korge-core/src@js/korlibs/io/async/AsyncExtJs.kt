package korlibs.io.async

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

@Suppress("ACTUAL_WITHOUT_EXPECT", "ACTUAL_TYPE_ALIAS_TO_CLASS_WITH_DECLARATION_SITE_VARIANCE")
@JsName("Promise")
actual external class AsyncEntryPointResult(func: (resolve: (Unit) -> Unit, reject: (Throwable) -> Unit) -> Unit)

actual fun asyncEntryPoint(callback: suspend () -> Unit): AsyncEntryPointResult {
    return AsyncEntryPointResult { resolve, reject ->
        launchImmediately(EmptyCoroutineContext) {
            try {
                callback()
                resolve(Unit)
            } catch (e: Throwable) {
                reject(e)
            }
        }
    }
}
actual fun asyncTestEntryPoint(callback: suspend () -> Unit): AsyncEntryPointResult = asyncEntryPoint(callback)


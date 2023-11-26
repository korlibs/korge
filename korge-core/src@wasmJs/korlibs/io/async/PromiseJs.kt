package korlibs.io.async

import kotlin.coroutines.*

actual fun <T> Promise(coroutineContext: CoroutineContext, executor: (resolve: (T) -> Unit, reject: (Throwable) -> Unit) -> Unit): Promise<T> {
    //return kotlin.js.Promise(executor).asDynamic()
    TODO()
}

fun <T> promise(coroutineContext: CoroutineContext = EmptyCoroutineContext, block: suspend () -> T): Promise<T> {
    return Promise(coroutineContext) { resolve, reject ->
        launchImmediately(coroutineContext) {
            try {
                resolve(block())
            } catch (e: Throwable) {
                reject(e)
            }
        }
    }
}

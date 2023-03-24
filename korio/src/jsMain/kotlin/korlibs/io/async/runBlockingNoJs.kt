package korlibs.io.async

import korlibs.io.lang.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

actual fun <T> runBlockingNoJs(context: CoroutineContext, block: suspend CoroutineScope.() -> T): T {
    unexpected("Calling runBlockingNoJs on JavaScript")
}

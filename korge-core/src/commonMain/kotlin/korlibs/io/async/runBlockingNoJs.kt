package korlibs.io.async

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@OptIn(ExperimentalStdlibApi::class)
internal fun runBlockingNoJs_transformContext(context: CoroutineContext): CoroutineContext {
    return context.minusKey(CoroutineDispatcher.Key).minusKey(Job.Key)
}

expect fun <T> runBlockingNoJs(context: CoroutineContext = EmptyCoroutineContext, block: suspend CoroutineScope.() -> T): T

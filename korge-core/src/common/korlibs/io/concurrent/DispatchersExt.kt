package korlibs.io.concurrent

import korlibs.io.lang.Closeable
import kotlinx.coroutines.CloseableCoroutineDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlin.coroutines.CoroutineContext

expect fun Dispatchers.createFixedThreadDispatcher(name: String, threadCount: Int = 1): CoroutineDispatcher

fun Dispatchers.createSingleThreadedDispatcher(name: String): CoroutineDispatcher = createFixedThreadDispatcher(name, 1)

fun Dispatchers.createRedirectedDispatcher(name: String, parent: CoroutineDispatcher): CoroutineDispatcher {
    return object : CoroutineDispatcher(), Closeable {
        override fun close() = Unit
        override fun dispatch(context: CoroutineContext, block: Runnable) = parent.dispatch(context, block)
        @InternalCoroutinesApi
        override fun dispatchYield(context: CoroutineContext, block: Runnable) = parent.dispatchYield(context, block)
        override fun isDispatchNeeded(context: CoroutineContext): Boolean = parent.isDispatchNeeded(context)
        @ExperimentalCoroutinesApi
        override fun limitedParallelism(parallelism: Int): CoroutineDispatcher = this
        override fun toString(): String = "Dispatcher-$name"
    }
}

@Suppress("OPT_IN_USAGE")
fun CoroutineDispatcher.close() {
    when (this) {
        is Closeable -> this.close()
        is CloseableCoroutineDispatcher -> this.close()
    }
}

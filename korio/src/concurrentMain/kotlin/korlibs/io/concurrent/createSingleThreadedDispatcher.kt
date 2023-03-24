package korlibs.io.concurrent

import korlibs.memory.Platform
import kotlinx.coroutines.*
import kotlin.coroutines.*

@OptIn(ExperimentalCoroutinesApi::class)
actual fun Dispatchers.createFixedThreadDispatcher(name: String, threadCount: Int): CoroutineDispatcher {
    //println("Dispatchers.createSingleThreadedDispatcher['$name'] : Platform.hasMultithreadedSharedHeap=${Platform.hasMultithreadedSharedHeap}")
    if (Platform.hasMultithreadedSharedHeap) {
        return newFixedThreadPoolContext(threadCount, name)
        //return newSingleThreadContext(name)
        //return createRedirectedDispatcher(name, Dispatchers.Default)
    } else {
        return createRedirectedDispatcher(name, Dispatchers.Default)
    }
}

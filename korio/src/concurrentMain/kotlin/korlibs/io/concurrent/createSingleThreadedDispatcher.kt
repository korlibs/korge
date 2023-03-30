package korlibs.io.concurrent

import kotlinx.coroutines.*

@OptIn(ExperimentalCoroutinesApi::class)
actual fun Dispatchers.createFixedThreadDispatcher(name: String, threadCount: Int): CoroutineDispatcher {
    //println("Dispatchers.createSingleThreadedDispatcher['$name'] : Platform.hasMultithreadedSharedHeap=${Platform.hasMultithreadedSharedHeap}")
    return newFixedThreadPoolContext(threadCount, name)
}

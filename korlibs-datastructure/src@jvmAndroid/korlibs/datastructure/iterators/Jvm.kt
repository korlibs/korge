package korlibs.datastructure.iterators

import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

//actual val CONCURRENCY_COUNT: Int = max(1, Runtime.getRuntime().availableProcessors() / 2)
actual val CONCURRENCY_COUNT: Int = max(1, java.lang.Runtime.getRuntime().availableProcessors())

@PublishedApi
internal val exec = Executors.newFixedThreadPool(CONCURRENCY_COUNT)

actual inline fun parallelForeach(count: Int, crossinline block: (n: Int) -> Unit) {
    if (count == 0) return

    //val futures = arrayListOf<Future<*>>()
    val countPerChunk = max(1, (count / CONCURRENCY_COUNT) + 1)

    val execCount = AtomicInteger(0)
    var m = 0
    for (start in 0 until count step countPerChunk) {
        val end = kotlin.math.min(count, start + countPerChunk)
        m++
        exec.execute {
            try {
                for (n in start until end) block(n)
            } finally {
                execCount.incrementAndGet()
            }
        }
    }
    // @TODO: Sleep thread. Use a Semaphore?
    while (execCount.get() != m) Unit
}

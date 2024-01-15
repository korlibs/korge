@file:OptIn(ExperimentalNativeApi::class)

package korlibs.datastructure.iterators

import kotlin.concurrent.AtomicInt
import kotlin.experimental.ExperimentalNativeApi
import kotlin.math.max
import kotlin.native.concurrent.Worker

@OptIn(ExperimentalStdlibApi::class)
actual val CONCURRENCY_COUNT: Int = Platform.getAvailableProcessors()

val PARALLEL_WORKERS = if (CONCURRENCY_COUNT > 1) Array(CONCURRENCY_COUNT) { Worker.start() } else emptyArray()

@PublishedApi internal class ParallelForeachChunk(val start: Int, val end: Int, val block: (Int) -> Unit)

actual inline fun parallelForeach(count: Int, crossinline block: (n: Int) -> Unit) {
    if (count == 0) return

    if (PARALLEL_WORKERS.isEmpty()) {
        for (n in 0 until count) {
            block(n)
        }
    } else {
        //val futures = arrayListOf<Future<*>>()
        val countPerChunk = max(1, count / PARALLEL_WORKERS.size + 1)
        //val rblock: (Int) -> Unit = { block(it) }

        val exec = AtomicInt(0)
        var m = 0
        for (start in 0 until count step countPerChunk) {
            val end = kotlin.math.min(count, start + countPerChunk)
            PARALLEL_WORKERS[m++].executeAfter {
                try {
                    for (n in start until end) {
                        //println("BLOCK: n=$n, start=${it.start}, end=${it.end}")
                        block(n)
                    }
                } finally {
                    exec.getAndIncrement()
                }
            }
        }
        // @TODO: Sleep thread
        while (exec.value != m) Unit
    }
}

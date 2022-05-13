package com.soywiz.kds.iterators

import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.math.max

//actual val CONCURRENCY_COUNT: Int = max(1, Runtime.getRuntime().availableProcessors() / 2)
actual val CONCURRENCY_COUNT: Int = max(1, java.lang.Runtime.getRuntime().availableProcessors())

@PublishedApi
internal val exec = Executors.newFixedThreadPool(CONCURRENCY_COUNT)

actual inline fun parallelForeach(count: Int, crossinline block: (n: Int) -> Unit) {
    if (count == 0) return

    val futures = arrayListOf<Future<*>>()
    val countPerChunk = max(1, count / CONCURRENCY_COUNT)

    for (start in 0 until count step countPerChunk) {
        futures.add(exec.submit {
            for (n in start until kotlin.math.min(count, start + countPerChunk)) {
                block(n)
            }
        })
    }

    futures.fastForEach { it.get() }
}

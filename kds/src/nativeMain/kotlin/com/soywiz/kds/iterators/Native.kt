package com.soywiz.kds.iterators

actual val CONCURRENCY_COUNT: Int = 1

actual inline fun parallelForeach(count: Int, crossinline block: (n: Int) -> Unit): Unit {
    for (n in 0 until count) block(n)
}

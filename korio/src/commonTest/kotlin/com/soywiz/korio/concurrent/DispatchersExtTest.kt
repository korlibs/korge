package com.soywiz.korio.concurrent

import com.soywiz.kds.algo.Historiogram
import com.soywiz.klock.milliseconds
import com.soywiz.kmem.Platform
import com.soywiz.korio.async.delay
import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.lang.currentThreadId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals

class DispatchersExtTest {
    @Test
    fun test() = suspendTest {
        val historiogram = Historiogram()
        val dispatcher = Dispatchers.createFixedThreadDispatcher("hello", threadCount = 4)
        try {
            withContext(dispatcher) {
                for (n in 0 until 2048) {
                    historiogram.add(currentThreadId.toInt())
                    yield()
                }
            }
            val map = historiogram.getMapCopy().toMap()
            assertEquals(if (Platform.hasMultithreadedSharedHeap) "4:2048" else "1:2048", "${map.size}:${map.values.sum()}")
        } finally {
            dispatcher.close()
        }
    }
}

package com.soywiz.klock

import kotlin.test.Test
import kotlin.test.assertEquals

class StopwatchTest {
    @Test
    fun test() {
        var nanos = 0.0
        val stopwatch = Stopwatch { nanos }
        fun check() = stopwatch.elapsedNanoseconds
        assertEquals(0.0, check()).also { nanos++ }
        assertEquals(0.0, check()).also { nanos++ }
        assertEquals(0.0, check())
        stopwatch.start()
        assertEquals(0.0, check()).also { nanos++ }
        assertEquals(1.0, check()).also { nanos++ }
        assertEquals(2.0, check())
        stopwatch.stop()
        assertEquals(2.0, check()).also { nanos++ }
        assertEquals(2.0, check())
        stopwatch.start()
        assertEquals(0.0, check()).also { nanos++ }
        assertEquals(1.0, check()).also { nanos++ }
    }
}

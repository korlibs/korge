package com.soywiz.klock

class Stopwatch(val nanosecondProvider: () -> Double = { PerformanceCounter.nanoseconds }) {
    private var running = false
    private var nanoseconds = 0.0
    private val clock get() = nanosecondProvider()
    private fun setStart() = run { nanoseconds = clock }
    init {
        setStart()
    }
    fun start() = this.apply {
        setStart()
        running = true
    }
    fun stop() = this.apply {
        nanoseconds = elapsedNanoseconds
        running = false
    }
    val elapsedNanoseconds get() = if (running) clock - nanoseconds else nanoseconds
    val elapsedMicroseconds get() = elapsedNanoseconds * 1000
    val elapsed: TimeSpan get() = elapsedNanoseconds.nanoseconds
}

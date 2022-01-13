package com.soywiz.klock

/**
 * Executes a [callback] and measure the time it takes to complete.
 */
inline fun measureTime(callback: () -> Unit): TimeSpan {
    val start = PerformanceCounter.microseconds
    callback()
    val end = PerformanceCounter.microseconds
    return (end - start).microseconds
}

/**
 * Executes the [callback] measuring the time it takes to complete.
 * Returns a [TimedResult] with the time and the return value of the callback.
 */
inline fun <T> measureTimeWithResult(callback: () -> T): TimedResult<T> {
    val start = PerformanceCounter.microseconds
    val result = callback()
    val end = PerformanceCounter.microseconds
    val elapsed = (end - start).microseconds
    return TimedResult(result, elapsed)
}

/**
 * Represents a [result] associated to a [time].
 */
data class TimedResult<T>(val result: T, val time: TimeSpan)

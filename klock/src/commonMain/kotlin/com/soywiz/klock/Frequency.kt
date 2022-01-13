package com.soywiz.klock

import com.soywiz.klock.hr.hr
import kotlin.jvm.JvmInline

val TimeSpan.hz get() = timesPerSecond
val Int.hz get() = timesPerSecond
val Double.hz get() = timesPerSecond

fun TimeSpan.toFrequency() = timesPerSecond

val TimeSpan.timesPerSecond get() = Frequency(1.0 / this.seconds)
val Int.timesPerSecond get() = Frequency(this.toDouble())
val Double.timesPerSecond get() = Frequency(this)

@JvmInline
value class Frequency(val hertz: Double) {
    companion object {
        fun from(timeSpan: TimeSpan) = timeSpan.toFrequency()
    }

    val timeSpan get() = (1.0 / this.hertz).seconds
    val hrTimeSpan get() = (1.0 / this.hertz).seconds.hr
}

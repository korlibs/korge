package com.soywiz.korge.view

import com.soywiz.klock.Frequency
import com.soywiz.klock.timesPerSecond
import com.soywiz.korio.lang.Cancellable

val Double.fps get() = this.timesPerSecond
val Int.fps get() = this.timesPerSecond

fun <T : View> T.addUpdater(referenceFps: Frequency, first: Boolean = true, updatable: T.(scale: Double) -> Unit): Cancellable {
    val time = referenceFps.timeSpan
    return addUpdater(first) {
        updatable(it / time)
    }
}

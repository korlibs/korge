package com.soywiz.korge.view

import com.soywiz.klock.Frequency
import com.soywiz.klock.timesPerSecond
import com.soywiz.korio.lang.Cancellable
import com.soywiz.korma.geom.IPoint
import com.soywiz.korma.geom.Point

val Double.fps get() = this.timesPerSecond
val Int.fps get() = this.timesPerSecond

fun <T : View> T.addUpdater(referenceFps: Frequency, first: Boolean = true, updatable: T.(scale: Double) -> Unit): Cancellable {
    val time = referenceFps.timeSpan
    return addUpdater(first) {
        updatable(it / time)
    }
}


fun View.Companion.convertViewSpace(src: View, srcPoint: IPoint, dst: View, dstPoint: Point = Point()): IPoint {
    src.localToGlobal(srcPoint, dstPoint)
    return dst.globalToLocal(dstPoint, dstPoint)
}

fun View.convertToSpace(srcPoint: IPoint, dst: View, dstPoint: Point = Point()): IPoint =
    View.Companion.convertViewSpace(this, srcPoint, dst, dstPoint)

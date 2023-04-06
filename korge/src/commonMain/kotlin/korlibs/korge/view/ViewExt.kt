package korlibs.korge.view

import korlibs.io.lang.*
import korlibs.math.geom.*
import korlibs.time.*

val Double.fps get() = this.timesPerSecond
val Int.fps get() = this.timesPerSecond

fun <T : View> T.addUpdater(referenceFps: Frequency, first: Boolean = true, updatable: T.(scale: Float) -> Unit): Cancellable {
    val time = referenceFps.timeSpan
    return addUpdater(first) {
        updatable(it / time)
    }
}


fun View.Companion.convertViewSpace(src: View, srcPoint: Point, dst: View?): Point {
    val global = src.localToGlobal(srcPoint)
    return dst?.globalToLocal(global) ?: global
}

@Deprecated("") fun View.Companion.convertViewSpace(src: View, srcPoint: MPoint, dst: View, dstPoint: MPoint = MPoint()): MPoint =
    dstPoint.copyFrom(convertViewSpace(src, srcPoint.point, dst))

@Deprecated("") fun View.convertToSpace(srcPoint: MPoint, dst: View, dstPoint: MPoint = MPoint()): MPoint =
    View.Companion.convertViewSpace(this, srcPoint, dst, dstPoint)

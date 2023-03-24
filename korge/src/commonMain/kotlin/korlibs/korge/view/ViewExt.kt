package korlibs.korge.view

import korlibs.time.Frequency
import korlibs.time.timesPerSecond
import korlibs.io.lang.Cancellable
import korlibs.math.geom.*

val Double.fps get() = this.timesPerSecond
val Int.fps get() = this.timesPerSecond

fun <T : View> T.addUpdater(referenceFps: Frequency, first: Boolean = true, updatable: T.(scale: Double) -> Unit): Cancellable {
    val time = referenceFps.timeSpan
    return addUpdater(first) {
        updatable(it / time)
    }
}


fun View.Companion.convertViewSpace(src: View, srcPoint: Point, dst: View): Point =
    dst.globalToLocal(src.localToGlobal(srcPoint))

@Deprecated("") fun View.Companion.convertViewSpace(src: View, srcPoint: MPoint, dst: View, dstPoint: MPoint = MPoint()): MPoint =
    dstPoint.copyFrom(convertViewSpace(src, srcPoint.point, dst))

@Deprecated("") fun View.convertToSpace(srcPoint: MPoint, dst: View, dstPoint: MPoint = MPoint()): MPoint =
    View.Companion.convertViewSpace(this, srcPoint, dst, dstPoint)
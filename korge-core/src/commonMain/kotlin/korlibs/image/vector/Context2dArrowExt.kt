package korlibs.image.vector

import korlibs.image.paint.*
import korlibs.math.geom.*
import korlibs.math.geom.vector.*

fun Context2d.arrowCap(p0: Point, p1: Point, width: Double, paint: Paint, cap: ArrowCap) {
    if (cap.filled) {
        fill(paint) { cap.apply { append(p0, p1, width) } }
    } else {
        stroke(paint, width) { cap.apply { append(p0, p1, width) } }
    }
}

fun Context2d.arrow(p0: Point, p1: Point, width: Double, paint: Paint, capEnd: ArrowCap = ArrowCap.Line(null), capStart: ArrowCap = ArrowCap.NoCap) {
    stroke(paint, width) { line(p0, p1) }
    arrowCap(p1, p0, width, paint, capStart)
    arrowCap(p0, p1, width, paint, capEnd)
}

fun Context2d.arrowTo(p1: Point, width: Double, paint: Paint, capEnd: ArrowCap = ArrowCap.Line(null), capStart: ArrowCap = ArrowCap.NoCap) {
    arrow(lastPos, p1, width, paint, capEnd, capStart)
}

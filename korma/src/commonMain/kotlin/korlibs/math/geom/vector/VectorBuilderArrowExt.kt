package korlibs.math.geom.vector

import korlibs.math.geom.*

fun VectorBuilder.arrowTo(p: Point, capEnd: ArrowCap = ArrowCap.Line(null), capStart: ArrowCap = ArrowCap.NoCap) {
    val p0 = this.lastPos
    lineTo(p)
    capStart.apply { append(p, p0, 2f) }
    capEnd.apply { append(p0, p, 2f) }
}

fun VectorBuilder.arrow(p0: Point, p1: Point, capEnd: ArrowCap = ArrowCap.Line(null), capStart: ArrowCap = ArrowCap.NoCap) {
    moveTo(p0)
    arrowTo(p1, capEnd, capStart)
}

interface ArrowCap {
    val filled: Boolean
    fun VectorBuilder.append(p0: Point, p1: Point, width: Float)
    object NoCap : ArrowCap {
        override val filled: Boolean get() = false
        override fun VectorBuilder.append(p0: Point, p1: Point, width: Float) = Unit
    }
    abstract class BaseStrokedCap(val capLen: Float? = null, val cross: Boolean) : ArrowCap {
        override val filled: Boolean get() = false
        override fun VectorBuilder.append(p0: Point, p: Point, width: Float) {
            val capLen = capLen ?: (10f)
            if (capLen <= 0.01f) return
            val angle = p0.angleTo(p)
            val p1 = Vector2.Companion.polar(p, angle - 60.degrees - 90.degrees, capLen)
            val p2 = Vector2.Companion.polar(p, angle + 60.degrees + 90.degrees, capLen)
            if (cross) {
                lineTo(p1); lineTo(p2); lineTo(p)
            } else {
                moveTo(p1); lineTo(p); moveTo(p2); lineTo(p)
            }
        }
    }
    class Line(capLen: Float? = null, override val filled: Boolean = false) : BaseStrokedCap(capLen, cross = false)
    class Cross(capLen: Float? = null, override val filled: Boolean = true) : BaseStrokedCap(capLen, cross = true)
    class Rounded(val radius: Float? = null, override val filled: Boolean = false) : ArrowCap {
        override fun VectorBuilder.append(p0: Point, p1: Point, width: Float) = circle(p1, radius ?: (10f))
    }
}

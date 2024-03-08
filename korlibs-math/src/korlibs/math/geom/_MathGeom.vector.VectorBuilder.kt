@file:Suppress("PackageDirectoryMismatch")

package korlibs.math.geom.vector

import korlibs.math.annotations.*
import korlibs.math.geom.*
import korlibs.math.geom.bezier.*

@KorDslMarker
@ViewDslMarker
@RootViewDslMarker
@VectorDslMarker
interface VectorBuilder {
    val totalPoints: Int
    val lastPos: Point
    val lastMovePos: Point
    fun moveTo(p: Point)
    fun lineTo(p: Point)
    fun quadTo(c: Point, a: Point) {
        Bezier.quadToCubic(lastPos.x, lastPos.y, c.x, c.y, a.x, a.y) { _, _, cx1, cy1, cx2, cy2, x2, y2 ->
            cubicTo(Point(cx1, cy1), Point(cx2, cy2), Point(x2, y2))
        }
        //val x1 = lastX
        //val y1 = lastY
        //val x2 = ax
        //val y2 = ay
        //val tt = (2.0 / 3.0)
        //val cx1 = x1 + (tt * (cx - x1))
        //val cy1 = y1 + (tt * (cy - y1))
        //val cx2 = x2 + (tt * (cx - x2))
        //val cy2 = y2 + (tt * (cy - y2))
        //return cubicTo(cx1, cy1, cx2, cy2, x2, y2)
    }

    fun cubicTo(c1: Point, c2: Point, a: Point)
    fun close()

    fun moveTo(x: Float, y: Float) = moveTo(Point(x, y))
    fun moveTo(x: Double, y: Double) = moveTo(Point(x, y))
    fun moveTo(x: Int, y: Int) = moveTo(Point(x, y))

    fun lineTo(x: Float, y: Float) = lineTo(Point(x, y))
    fun lineTo(x: Double, y: Double) = lineTo(Point(x, y))
    fun lineTo(x: Int, y: Int) = lineTo(Point(x, y))

    fun quadTo(cx: Float, cy: Float, ax: Float, ay: Float) = quadTo(Point(cx, cy), Point(ax, ay))
    fun quadTo(cx: Double, cy: Double, ax: Double, ay: Double) = quadTo(Point(cx, cy), Point(ax, ay))
    fun quadTo(cx: Int, cy: Int, ax: Int, ay: Int) = quadTo(Point(cx, cy), Point(ax, ay))

    fun cubicTo(c1x: Float, c1y: Float, c2x: Float, c2y: Float, ax: Float, ay: Float) = cubicTo(Point(c1x, c1y), Point(c2x, c2y), Point(ax, ay))
    fun cubicTo(c1x: Double, c1y: Double, c2x: Double, c2y: Double, ax: Double, ay: Double) = cubicTo(Point(c1x, c1y), Point(c2x, c2y), Point(ax, ay))
    fun cubicTo(c1x: Int, c1y: Int, c2x: Int, c2y: Int, ax: Int, ay: Int) = cubicTo(Point(c1x, c1y), Point(c2x, c2y), Point(ax, ay))

    fun line(p0: Point, p1: Point) {
        moveTo(p0); lineTo(p1)
    }

    fun quad(o: Point, c: Point, a: Point) {
        moveTo(o); quadTo(c, a)
    }

    fun cubic(o: Point, c1: Point, c2: Point, a: Point) {
        moveTo(o); cubicTo(c1, c2, a)
    }

    fun curve(curve: Bezier) {
        val p = curve.points
        when (curve.order) {
            3 -> cubic(p[0], p[1], p[2], p[3])
            2 -> quad(p[0], p[1], p[2])
            1 -> line(p[0], p[1])
            else -> TODO("Unsupported curve of order ${curve.order}")
        }
    }

    fun arcTo(a: Point, c: Point, r: Float) = Arc.arcToPath(this, a, c, r.toDouble())
    fun arcTo(a: Point, c: Point, r: Double) = Arc.arcToPath(this, a, c, r)
    fun isEmpty(): Boolean = totalPoints == 0
    fun isNotEmpty(): Boolean = totalPoints != 0

    fun rect(rect: RectangleInt) = rect(rect.x, rect.y, rect.width, rect.height)
    fun rect(rect: Rectangle) = rect(rect.x, rect.y, rect.width, rect.height)

    fun rect(pos: Point, size: Size) {
        rect(pos.x, pos.y, size.width, size.height)
    }

    fun rect(x: Double, y: Double, width: Double, height: Double) {
        moveTo(Point(x, y))
        lineTo(Point(x + width, y))
        lineTo(Point(x + width, y + height))
        lineTo(Point(x, y + height))
        close()
    }

    fun rect(x: Float, y: Float, width: Float, height: Float) =
        rect(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())

    fun rect(x: Int, y: Int, width: Int, height: Int) =
        rect(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())

    fun rectHole(x: Double, y: Double, width: Double, height: Double) {
        moveTo(Point(x, y))
        lineTo(Point(x, y + height))
        lineTo(Point(x + width, y + height))
        lineTo(Point(x + width, y))
        close()
    }

    fun roundRect(x: Double, y: Double, w: Double, h: Double, rtl: Double, rtr: Double, rbr: Double, rbl: Double) {
        if (rtl == 0.0 && rtr == 0.0 && rbr == 0.0 && rbl == 0.0) {
            rect(x, y, w, h)
        } else {
            this.moveTo(Point(x + rtl, y))
            this.arcTo(Point(x + w, y), Point(x + w, y + h), rtr)
            this.arcTo(Point(x + w, y + h), Point(x, y + h), rbr)
            this.arcTo(Point(x, y + h), Point(x, y), rbl)
            this.arcTo(Point(x, y), Point(x + w, y), rtl)
            this.close()
        }
    }

    fun roundRect(rect: RoundRectangle) {
        val r = rect.rect
        val c = rect.corners
        roundRect(r.x, r.y, r.width, r.height, c.topLeft, c.topRight, c.bottomLeft, c.bottomRight)
    }

    fun roundRect(x: Double, y: Double, w: Double, h: Double, rx: Double, ry: Double = rx) {
        val r = if (w < 2 * rx) w / 2.0 else if (h < 2 * rx) h / 2.0 else rx
        roundRect(x, y, w, h, r, r, r, r)
    }

    fun roundRect(x: Float, y: Float, w: Float, h: Float, rx: Float, ry: Float = rx) =
        roundRect(x.toDouble(), y.toDouble(), w.toDouble(), h.toDouble(), rx.toDouble(), ry.toDouble())

    fun roundRect(x: Int, y: Int, w: Int, h: Int, rx: Int, ry: Int = rx) =
        roundRect(x.toDouble(), y.toDouble(), w.toDouble(), h.toDouble(), rx.toDouble(), ry.toDouble())

    fun rectHole(x: Float, y: Float, width: Float, height: Float) =
        rectHole(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())

    fun rectHole(x: Int, y: Int, width: Int, height: Int) =
        rectHole(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())

    fun rectHole(rect: Rectangle) = rectHole(rect.x, rect.y, rect.width, rect.height)

    fun curves(curves: List<Curves>) = write(curves.toVectorPath())
    fun curves(curves: Curves) = write(curves.toVectorPath())

    fun write(curves: List<Curves>) = write(curves.toVectorPath())
    fun write(curves: Curves) = write(curves.toVectorPath())
    fun arc(center: Point, r: Number, start: Angle, end: Angle, counterclockwise: Boolean = false) = Arc.arcPath(this, center, r.toDouble(), start, end, counterclockwise)

    fun circle(circle: Circle) = circle(circle.center, circle.radius)
    fun circleHole(circle: Circle) = circleHole(circle.center, circle.radius)

    fun circle(center: Point, radius: Number): Unit = arc(center, radius, Angle.ZERO, Angle.FULL)
    fun circleHole(center: Point, radius: Number) = arc(center, radius, Angle.ZERO, Angle.FULL, counterclockwise = true)

    fun ellipse(bounds: Rectangle) = ellipse(bounds.center, bounds.size / 2)
    fun ellipse(ellipse: Ellipse) = ellipse(ellipse.center, ellipse.radius)
    fun ellipse(center: Point, radius: Size) = Arc.ellipsePath(this, center - radius, radius * 2)

    fun star(
        points: Int,
        radiusSmall: Double,
        radiusBig: Double,
        rotated: Angle = 0.degrees,
        x: Double = 0.0,
        y: Double = 0.0
    ) {
        _regularPolygonStar(points * 2, radiusSmall, radiusBig, rotated, false, x, y)
    }

    fun regularPolygon(
        points: Int,
        radius: Double,
        rotated: Angle = 0.degrees,
        x: Double = 0.0,
        y: Double = 0.0
    ) = _regularPolygonStar(points, radius, radius, rotated, false, x, y)

    fun starHole(
        points: Int,
        radiusSmall: Double,
        radiusBig: Double,
        rotated: Angle = 0.degrees,
        x: Double = 0.0,
        y: Double = 0.0
    ) =
        _regularPolygonStar(points * 2, radiusSmall, radiusBig, rotated, true, x, y)

    fun regularPolygonHole(
        points: Int,
        radius: Double,
        rotated: Angle = 0.degrees,
        x: Double = 0.0,
        y: Double = 0.0
    ) = _regularPolygonStar(points, radius, radius, rotated, true, x, y)



    /**
     * Creates a parallelogram where the inner part is within [bounds].
     *
     * An [angle] of 0.degrees, creates a rectangle. While an [angle] of 90 creates a shape with a line of 45 degrees.
     * Negative angles create a parallelogram those bounds instead of being inner, are outer.
     *
     * [direction] = true
     *    __________
     *   /        /
     *  /________/
     *
     * [direction] = false
     * _________
     * \        \
     *  \________\
     *
     * */
    fun parallelogram(bounds: Rectangle, angle: Angle = 30.degrees, direction: Boolean = true) {
        val dx: Double = angle.sine * bounds.height
        val dx0: Double = if (direction) 0.0 else dx
        val dx1: Double = if (direction) dx else 0.0
        moveTo(Point(bounds.left - dx0, bounds.top))
        lineTo(Point(bounds.right + dx1, bounds.top))
        lineTo(Point(bounds.right + dx0, bounds.bottom))
        lineTo(Point(bounds.left - dx1, bounds.bottom))
    }

    fun polygon(path: PointList, close: Boolean = true) {
        moveTo(path[0])
        for (i in 1 until path.size) lineTo(path[i])
        if (close) close()
    }
    fun polygon(path: List<Point>, close: Boolean = true) = polygon(path.toPointArrayList(), close = close)
    fun polygon(vararg path: Point, close: Boolean = true) = polygon(path.toPointArrayList(), close = close)

    fun polyline(points: PointList, close: Boolean = false): Unit = polygon(points, close = close)
    fun polyline(points: List<Point>, close: Boolean = false): Unit = polyline(points.toPointArrayList(), close = close)
    fun polyline(vararg points: Point, close: Boolean = false): Unit = polyline(points.toPointArrayList(), close = close)

    fun moveToH(x: Double) = moveTo(Point(x, lastPos.y))
    fun moveToV(y: Double) = moveTo(Point(lastPos.x, y))
    fun lineToH(x: Double) = lineTo(Point(x, lastPos.y))
    fun lineToV(y: Double) = lineTo(Point(lastPos.x, y))
    fun rMoveToH(x: Double) = rMoveTo(Point(x, 0.0))
    fun rMoveToV(y: Double) = rMoveTo(Point(0.0, y))
    fun rLineToH(x: Double) = rLineTo(Point(x, 0.0))
    fun rLineToV(y: Double) = rLineTo(Point(0.0, y))
    fun rMoveToHV(value: Double, horizontal: Boolean) = if (horizontal) rMoveToH(value) else rMoveToV(value)
    fun rLineToHV(value: Double, horizontal: Boolean) = if (horizontal) rLineToH(value) else rLineToV(value)
    fun rMoveTo(delta: Point) = moveTo(this.lastPos + delta)
    fun rLineTo(delta: Point) = lineTo(this.lastPos + delta)
    fun rQuadTo(c: Point, a: Point) = quadTo(this.lastPos + c, this.lastPos + a)
    fun rCubicTo(c1: Point, c2: Point, a: Point) = cubicTo(this.lastPos + c1, this.lastPos + c2, this.lastPos + a)

// Variants supporting relative and absolute modes

    fun rCubicTo(c1: Point, c2: Point, a: Point, relative: Boolean) = if (relative) rCubicTo(c1, c2, a) else cubicTo(c1, c2, a)
    fun rQuadTo(c: Point, a: Point, relative: Boolean) = if (relative) rQuadTo(c, a) else quadTo(c, a)
    fun rLineTo(a: Point, relative: Boolean) = if (relative) rLineTo(a) else lineTo(a)
    fun rMoveTo(a: Point, relative: Boolean) = if (relative) rMoveTo(a) else moveTo(a)
    fun rMoveToH(x: Double, relative: Boolean) = if (relative) rMoveToH(x) else moveToH(x)
    fun rMoveToV(y: Double, relative: Boolean) = if (relative) rMoveToV(y) else moveToV(y)
    fun rLineToH(x: Double, relative: Boolean) = if (relative) rLineToH(x) else lineToH(x)
    fun rLineToV(y: Double, relative: Boolean) = if (relative) rLineToV(y) else lineToV(y)

    fun transformed(m: Matrix): VectorBuilder {
        val im = m.inverted()
        val parent = this
        return object : VectorBuilder {
            override val lastPos: Point get() = im.transform(parent.lastPos)
            override val lastMovePos: Point get() = im.transform(parent.lastMovePos)
            override val totalPoints: Int = parent.totalPoints

            private fun t(p: Point): Point = m.transform(p)
            override fun close() = parent.close()
            override fun lineTo(p: Point) = parent.lineTo(t(p))
            override fun moveTo(p: Point) = parent.lineTo(t(p))
            override fun quadTo(c: Point, a: Point) = parent.quadTo(t(c), t(a))
            override fun cubicTo(c1: Point, c2: Point, a: Point) = parent.cubicTo(t(c1), t(c2), t(a))
        }
    }

    fun <T> transformed(m: Matrix, block: VectorBuilder.() -> T): T = block(this.transformed(m))
}

inline fun VectorBuilder.circle(center: Point, radius: Number): Unit = circle(center, radius.toDouble())
inline fun VectorBuilder.circleHole(center: Point, radius: Number) = circleHole(center, radius.toDouble())

private fun VectorBuilder._regularPolygonStar(
    points: Int,
    radiusSmall: Double = 20.0,
    radiusBig: Double = 50.0,
    rotated: Angle = 0.degrees,
    hole: Boolean = false,
    x: Double = 0.0,
    y: Double = 0.0
) {
    for (n in 0 until points) {
        val baseAngle = (Angle.FULL * (n.toDouble() / points))
        val realAngle = if (hole) -baseAngle else baseAngle
        val angle = realAngle - Angle.QUARTER + rotated
        val radius = if (n % 2 == 0) radiusSmall else radiusBig
        val px = angle.cosine * radius
        val py = angle.sine * radius
        if (n == 0) moveTo(Point(x + px, y + py)) else lineTo(Point(x + px, y + py))
    }
    close()
}

fun VectorBuilder.arrowTo(p: Point, capEnd: ArrowCap = ArrowCap.Line(null), capStart: ArrowCap = ArrowCap.NoCap) {
    val p0 = this.lastPos
    lineTo(p)
    capStart.apply { append(p, p0, 2.0) }
    capEnd.apply { append(p0, p, 2.0) }
}

fun VectorBuilder.arrow(p0: Point, p1: Point, capEnd: ArrowCap = ArrowCap.Line(null), capStart: ArrowCap = ArrowCap.NoCap) {
    moveTo(p0)
    arrowTo(p1, capEnd, capStart)
}

interface ArrowCap {
    val filled: Boolean
    fun VectorBuilder.append(p0: Point, p1: Point, width: Double)
    object NoCap : ArrowCap {
        override val filled: Boolean get() = false
        override fun VectorBuilder.append(p0: Point, p1: Point, width: Double) = Unit
    }
    abstract class BaseStrokedCap(val capLen: Double? = null, val cross: Boolean) : ArrowCap {
        override val filled: Boolean get() = false
        override fun VectorBuilder.append(p0: Point, p: Point, width: Double) {
            val capLen = capLen ?: (10.0)
            if (capLen <= 0.01) return
            val angle = p0.angleTo(p)
            val p1 = Vector2D.polar(p, angle - 60.degrees - 90.degrees, capLen)
            val p2 = Vector2D.polar(p, angle + 60.degrees + 90.degrees, capLen)
            if (cross) {
                lineTo(p1); lineTo(p2); lineTo(p)
            } else {
                moveTo(p1); lineTo(p); moveTo(p2); lineTo(p)
            }
        }
    }
    class Line(capLen: Double? = null, override val filled: Boolean = false) : BaseStrokedCap(capLen, cross = false)
    class Cross(capLen: Double? = null, override val filled: Boolean = true) : BaseStrokedCap(capLen, cross = true)
    class Rounded(val radius: Double? = null, override val filled: Boolean = false) : ArrowCap {
        override fun VectorBuilder.append(p0: Point, p1: Point, width: Double) = circle(p1, radius ?: (10.0))
    }
}

/** Creates a polyline from [points] adding arrow caps ([capEnd] and [capStart]) in each segment. Useful for displaying directed graphs */
fun VectorBuilder.polyArrows(points: PointList, capEnd: ArrowCap = ArrowCap.Line(), capStart: ArrowCap = ArrowCap.NoCap) {
    if (points.isEmpty()) return
    moveTo(points[0])
    for (n in 1 until points.size) arrowTo(points[n], capEnd, capStart)
}

/** Creates a polyline from [points] adding arrow caps ([capEnd] and [capStart]) in each segment. Useful for displaying directed graphs */
@OptIn(KormaExperimental::class)
fun VectorBuilder.polyArrows(vararg points: Point, capEnd: ArrowCap = ArrowCap.Line(), capStart: ArrowCap = ArrowCap.NoCap) =
    polyArrows(pointArrayListOf(*points), capEnd, capStart)
/** Creates a polyline from [points] adding arrow caps ([capEnd] and [capStart]) in each segment. Useful for displaying directed graphs */
fun VectorBuilder.polyArrows(points: List<Point>, capEnd: ArrowCap = ArrowCap.Line(), capStart: ArrowCap = ArrowCap.NoCap) =
    polyArrows(points.toPointArrayList(), capEnd, capStart)

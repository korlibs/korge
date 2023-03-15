package com.soywiz.korma.geom.vector

import com.soywiz.korma.annotations.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.bezier.*
import kotlin.jvm.*

@KorDslMarker
@ViewDslMarker
@RootViewDslMarker
@VectorDslMarker
interface VectorBuilder {
    val totalPoints: Int
    val lastPos: Point
    fun moveTo(p: Point)
    fun lineTo(p: Point)
    fun quadTo(c: Point, a: Point) {
        Bezier.quadToCubic(lastPos.xD, lastPos.yD, c.xD, c.yD, a.xD, a.yD) { _, _, cx1, cy1, cx2, cy2, x2, y2 ->
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

    fun arcTo(a: Point, c: Point, r: Double) = Arc.arcToPath(this, a, c, r)
    fun isEmpty(): Boolean = totalPoints == 0
    fun isNotEmpty(): Boolean = totalPoints != 0


    fun rect(rect: MRectangleInt) = rect(rect.x, rect.y, rect.width, rect.height)
    fun rect(rect: MRectangle) = rect(rect.x, rect.y, rect.width, rect.height)
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

    fun roundRect(x: Double, y: Double, w: Double, h: Double, rx: Double, ry: Double = rx) {
        if (rx == 0.0 && ry == 0.0) {
            rect(x, y, w, h)
        } else {
            val r = if (w < 2 * rx) w / 2f else if (h < 2 * rx) h / 2f else rx
            this.moveTo(Point(x + r, y))
            this.arcTo(Point(x + w, y), Point(x + w, y + h), r)
            this.arcTo(Point(x + w, y + h), Point(x, y + h), r)
            this.arcTo(Point(x, y + h), Point(x, y), r)
            this.arcTo(Point(x, y), Point(x + w, y), r)
            this.close()
        }
    }

    fun roundRect(x: Float, y: Float, w: Float, h: Float, rx: Float, ry: Float = rx) =
        roundRect(x.toDouble(), y.toDouble(), w.toDouble(), h.toDouble(), rx.toDouble(), ry.toDouble())

    fun roundRect(x: Int, y: Int, w: Int, h: Int, rx: Int, ry: Int = rx) =
        roundRect(x.toDouble(), y.toDouble(), w.toDouble(), h.toDouble(), rx.toDouble(), ry.toDouble())

    fun rectHole(x: Float, y: Float, width: Float, height: Float) =
        rectHole(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())

    fun rectHole(x: Int, y: Int, width: Int, height: Int) =
        rectHole(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())

    fun rectHole(rect: MRectangle) = rectHole(rect.x, rect.y, rect.width, rect.height)

    fun curves(curves: List<Curves>) = write(curves.toVectorPath())
    fun curves(curves: Curves) = write(curves.toVectorPath())

    fun write(curves: List<Curves>) = write(curves.toVectorPath())
    fun write(curves: Curves) = write(curves.toVectorPath())
    fun arc(center: Point, r: Float, start: Angle, end: Angle, counterclockwise: Boolean = false) = Arc.arcPath(this, center, r, start, end, counterclockwise)
    fun circle(center: Point, radius: Float) = arc(center, radius, Angle.ZERO, Angle.FULL)
    fun circleHole(center: Point, radius: Float) = arc(center, radius, Angle.ZERO, Angle.FULL, counterclockwise = true)
    fun ellipse(center: Point, radius: Size) = Arc.ellipsePath(this, center, radius)
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
    fun parallelogram(bounds: MRectangle, angle: Angle = 30.degrees, direction: Boolean = true) {
        val dx = angle.sineD * bounds.height
        val dx0 = if (direction) 0.0 else dx
        val dx1 = if (direction) dx else 0.0
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

    fun moveToH(x: Float) = moveTo(Point(x, lastPos.yF))
    fun moveToV(y: Float) = moveTo(Point(lastPos.xF, y))
    fun lineToH(x: Float) = lineTo(Point(x, lastPos.yF))
    fun lineToV(y: Float) = lineTo(Point(lastPos.xF, y))
    fun rMoveToH(x: Float) = rMoveTo(Point(x, 0f))
    fun rMoveToV(y: Float) = rMoveTo(Point(0f, y))
    fun rLineToH(x: Float) = rLineTo(Point(x, 0f))
    fun rLineToV(y: Float) = rLineTo(Point(0f, y))
    fun rMoveToHV(value: Float, horizontal: Boolean) = if (horizontal) rMoveToH(value) else rMoveToV(value)
    fun rLineToHV(value: Float, horizontal: Boolean) = if (horizontal) rLineToH(value) else rLineToV(value)
    fun rMoveTo(delta: Point) = moveTo(this.lastPos + delta)
    fun rLineTo(delta: Point) = lineTo(this.lastPos + delta)
    fun rQuadTo(c: Point, a: Point) = quadTo(this.lastPos + c, this.lastPos + a)
    fun rCubicTo(c1: Point, c2: Point, a: Point) = cubicTo(this.lastPos + c1, this.lastPos + c2, this.lastPos + a)

// Variants supporting relative and absolute modes

    fun rCubicTo(c1: Point, c2: Point, a: Point, relative: Boolean) = if (relative) rCubicTo(c1, c2, a) else cubicTo(c1, c2, a)
    fun rQuadTo(c: Point, a: Point, relative: Boolean) = if (relative) rQuadTo(c, a) else quadTo(c, a)
    fun rLineTo(a: Point, relative: Boolean) = if (relative) rLineTo(a) else lineTo(a)
    fun rMoveTo(a: Point, relative: Boolean) = if (relative) rMoveTo(a) else moveTo(a)
    fun rMoveToH(x: Float, relative: Boolean) = if (relative) rMoveToH(x) else moveToH(x)
    fun rMoveToV(y: Float, relative: Boolean) = if (relative) rMoveToV(y) else moveToV(y)
    fun rLineToH(x: Float, relative: Boolean) = if (relative) rLineToH(x) else lineToH(x)
    fun rLineToV(y: Float, relative: Boolean) = if (relative) rLineToV(y) else lineToV(y)

    fun transformed(m: Matrix): VectorBuilder {
        val im = m.inverted()
        val parent = this
        return object : VectorBuilder {
            override val lastPos: Point get() = im.transform(parent.lastPos)
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
        val px = angle.cosineD * radius
        val py = angle.sineD * radius
        if (n == 0) moveTo(Point(x + px, y + py)) else lineTo(Point(x + px, y + py))
    }
    close()
}

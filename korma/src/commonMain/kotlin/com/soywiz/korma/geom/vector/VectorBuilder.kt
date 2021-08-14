package com.soywiz.korma.geom.vector

import com.soywiz.korma.annotations.KorDslMarker
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.bezier.*
import kotlin.math.*

@KorDslMarker
interface VectorBuilder {
    val totalPoints: Int
    val lastX: Double
    val lastY: Double
    fun moveTo(x: Double, y: Double)
    fun lineTo(x: Double, y: Double)
    fun quadTo(cx: Double, cy: Double, ax: Double, ay: Double) {
        val x1 = lastX
        val y1 = lastY
        val x2 = ax
        val y2 = ay
        val tt = (2.0 / 3.0)
        val cx1 = x1 + (tt * (cx - x1))
        val cy1 = y1 + (tt * (cy - y1))
        val cx2 = x2 + (tt * (cx - x2))
        val cy2 = y2 + (tt * (cy - y2))
        return cubicTo(cx1, cy1, cx2, cy2, x2, y2)
    }
    fun cubicTo(cx1: Double, cy1: Double, cx2: Double, cy2: Double, ax: Double, ay: Double)
    fun close()
}

fun VectorBuilder.isEmpty() = totalPoints == 0
fun VectorBuilder.isNotEmpty() = totalPoints != 0

//fun arcTo(b: Point2d, a: Point2d, c: Point2d, r: Double) {
fun VectorBuilder.arcTo(ax: Double, ay: Double, cx: Double, cy: Double, r: Double) {
    if (isEmpty()) moveTo(ax, ay)
    val bx = lastX
    val by = lastY
    val b = IPoint(bx, by)
    val a = IPoint(ax, ay)
    val c = IPoint(cx, cy)
    val AB = b - a
    val AC = c - a
    val angle = Point.angleArc(AB, AC).radians * 0.5
    val x = r * sin((PI / 2.0) - angle) / sin(angle)
    val A = a + AB.unit * x
    val B = a + AC.unit * x
    lineTo(A.x, A.y)
    quadTo(a.x, a.y, B.x, B.y)
}
fun VectorBuilder.arcTo(ax: Float, ay: Float, cx: Float, cy: Float, r: Float) = arcTo(ax.toDouble(), ay.toDouble(), cx.toDouble(), cy.toDouble(), r.toDouble())
fun VectorBuilder.arcTo(ax: Int, ay: Int, cx: Int, cy: Int, r: Int) = arcTo(ax.toDouble(), ay.toDouble(), cx.toDouble(), cy.toDouble(), r.toDouble())

fun VectorBuilder.rect(rect: IRectangleInt) = rect(rect.x, rect.y, rect.width, rect.height)
fun VectorBuilder.rect(rect: IRectangle) = rect(rect.x, rect.y, rect.width, rect.height)
fun VectorBuilder.rect(x: Double, y: Double, width: Double, height: Double) {
    moveTo(x, y)
    lineTo(x + width, y)
    lineTo(x + width, y + height)
    lineTo(x, y + height)
    close()
}
fun VectorBuilder.rect(x: Float, y: Float, width: Float, height: Float) = rect(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())
fun VectorBuilder.rect(x: Int, y: Int, width: Int, height: Int) = rect(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())

fun VectorBuilder.rectHole(x: Double, y: Double, width: Double, height: Double) {
    moveTo(x, y)
    lineTo(x, y + height)
    lineTo(x + width, y + height)
    lineTo(x + width, y)
    close()
}

fun VectorBuilder.roundRect(x: Double, y: Double, w: Double, h: Double, rx: Double, ry: Double = rx) {
    if (rx == 0.0 && ry == 0.0) {
        rect(x, y, w, h)
    } else {
        val r = if (w < 2 * rx) w / 2f else if (h < 2 * rx) h / 2f else rx
        this.moveTo(x + r, y)
        this.arcTo(x + w, y, x + w, y + h, r)
        this.arcTo(x + w, y + h, x, y + h, r)
        this.arcTo(x, y + h, x, y, r)
        this.arcTo(x, y, x + w, y, r)
        this.close()
    }
}
fun VectorBuilder.roundRect(x: Float, y: Float, w: Float, h: Float, rx: Float, ry: Float = rx) = roundRect(x.toDouble(), y.toDouble(), w.toDouble(), h.toDouble(), rx.toDouble(), ry.toDouble())
fun VectorBuilder.roundRect(x: Int, y: Int, w: Int, h: Int, rx: Int, ry: Int = rx) = roundRect(x.toDouble(), y.toDouble(), w.toDouble(), h.toDouble(), rx.toDouble(), ry.toDouble())

fun VectorBuilder.rectHole(x: Float, y: Float, width: Float, height: Float) = rectHole(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())
fun VectorBuilder.rectHole(x: Int, y: Int, width: Int, height: Int) = rectHole(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())

fun VectorBuilder.arc(x: Double, y: Double, r: Double, start: Angle, end: Angle) {
    // http://hansmuller-flex.blogspot.com.es/2011/04/approximating-circular-arc-with-cubic.html
    val EPSILON = 0.00001
    val PI_TWO = PI * 2.0
    val PI_OVER_TWO = PI / 2.0

    val startAngle = start.radians % PI_TWO
    val endAngle = end.radians % PI_TWO
    var remainingAngle = min(PI_TWO, abs(endAngle - startAngle))
    if (remainingAngle.absoluteValue < EPSILON && start != end) remainingAngle = PI_TWO
    val sgn = if (startAngle < endAngle) +1 else -1
    var a1 = startAngle
    val p1 = Point()
    val p2 = Point()
    val p3 = Point()
    val p4 = Point()
    var index = 0
    while (remainingAngle > EPSILON) {
        val a2 = a1 + sgn * min(remainingAngle, PI_OVER_TWO)

        val k = 0.5522847498
        val a = (a2 - a1) / 2.0
        val x4 = r * cos(a)
        val y4 = r * sin(a)
        val x1 = x4
        val y1 = -y4
        val f = k * tan(a)
        val x2 = x1 + f * y4
        val y2 = y1 + f * x4
        val x3 = x2
        val y3 = -y2
        val ar = a + a1
        val cos_ar = cos(ar)
        val sin_ar = sin(ar)
        p1.setTo(x + r * cos(a1), y + r * sin(a1))
        p2.setTo(x + x2 * cos_ar - y2 * sin_ar, y + x2 * sin_ar + y2 * cos_ar)
        p3.setTo(x + x3 * cos_ar - y3 * sin_ar, y + x3 * sin_ar + y3 * cos_ar)
        p4.setTo(x + r * cos(a2), y + r * sin(a2))

        if (index == 0) moveTo(p1.x, p1.y)
        cubicTo(p2.x, p2.y, p3.x, p3.y, p4.x, p4.y)

        index++
        remainingAngle -= abs(a2 - a1)
        a1 = a2
    }
    if (startAngle == endAngle && index != 0) {
        close()
    }
}
fun VectorBuilder.arc(x: Float, y: Float, r: Float, start: Angle, end: Angle) = arc(x.toDouble(), y.toDouble(), r.toDouble(), start, end)
fun VectorBuilder.arc(x: Int, y: Int, r: Int, start: Angle, end: Angle) = arc(x.toDouble(), y.toDouble(), r.toDouble(), start, end)

fun VectorBuilder.circle(point: Point, radius: Double) = arc(point.x, point.y, radius, 0.degrees, 360.degrees)
fun VectorBuilder.circle(x: Double, y: Double, radius: Double) = arc(x, y, radius, 0.degrees, 360.degrees)
fun VectorBuilder.circle(x: Float, y: Float, radius: Float) = circle(x.toDouble(), y.toDouble(), radius.toDouble())
fun VectorBuilder.circle(x: Int, y: Int, radius: Int) = circle(x.toDouble(), y.toDouble(), radius.toDouble())

fun VectorBuilder.ellipse(x: Double, y: Double, rw: Double, rh: Double) {
    val k = .5522848
    val ox = (rw / 2) * k
    val oy = (rh / 2) * k
    val xe = x + rw
    val ye = y + rh
    val xm = x + rw / 2
    val ym = y + rh / 2
    moveTo(x, ym)
    cubicTo(x, ym - oy, xm - ox, y, xm, y)
    cubicTo(xm + ox, y, xe, ym - oy, xe, ym)
    cubicTo(xe, ym + oy, xm + ox, ye, xm, ye)
    cubicTo(xm - ox, ye, x, ym + oy, x, ym)
    close()
}
fun VectorBuilder.ellipse(x: Float, y: Float, rw: Float, rh: Float) = ellipse(x.toDouble(), y.toDouble(), rw.toDouble(), rh.toDouble())
fun VectorBuilder.ellipse(x: Int, y: Int, rw: Int, rh: Int) = ellipse(x.toDouble(), y.toDouble(), rw.toDouble(), rh.toDouble())


fun VectorBuilder.star(points: Int, radiusSmall: Double, radiusBig: Double, rotated: Angle = 0.degrees, x: Double = 0.0, y: Double = 0.0) {
    _regularPolygonStar(points * 2, radiusSmall, radiusBig, rotated, false, x, y)
}

fun VectorBuilder.regularPolygon(points: Int, radius: Double, rotated: Angle = 0.degrees, x: Double = 0.0, y: Double = 0.0) {
    _regularPolygonStar(points, radius, radius, rotated, false, x, y)
}

fun VectorBuilder.starHole(points: Int, radiusSmall: Double, radiusBig: Double, rotated: Angle = 0.degrees, x: Double = 0.0, y: Double = 0.0) {
    _regularPolygonStar(points * 2, radiusSmall, radiusBig, rotated, true, x, y)
}

fun VectorBuilder.regularPolygonHole(points: Int, radius: Double, rotated: Angle = 0.degrees, x: Double = 0.0, y: Double = 0.0) {
    _regularPolygonStar(points, radius, radius, rotated, true, x, y)
}

internal fun VectorBuilder._regularPolygonStar(points: Int, radiusSmall: Double = 20.0, radiusBig: Double = 50.0, rotated: Angle = 0.degrees, hole: Boolean = false, x: Double = 0.0, y: Double = 0.0) {
    for (n in 0 until points) {
        val baseAngle = (360.degrees * (n.toDouble() / points))
        val realAngle = if (hole) -baseAngle else baseAngle
        val angle = realAngle - 90.degrees + rotated
        val radius = if (n % 2 == 0) radiusSmall else radiusBig
        val px = angle.cosine * radius
        val py = angle.sine * radius
        if (n == 0) moveTo(x + px, y + py) else lineTo(x + px, y + py)
    }
    close()
}

fun VectorBuilder.moveTo(p: IPoint) = moveTo(p.x, p.y)
fun VectorBuilder.lineTo(p: IPoint) = lineTo(p.x, p.y)
fun VectorBuilder.quadTo(c: IPoint, a: IPoint) = quadTo(c.x, c.y, a.x, a.y)
fun VectorBuilder.cubicTo(c1: IPoint, c2: IPoint, a: IPoint) = cubicTo(c1.x, c1.y, c2.x, c2.y, a.x, a.y)

fun VectorBuilder.polygon(path: IPointArrayList, close: Boolean = true) {
    moveTo(path.getX(0), path.getY(0))
    for (i in 1 until path.size) {
        lineTo(path.getX(i), path.getY(i))
    }
    if (close) close()
}
fun VectorBuilder.polygon(path: Array<IPoint>, close: Boolean = true) = polygon(PointArrayList(*path), close)
fun VectorBuilder.polygon(path: List<IPoint>, close: Boolean = true) = polygon(PointArrayList(path), close)

fun VectorBuilder.moveToH(x: Double) = moveTo(x, lastY)
fun VectorBuilder.moveToH(x: Float) = moveToH(x.toDouble())
fun VectorBuilder.moveToH(x: Int) = moveToH(x.toDouble())

fun VectorBuilder.rMoveToH(x: Double) = rMoveTo(x, 0.0)
fun VectorBuilder.rMoveToH(x: Float) = rMoveToH(x.toDouble())
fun VectorBuilder.rMoveToH(x: Int) = rMoveToH(x.toDouble())

fun VectorBuilder.moveToV(y: Double) = moveTo(lastX, y)
fun VectorBuilder.moveToV(y: Float) = moveToV(y.toDouble())
fun VectorBuilder.moveToV(y: Int) = moveToV(y.toDouble())

fun VectorBuilder.rMoveToV(y: Double) = rMoveTo(0.0, y)
fun VectorBuilder.rMoveToV(y: Float) = rMoveToV(y.toDouble())
fun VectorBuilder.rMoveToV(y: Int) = rMoveToV(y.toDouble())

fun VectorBuilder.lineToH(x: Double) = lineTo(x, lastY)
fun VectorBuilder.lineToH(x: Float) = lineToH(x.toDouble())
fun VectorBuilder.lineToH(x: Int) = lineToH(x.toDouble())

fun VectorBuilder.rLineToH(x: Double) = rLineTo(x, 0.0)
fun VectorBuilder.rLineToH(x: Float) = rLineToH(x.toDouble())
fun VectorBuilder.rLineToH(x: Int) = rLineToH(x.toDouble())

fun VectorBuilder.lineToV(y: Double) = lineTo(lastX, y)
fun VectorBuilder.lineToV(y: Float) = lineToV(y.toDouble())
fun VectorBuilder.lineToV(y: Int) = lineToV(y.toDouble())

fun VectorBuilder.rLineToV(y: Double) = rLineTo(0.0, y)
fun VectorBuilder.rLineToV(y: Float) = rLineToV(y.toDouble())
fun VectorBuilder.rLineToV(y: Int) = rLineToV(y.toDouble())

fun VectorBuilder.rMoveTo(x: Double, y: Double) = moveTo(this.lastX + x, this.lastY + y)
fun VectorBuilder.rMoveTo(x: Float, y: Float) = rMoveTo(x.toDouble(), y.toDouble())
fun VectorBuilder.rMoveTo(x: Int, y: Int) = rMoveTo(x.toDouble(), y.toDouble())

fun VectorBuilder.rLineTo(x: Double, y: Double) = lineTo(this.lastX + x, this.lastY + y)
fun VectorBuilder.rLineTo(x: Float, y: Float) = rLineTo(x.toDouble(), y.toDouble())
fun VectorBuilder.rLineTo(x: Int, y: Int) = rLineTo(x.toDouble(), y.toDouble())

fun VectorBuilder.rQuadTo(cx: Double, cy: Double, ax: Double, ay: Double) = quadTo(this.lastX + cx, this.lastY + cy, this.lastX + ax, this.lastY + ay)
fun VectorBuilder.rQuadTo(cx: Float, cy: Float, ax: Float, ay: Float) = rQuadTo(cx.toDouble(), cy.toDouble(), ax.toDouble(), ay.toDouble())
fun VectorBuilder.rQuadTo(cx: Int, cy: Int, ax: Int, ay: Int) = rQuadTo(cx.toDouble(), cy.toDouble(), ax.toDouble(), ay.toDouble())

fun VectorBuilder.rCubicTo(cx1: Double, cy1: Double, cx2: Double, cy2: Double, ax: Double, ay: Double) = cubicTo(this.lastX + cx1, this.lastY + cy1, this.lastX + cx2, this.lastY + cy2, this.lastX + ax, this.lastY + ay)
fun VectorBuilder.rCubicTo(cx1: Float, cy1: Float, cx2: Float, cy2: Float, ax: Float, ay: Float) = rCubicTo(cx1.toDouble(), cy1.toDouble(), cx2.toDouble(), cy2.toDouble(), ax.toDouble(), ay.toDouble())
fun VectorBuilder.rCubicTo(cx1: Int, cy1: Int, cx2: Int, cy2: Int, ax: Int, ay: Int) = rCubicTo(cx1.toDouble(), cy1.toDouble(), cx2.toDouble(), cy2.toDouble(), ax.toDouble(), ay.toDouble())

fun VectorBuilder.moveTo(x: Float, y: Float) = moveTo(x.toDouble(), y.toDouble())
fun VectorBuilder.moveTo(x: Int, y: Int) = moveTo(x.toDouble(), y.toDouble())

fun VectorBuilder.lineTo(x: Float, y: Float) = lineTo(x.toDouble(), y.toDouble())
fun VectorBuilder.lineTo(x: Int, y: Int) = lineTo(x.toDouble(), y.toDouble())

fun VectorBuilder.quadTo(controlX: Float, controlY: Float, anchorX: Float, anchorY: Float) = quadTo(controlX.toDouble(), controlY.toDouble(), anchorX.toDouble(), anchorY.toDouble())
fun VectorBuilder.quadTo(controlX: Int, controlY: Int, anchorX: Int, anchorY: Int) = quadTo(controlX.toDouble(), controlY.toDouble(), anchorX.toDouble(), anchorY.toDouble())

fun VectorBuilder.cubicTo(cx1: Float, cy1: Float, cx2: Float, cy2: Float, ax: Float, ay: Float) = cubicTo(cx1.toDouble(), cy1.toDouble(), cx2.toDouble(), cy2.toDouble(), ax.toDouble(), ay.toDouble())
fun VectorBuilder.cubicTo(cx1: Int, cy1: Int, cx2: Int, cy2: Int, ax: Int, ay: Int) = cubicTo(cx1.toDouble(), cy1.toDouble(), cx2.toDouble(), cy2.toDouble(), ax.toDouble(), ay.toDouble())

fun VectorBuilder.line(p0: Point, p1: Point) = line(p0.x, p0.y, p1.x, p1.y)
fun VectorBuilder.line(x0: Double, y0: Double, x1: Double, y1: Double) = moveTo(x0, y0).also { lineTo(x1, y1) }
fun VectorBuilder.line(x0: Float, y0: Float, x1: Float, y1: Float) = line(x0.toDouble(), y0.toDouble(), x1.toDouble(), y1.toDouble())
fun VectorBuilder.line(x0: Int, y0: Int, x1: Int, y1: Int) = line(x0.toDouble(), y0.toDouble(), x1.toDouble(), y1.toDouble())

fun VectorBuilder.quad(x0: Double, y0: Double, controlX: Double, controlY: Double, anchorX: Double, anchorY: Double) = moveTo(x0, y0).also { quadTo(controlX, controlY, anchorX, anchorY) }
fun VectorBuilder.quad(x0: Float, y0: Float, controlX: Float, controlY: Float, anchorX: Float, anchorY: Float) = quad(x0.toDouble(), y0.toDouble(), controlX.toDouble(), controlY.toDouble(), anchorX.toDouble(), anchorY.toDouble())
fun VectorBuilder.quad(x0: Int, y0: Int, controlX: Int, controlY: Int, anchorX: Int, anchorY: Int) = quad(x0.toDouble(), y0.toDouble(), controlX.toDouble(), controlY.toDouble(), anchorX.toDouble(), anchorY.toDouble())

fun VectorBuilder.cubic(x0: Double, y0: Double, cx1: Double, cy1: Double, cx2: Double, cy2: Double, ax: Double, ay: Double) = moveTo(x0, y0).also { cubicTo(cx1, cy1, cx2, cy2, ax, ay) }
fun VectorBuilder.cubic(x0: Float, y0: Float, cx1: Float, cy1: Float, cx2: Float, cy2: Float, ax: Float, ay: Float) = cubic(x0.toDouble(), y0.toDouble(), cx1.toDouble(), cy1.toDouble(), cx2.toDouble(), cy2.toDouble(), ax.toDouble(), ay.toDouble())
fun VectorBuilder.cubic(x0: Int, y0: Int, cx1: Int, cy1: Int, cx2: Int, cy2: Int, ax: Int, ay: Int) = cubic(x0.toDouble(), y0.toDouble(), cx1.toDouble(), cy1.toDouble(), cx2.toDouble(), cy2.toDouble(), ax.toDouble(), ay.toDouble())

fun VectorBuilder.quad(o: Point, c: Point, a: Point) = quad(o.x, o.y, c.x, c.y, a.x, a.y)
fun VectorBuilder.cubic(o: Point, c1: Point, c2: Point, a: Point) = cubic(o.x, o.y, c1.x, c1.y, c2.x, c2.y, a.x, a.y)

fun VectorBuilder.quad(curve: Bezier.Quad) = quad(curve.p0, curve.p1, curve.p2)
fun VectorBuilder.cubic(curve: Bezier.Cubic) = cubic(curve.p0, curve.p1, curve.p2, curve.p3)

// Variants supporting relative and absolute modes

fun VectorBuilder.rCubicTo(cx1: Double, cy1: Double, cx2: Double, cy2: Double, ax: Double, ay: Double, relative: Boolean) = if (relative) rCubicTo(cx1, cy1, cx2, cy2, ax, ay) else cubicTo(cx1, cy1, cx2, cy2, ax, ay)
fun VectorBuilder.rCubicTo(cx1: Float, cy1: Float, cx2: Float, cy2: Float, ax: Float, ay: Float, relative: Boolean) = if (relative) rCubicTo(cx1, cy1, cx2, cy2, ax, ay) else cubicTo(cx1, cy1, cx2, cy2, ax, ay)
fun VectorBuilder.rCubicTo(cx1: Int, cy1: Int, cx2: Int, cy2: Int, ax: Int, ay: Int, relative: Boolean) = if (relative) rCubicTo(cx1, cy1, cx2, cy2, ax, ay) else cubicTo(cx1, cy1, cx2, cy2, ax, ay)

fun VectorBuilder.rQuadTo(cx: Double, cy: Double, ax: Double, ay: Double, relative: Boolean) = if (relative) rQuadTo(cx, cy, ax, ay) else quadTo(cx, cy, ax, ay)
fun VectorBuilder.rQuadTo(cx: Float, cy: Float, ax: Float, ay: Float, relative: Boolean) = if (relative) rQuadTo(cx, cy, ax, ay) else quadTo(cx, cy, ax, ay)
fun VectorBuilder.rQuadTo(cx: Int, cy: Int, ax: Int, ay: Int, relative: Boolean) = if (relative) rQuadTo(cx, cy, ax, ay) else quadTo(cx, cy, ax, ay)

fun VectorBuilder.rLineTo(ax: Double, ay: Double, relative: Boolean) = if (relative) rLineTo(ax, ay) else lineTo(ax, ay)
fun VectorBuilder.rLineTo(ax: Float, ay: Float, relative: Boolean) = if (relative) rLineTo(ax, ay) else lineTo(ax, ay)
fun VectorBuilder.rLineTo(ax: Int, ay: Int, relative: Boolean) = if (relative) rLineTo(ax, ay) else lineTo(ax, ay)

fun VectorBuilder.rMoveTo(ax: Double, ay: Double, relative: Boolean) = if (relative) rMoveTo(ax, ay) else moveTo(ax, ay)
fun VectorBuilder.rMoveTo(ax: Float, ay: Float, relative: Boolean) = if (relative) rMoveTo(ax, ay) else moveTo(ax, ay)
fun VectorBuilder.rMoveTo(ax: Int, ay: Int, relative: Boolean) = if (relative) rMoveTo(ax, ay) else moveTo(ax, ay)

fun VectorBuilder.rMoveToH(ax: Double, relative: Boolean) = if (relative) rMoveToH(ax) else moveToH(ax)
fun VectorBuilder.rMoveToH(ax: Float, relative: Boolean) = if (relative) rMoveToH(ax) else moveToH(ax)
fun VectorBuilder.rMoveToH(ax: Int, relative: Boolean) = if (relative) rMoveToH(ax) else moveToH(ax)

fun VectorBuilder.rMoveToV(ay: Double, relative: Boolean) = if (relative) rMoveToV(ay) else moveToV(ay)
fun VectorBuilder.rMoveToV(ay: Float, relative: Boolean) = if (relative) rMoveToV(ay) else moveToV(ay)
fun VectorBuilder.rMoveToV(ay: Int, relative: Boolean) = if (relative) rMoveToV(ay) else moveToV(ay)

fun VectorBuilder.rLineToH(ax: Double, relative: Boolean) = if (relative) rLineToH(ax) else lineToH(ax)
fun VectorBuilder.rLineToH(ax: Float, relative: Boolean) = if (relative) rLineToH(ax) else lineToH(ax)
fun VectorBuilder.rLineToH(ax: Int, relative: Boolean) = if (relative) rLineToH(ax) else lineToH(ax)

fun VectorBuilder.rLineToV(ay: Double, relative: Boolean) = if (relative) rLineToV(ay) else lineToV(ay)
fun VectorBuilder.rLineToV(ay: Float, relative: Boolean) = if (relative) rLineToV(ay) else lineToV(ay)
fun VectorBuilder.rLineToV(ay: Int, relative: Boolean) = if (relative) rLineToV(ay) else lineToV(ay)

fun VectorBuilder.transformed(m: Matrix): VectorBuilder {
    val im = m.inverted()
    val parent = this
    return object : VectorBuilder {
        override val lastX: Double get() = im.transformX(parent.lastX, parent.lastY)
        override val lastY: Double get() = im.transformY(parent.lastX, parent.lastY)
        override val totalPoints: Int = parent.totalPoints

        fun tX(x: Double, y: Double) = m.transformX(x, y)
        fun tY(x: Double, y: Double) = m.transformY(x, y)

        override fun close() = parent.close()
        override fun lineTo(x: Double, y: Double) = parent.lineTo(tX(x, y), tY(x, y))
        override fun moveTo(x: Double, y: Double) = parent.lineTo(tX(x, y), tY(x, y))
        override fun quadTo(cx: Double, cy: Double, ax: Double, ay: Double) = parent.quadTo(
            tX(cx, cy), tY(cx, cy),
            tX(ax, ay), tY(ax, ay)
        )
        override fun cubicTo(cx1: Double, cy1: Double, cx2: Double, cy2: Double, ax: Double, ay: Double) = parent.cubicTo(
            tX(cx1, cy1), tY(cx1, cy1),
            tX(cx2, cy2), tY(cx2, cy2),
            tX(ax, ay), tY(ax, ay)
        )
    }
}

fun <T> VectorBuilder.transformed(m: Matrix, block: VectorBuilder.() -> T): T = block(this.transformed(m))

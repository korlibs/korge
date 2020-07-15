package com.soywiz.korma.geom.vector

import com.soywiz.korma.annotations.KorDslMarker
import com.soywiz.korma.geom.*
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
    val angle = Point.angle(AB, AC).radians * 0.5
    val x = r * sin((PI / 2.0) - angle) / sin(angle)
    val A = a + AB.unit * x
    val B = a + AC.unit * x
    lineTo(A.x, A.y)
    quadTo(a.x, a.y, B.x, B.y)
}

fun VectorBuilder.rect(x: Double, y: Double, width: Double, height: Double) {
    moveTo(x, y)
    lineTo(x + width, y)
    lineTo(x + width, y + height)
    lineTo(x, y + height)
    close()
}

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
    }
}

fun VectorBuilder.arc(x: Double, y: Double, r: Double, start: Double, end: Double) {
    // http://hansmuller-flex.blogspot.com.es/2011/04/approximating-circular-arc-with-cubic.html
    val EPSILON = 0.00001
    val PI_TWO = PI * 2.0
    val PI_OVER_TWO = PI / 2.0

    val startAngle = start % PI_TWO
    val endAngle = end % PI_TWO
    var remainingAngle = kotlin.math.min(PI_TWO, abs(endAngle - startAngle))
    if (remainingAngle == 0.0 && start != end) remainingAngle = PI_TWO
    val sgn = if (startAngle < endAngle) +1 else -1
    var a1 = startAngle
    val p1 = Point()
    val p2 = Point()
    val p3 = Point()
    val p4 = Point()
    var index = 0
    while (remainingAngle > EPSILON) {
        val a2 = a1 + sgn * kotlin.math.min(remainingAngle, PI_OVER_TWO)

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

fun VectorBuilder.circle(x: Double, y: Double, radius: Double) = arc(x, y, radius, 0.0, PI.toDouble() * 2f)

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
}

fun VectorBuilder.moveTo(p: Point) = moveTo(p.x, p.y)
fun VectorBuilder.lineTo(p: Point) = lineTo(p.x, p.y)
fun VectorBuilder.quadTo(c: Point, a: Point) = quadTo(c.x, c.y, a.x, a.y)
fun VectorBuilder.cubicTo(c1: Point, c2: Point, a: Point) = cubicTo(c1.x, c1.y, c2.x, c2.y, a.x, a.y)


fun VectorBuilder.moveToH(x: Double) = moveTo(x, lastY)
fun VectorBuilder.rMoveToH(x: Double) = rMoveTo(x, 0.0)

fun VectorBuilder.moveToV(y: Double) = moveTo(lastX, y)
fun VectorBuilder.rMoveToV(y: Double) = rMoveTo(0.0, y)

fun VectorBuilder.lineToH(x: Double) = lineTo(x, lastY)
fun VectorBuilder.rLineToH(x: Double) = rLineTo(x, 0.0)

fun VectorBuilder.lineToV(y: Double) = lineTo(lastX, y)
fun VectorBuilder.rLineToV(y: Double) = rLineTo(0.0, y)

fun VectorBuilder.rMoveTo(x: Double, y: Double) = moveTo(this.lastX + x, this.lastY + y)
fun VectorBuilder.rLineTo(x: Double, y: Double) = lineTo(this.lastX + x, this.lastY + y)

fun VectorBuilder.rQuadTo(cx: Double, cy: Double, ax: Double, ay: Double) = quadTo(this.lastX + cx, this.lastY + cy, this.lastX + ax, this.lastY + ay)

fun VectorBuilder.rCubicTo(cx1: Double, cy1: Double, cx2: Double, cy2: Double, ax: Double, ay: Double) = cubicTo(
    this.lastX + cx1, this.lastY + cy1, this.lastX + cx2, this.lastY + cy2, this.lastX + ax, this.lastY + ay
)


@Deprecated("Kotlin/Native boxes Number in inline")
inline fun VectorBuilder.moveTo(x: Number, y: Number) = moveTo(x.toDouble(), y.toDouble())
@Deprecated("Kotlin/Native boxes Number in inline")
inline fun VectorBuilder.lineTo(x: Number, y: Number) = lineTo(x.toDouble(), y.toDouble())
@Deprecated("Kotlin/Native boxes Number in inline")
inline fun VectorBuilder.quadTo(controlX: Number, controlY: Number, anchorX: Number, anchorY: Number) = quadTo(controlX.toDouble(), controlY.toDouble(), anchorX.toDouble(), anchorY.toDouble())
@Deprecated("Kotlin/Native boxes Number in inline")
inline fun VectorBuilder.cubicTo(cx1: Number, cy1: Number, cx2: Number, cy2: Number, ax: Number, ay: Number) = cubicTo(cx1.toDouble(), cy1.toDouble(), cx2.toDouble(), cy2.toDouble(), ax.toDouble(), ay.toDouble())

@Deprecated("Kotlin/Native boxes Number in inline")
inline fun VectorBuilder.moveToH(x: Number) = moveToH(x.toDouble())
@Deprecated("Kotlin/Native boxes Number in inline")
inline fun VectorBuilder.rMoveToH(x: Number) = rMoveToH(x.toDouble())

@Deprecated("Kotlin/Native boxes Number in inline")
inline fun VectorBuilder.moveToV(y: Number) = moveToV(y.toDouble())
@Deprecated("Kotlin/Native boxes Number in inline")
inline fun VectorBuilder.rMoveToV(y: Number) = rMoveToV(y.toDouble())

@Deprecated("Kotlin/Native boxes Number in inline")
inline fun VectorBuilder.lineToH(x: Number) = lineToH(x.toDouble())
@Deprecated("Kotlin/Native boxes Number in inline")
inline fun VectorBuilder.rLineToH(x: Number) = rLineToH(x.toDouble())

@Deprecated("Kotlin/Native boxes Number in inline")
inline fun VectorBuilder.lineToV(y: Number) = lineToV(y.toDouble())
@Deprecated("Kotlin/Native boxes Number in inline")
inline fun VectorBuilder.rLineToV(y: Number) = rLineToV(y.toDouble())

@Deprecated("Kotlin/Native boxes Number in inline")
inline fun VectorBuilder.rMoveTo(x: Number, y: Number) = rMoveTo(x.toDouble(), y.toDouble())
@Deprecated("Kotlin/Native boxes Number in inline")
inline fun VectorBuilder.rLineTo(x: Number, y: Number) = rLineTo(x.toDouble(), y.toDouble())

@Deprecated("Kotlin/Native boxes Number in inline")
inline fun VectorBuilder.rQuadTo(cx: Number, cy: Number, ax: Number, ay: Number) = rQuadTo(cx.toDouble(), cy.toDouble(), ax.toDouble(), ay.toDouble())
@Deprecated("Kotlin/Native boxes Number in inline")
inline fun VectorBuilder.rCubicTo(cx1: Number, cy1: Number, cx2: Number, cy2: Number, ax: Number, ay: Number) = rCubicTo(cx1.toDouble(), cy1.toDouble(), cx2.toDouble(), cy2.toDouble(), ax.toDouble(), ay.toDouble())
@Deprecated("Kotlin/Native boxes Number in inline")
inline fun VectorBuilder.arcTo(ax: Number, ay: Number, cx: Number, cy: Number, r: Number) = arcTo(ax.toDouble(), ay.toDouble(), cx.toDouble(), cy.toDouble(), r.toDouble())

fun VectorBuilder.line(x0: Double, y0: Double, x1: Double, y1: Double) = moveTo(x0, y0).also { lineTo(x1, y1) }
fun VectorBuilder.quad(x0: Double, y0: Double, controlX: Double, controlY: Double, anchorX: Double, anchorY: Double) = moveTo(x0, y0).also { quadTo(controlX, controlY, anchorX, anchorY) }
fun VectorBuilder.cubic(x0: Double, y0: Double, cx1: Double, cy1: Double, cx2: Double, cy2: Double, ax: Double, ay: Double) = moveTo(x0, y0).also { cubicTo(cx1, cy1, cx2, cy2, ax, ay) }

fun VectorBuilder.line(p0: Point, p1: Point) = line(p0.x, p0.y, p1.x, p1.y)
fun VectorBuilder.quad(o: Point, c: Point, a: Point) = quad(o.x, o.y, c.x, c.y, a.x, a.y)
fun VectorBuilder.cubic(o: Point, c1: Point, c2: Point, a: Point) = cubic(o.x, o.y, c1.x, c1.y, c2.x, c2.y, a.x, a.y)

@Deprecated("Kotlin/Native boxes Number in inline")
inline fun VectorBuilder.line(x0: Number, y0: Number, x1: Number, y1: Number) = line(x0.toDouble(), y0.toDouble(), x1.toDouble(), y1.toDouble())
@Deprecated("Kotlin/Native boxes Number in inline")
inline fun VectorBuilder.quad(x0: Number, y0: Number, controlX: Number, controlY: Number, anchorX: Number, anchorY: Number) = quad(x0.toDouble(), y0.toDouble(), controlX.toDouble(), controlY.toDouble(), anchorX.toDouble(), anchorY.toDouble())
@Deprecated("Kotlin/Native boxes Number in inline")
inline fun VectorBuilder.cubic(x0: Number, y0: Number, cx1: Number, cy1: Number, cx2: Number, cy2: Number, ax: Number, ay: Number) = cubic(x0.toDouble(), y0.toDouble(), cx1.toDouble(), cy1.toDouble(), cx2.toDouble(), cy2.toDouble(), ax.toDouble(), ay.toDouble())

@Deprecated("Kotlin/Native boxes Number in inline")
inline fun VectorBuilder.rect(x: Number, y: Number, width: Number, height: Number) = rect(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())
@Deprecated("Kotlin/Native boxes Number in inline")
inline fun VectorBuilder.rectHole(x: Number, y: Number, width: Number, height: Number) = rectHole(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())
@Deprecated("Kotlin/Native boxes Number in inline")
inline fun VectorBuilder.roundRect(x: Number, y: Number, w: Number, h: Number, rx: Number, ry: Number = rx) = roundRect(x.toDouble(), y.toDouble(), w.toDouble(), h.toDouble(), rx.toDouble(), ry.toDouble())
@Deprecated("Kotlin/Native boxes Number in inline")
inline fun VectorBuilder.arc(x: Number, y: Number, r: Number, start: Number, end: Number) = arc(x.toDouble(), y.toDouble(), r.toDouble(), start.toDouble(), end.toDouble())
@Deprecated("Kotlin/Native boxes Number in inline")
inline fun VectorBuilder.circle(x: Number, y: Number, radius: Number) = circle(x.toDouble(), y.toDouble(), radius.toDouble())
@Deprecated("Kotlin/Native boxes Number in inline")
inline fun VectorBuilder.ellipse(x: Number, y: Number, rw: Number, rh: Number) = ellipse(x.toDouble(), y.toDouble(), rw.toDouble(), rh.toDouble())

// Variants supporting relative and absolute modes

fun VectorBuilder.rCubicTo(cx1: Double, cy1: Double, cx2: Double, cy2: Double, ax: Double, ay: Double, relative: Boolean) = if (relative) rCubicTo(cx1, cy1, cx2, cy2, ax, ay) else cubicTo(cx1, cy1, cx2, cy2, ax, ay)
fun VectorBuilder.rQuadTo(cx: Double, cy: Double, ax: Double, ay: Double, relative: Boolean) = if (relative) rQuadTo(cx, cy, ax, ay) else quadTo(cx, cy, ax, ay)
fun VectorBuilder.rLineTo(ax: Double, ay: Double, relative: Boolean) = if (relative) rLineTo(ax, ay) else lineTo(ax, ay)
fun VectorBuilder.rMoveTo(ax: Double, ay: Double, relative: Boolean) = if (relative) rMoveTo(ax, ay) else moveTo(ax, ay)
fun VectorBuilder.rMoveToH(ax: Double, relative: Boolean) = if (relative) rMoveToH(ax) else moveToH(ax)
fun VectorBuilder.rMoveToV(ay: Double, relative: Boolean) = if (relative) rMoveToV(ay) else moveToV(ay)
fun VectorBuilder.rLineToH(ax: Double, relative: Boolean) = if (relative) rLineToH(ax) else lineToH(ax)
fun VectorBuilder.rLineToV(ay: Double, relative: Boolean) = if (relative) rLineToV(ay) else lineToV(ay)

@Deprecated("Kotlin/Native boxes Number in inline")
inline fun VectorBuilder.rCubicTo(cx1: Number, cy1: Number, cx2: Number, cy2: Number, ax: Number, ay: Number, relative: Boolean) = if (relative) rCubicTo(cx1, cy1, cx2, cy2, ax, ay) else cubicTo(cx1, cy1, cx2, cy2, ax, ay)
@Deprecated("Kotlin/Native boxes Number in inline")
inline fun VectorBuilder.rQuadTo(cx: Number, cy: Number, ax: Number, ay: Number, relative: Boolean) = if (relative) rQuadTo(cx, cy, ax, ay) else quadTo(cx, cy, ax, ay)
@Deprecated("Kotlin/Native boxes Number in inline")
inline fun VectorBuilder.rLineTo(ax: Number, ay: Number, relative: Boolean) = if (relative) rLineTo(ax, ay) else lineTo(ax, ay)
@Deprecated("Kotlin/Native boxes Number in inline")
inline fun VectorBuilder.rMoveTo(ax: Number, ay: Number, relative: Boolean) = if (relative) rMoveTo(ax, ay) else moveTo(ax, ay)
@Deprecated("Kotlin/Native boxes Number in inline")
inline fun VectorBuilder.rMoveToH(ax: Number, relative: Boolean) = if (relative) rMoveToH(ax) else moveToH(ax)
@Deprecated("Kotlin/Native boxes Number in inline")
inline fun VectorBuilder.rMoveToV(ay: Number, relative: Boolean) = if (relative) rMoveToV(ay) else moveToV(ay)
@Deprecated("Kotlin/Native boxes Number in inline")
inline fun VectorBuilder.rLineToH(ax: Number, relative: Boolean) = if (relative) rLineToH(ax) else lineToH(ax)
@Deprecated("Kotlin/Native boxes Number in inline")
inline fun VectorBuilder.rLineToV(ay: Number, relative: Boolean) = if (relative) rLineToV(ay) else lineToV(ay)

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

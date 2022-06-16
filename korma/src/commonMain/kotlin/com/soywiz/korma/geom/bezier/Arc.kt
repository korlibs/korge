package com.soywiz.korma.geom.bezier

import com.soywiz.korma.geom.Angle
import com.soywiz.korma.geom.IPoint
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.abs
import com.soywiz.korma.geom.absoluteValue
import com.soywiz.korma.geom.cosine
import com.soywiz.korma.geom.degrees
import com.soywiz.korma.geom.div
import com.soywiz.korma.geom.min
import com.soywiz.korma.geom.minus
import com.soywiz.korma.geom.mutable
import com.soywiz.korma.geom.normalized
import com.soywiz.korma.geom.plus
import com.soywiz.korma.geom.shape.buildVectorPath
import com.soywiz.korma.geom.sine
import com.soywiz.korma.geom.tangent
import com.soywiz.korma.geom.times
import com.soywiz.korma.geom.unit
import com.soywiz.korma.geom.vector.VectorBuilder
import com.soywiz.korma.geom.vector.arc
import com.soywiz.korma.geom.vector.isEmpty
import com.soywiz.korma.geom.vector.toCurves
import kotlin.math.PI
import kotlin.math.sin

object Arc {
    // http://hansmuller-flex.blogspot.com/2011/04/approximating-circular-arc-with-cubic.html
    //val K = (4.0 / 3.0) * (sqrt(2.0) - 1.0)
    const val K = 0.5522847498307933

    fun arcToPath(out: VectorBuilder, ax: Double, ay: Double, cx: Double, cy: Double, r: Double) {
        if (out.isEmpty()) out.moveTo(ax, ay)
        val bx = out.lastX
        val by = out.lastY
        val b = IPoint(bx, by)
        val a = IPoint(ax, ay)
        val c = IPoint(cx, cy)
        val AB = b - a
        val AC = c - a
        val angle = Point.angleArc(AB, AC).radians * 0.5
        val x = r * sin((PI / 2.0) - angle) / sin(angle)
        val A = a + AB.unit * x
        val B = a + AC.unit * x
        out.lineTo(A.x, A.y)
        out.quadTo(a.x, a.y, B.x, B.y)
    }

    fun ellipsePath(out: VectorBuilder, x: Double, y: Double, rw: Double, rh: Double) {
        val k = K
        val ox = (rw / 2) * k
        val oy = (rh / 2) * k
        val xe = x + rw
        val ye = y + rh
        val xm = x + rw / 2
        val ym = y + rh / 2
        out.moveTo(x, ym)
        out.cubicTo(x, ym - oy, xm - ox, y, xm, y)
        out.cubicTo(xm + ox, y, xe, ym - oy, xe, ym)
        out.cubicTo(xe, ym + oy, xm + ox, ye, xm, ye)
        out.cubicTo(xm - ox, ye, x, ym + oy, x, ym)
        out.close()
    }

    fun arcPath(out: VectorBuilder, p1: IPoint, p2: IPoint, radius: Double, counterclockwise: Boolean = false) {
        val circleCenter = findArcCenter(p1, p2, radius)
        arcPath(out, circleCenter.x, circleCenter.y, radius, Angle.between(circleCenter, p1), Angle.between(circleCenter, p2), counterclockwise)
    }

    fun arcPath(out: VectorBuilder, x: Double, y: Double, r: Double, start: Angle, end: Angle, counterclockwise: Boolean = false) {
        // http://hansmuller-flex.blogspot.com.es/2011/04/approximating-circular-arc-with-cubic.html
        val startAngle = start.normalized
        val endAngle1 = end.normalized
        val endAngle = if (endAngle1 < startAngle) (endAngle1 + Angle.FULL) else endAngle1
        var remainingAngle = min(Angle.FULL, abs(endAngle - startAngle))
        //println("remainingAngle=$remainingAngle")
        if (remainingAngle.absoluteValue < Angle.EPSILON && start != end) remainingAngle = Angle.FULL
        val sgn1 = if (startAngle < endAngle) +1 else -1
        val sgn = if (counterclockwise) -sgn1 else sgn1
        if (counterclockwise) remainingAngle = Angle.FULL - remainingAngle
        var a1 = startAngle
        var index = 0

        //println("start=$start, end=$end, startAngle=$startAngle, remainingAngle=$remainingAngle")

        while (remainingAngle > Angle.EPSILON) {
            val a2 = a1 + min(remainingAngle, Angle.QUARTER) * sgn

            val a = (a2 - a1) / 2.0
            val x4 = r * a.cosine
            val y4 = r * a.sine
            val x1 = x4
            val y1 = -y4
            val f = K * a.tangent
            val x2 = x1 + f * y4
            val y2 = y1 + f * x4
            val x3 = x2
            val y3 = -y2
            val ar = a + a1
            val cos_ar = ar.cosine
            val sin_ar = ar.sine

            if (index == 0) {
                out.moveTo(x + r * a1.cosine, y + r * a1.sine)
            }
            out.cubicTo(
                x + x2 * cos_ar - y2 * sin_ar, y + x2 * sin_ar + y2 * cos_ar,
                x + x3 * cos_ar - y3 * sin_ar, y + x3 * sin_ar + y3 * cos_ar,
                x + r * a2.cosine, y + r * a2.sine
            )
            //println(" - ARC: $a1-$a2")
            index++
            remainingAngle -= abs(a2 - a1)
            a1 = a2
        }
        if (startAngle == endAngle && index != 0) out.close()
    }

    // c = √(a² + b²)
    // b = √(c² - a²)
    private fun triangleFindSideFromSideAndHypot(side: Double, hypot: Double): Double =
        kotlin.math.sqrt(hypot * hypot - side * side)

    fun findArcCenter(p1: IPoint, p2: IPoint, radius: Double, out: Point = Point()): IPoint {
        val tangent = p2 - p1
        val normal = tangent.mutable.setToNormal().normalized
        val mid = (p1 + p2) / 2.0
        val lineLen = triangleFindSideFromSideAndHypot(Point.distance(p1, mid), radius)
        return out.copyFrom(mid + normal * lineLen)
    }

    fun createArc(p1: IPoint, p2: IPoint, radius: Double, counterclockwise: Boolean = false): Curves =
        buildVectorPath { arcPath(this, p1, p2, radius, counterclockwise) }.toCurves()

    fun createEllipse(x: Double, y: Double, rw: Double, rh: Double): Curves =
        buildVectorPath { ellipsePath(this, x, y, rw, rh) }.toCurves()

    fun createCircle(x: Double, y: Double, radius: Double): Curves =
        buildVectorPath { arc(x, y, radius, Angle.ZERO, Angle.FULL) }.toCurves()

    fun createArc(x: Double, y: Double, r: Double, start: Angle, end: Angle, counterclockwise: Boolean = false): Curves =
        buildVectorPath { arcPath(this, x, y, r, start, end, counterclockwise) }.toCurves()

}

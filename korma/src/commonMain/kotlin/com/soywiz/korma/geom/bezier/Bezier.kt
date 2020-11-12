package com.soywiz.korma.geom.bezier

import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.interpolate
import com.soywiz.korma.interpolation.*
import com.soywiz.korma.math.*
import kotlin.math.*

//(x0,y0) is start point; (x1,y1),(x2,y2) is control points; (x3,y3) is end point.
// https://pomax.github.io/bezierinfo/
interface Bezier {
    fun getBounds(target: Rectangle = Rectangle()): Rectangle
    fun calc(t: Double, target: Point = Point()): Point

    class Quad(
        p0x: Double = 0.0, p0y: Double = 0.0,
        p1x: Double = 0.0, p1y: Double = 0.0,
        p2x: Double = 0.0, p2y: Double = 0.0,
    ) : Bezier {
        val p0 = Point(p0x, p0y)
        val p1 = Point(p1x, p1y)
        val p2 = Point(p2x, p2y)

        constructor(p0: IPoint, p1: IPoint, p2: IPoint) : this(
            p0.x, p0.y,
            p1.x, p1.y,
            p2.x, p2.y,
        )

        fun setTo(
            p0x: Double, p0y: Double,
            p1x: Double, p1y: Double,
            p2x: Double, p2y: Double,
        ): Quad {
            this.p0.setTo(p0x, p0y)
            this.p1.setTo(p1x, p1y)
            this.p2.setTo(p2x, p2y)
            return this
        }
        fun setTo(p0: IPoint, p1: IPoint, p2: IPoint) = setTo(p0.x, p0.y, p1.x, p1.y, p2.x, p2.y)
        fun copyFrom(other: Quad): Quad = setTo(other.p0, other.p1, other.p2)
        override fun getBounds(target: Rectangle): Rectangle = quadBounds(p0.x, p0.y, p1.x, p1.y, p2.x, p2.y, target)
        override fun calc(t: Double, target: Point): Point = quadCalc(p0.x, p0.y, p1.x, p1.y, p2.x, p2.y, t, target)

        // http://fontforge.github.io/bezier.html
        fun toCubic(out: Cubic = Cubic()): Cubic {
            return out.setTo(
                p0.x, p0.y,
                quadToCubic1(p0.x, p1.x, p2.x), quadToCubic1(p0.y, p1.y, p2.y),
                quadToCubic2(p0.x, p1.x, p2.x), quadToCubic2(p0.y, p1.y, p2.y),
                p2.x, p2.y,
            )
        }

        override fun toString(): String = "Bezier.Quad($p0, $p1, $p2)"
    }

    class Cubic(
        p0x: Double = 0.0, p0y: Double = 0.0,
        p1x: Double = 0.0, p1y: Double = 0.0,
        p2x: Double = 0.0, p2y: Double = 0.0,
        p3x: Double = 0.0, p3y: Double = 0.0,
    ) : Bezier {
        val p0 = Point(p0x, p0y)
        val p1 = Point(p1x, p1y)
        val p2 = Point(p2x, p2y)
        val p3 = Point(p3x, p3y)

        constructor(p0: IPoint, p1: IPoint, p2: IPoint, p3: IPoint) : this(
            p0.x, p0.y,
            p1.x, p1.y,
            p2.x, p2.y,
            p3.x, p3.y,
        )

        private val temp = Temp()

        fun setToSplitFirst(p0: Point, p1: Point, p2: Point, p3: Point, ratio: Double = 0.5): Cubic {
            val np1 = temp.tpoint0.setToInterpolated(ratio, p0, p1)
            val t = temp.tpoint1.setToInterpolated(ratio, p1, p2)
            val np2 = temp.tpoint2.setToInterpolated(ratio, np1, t)
            val p3 = cubicCalc(p0, p1, p2, p3, ratio, temp.tpoint3)
            return setTo(p0, np1, np2, p3)
        }

        fun setToSplitFirst(cubic: Cubic, ratio: Double = 0.5): Cubic {
            return setToSplitFirst(cubic.p0, cubic.p1, cubic.p2, cubic.p3, ratio)
        }

        fun setToSplitSecond(cubic: Cubic, ratio: Double = 0.5): Cubic {
            return setToSplitFirst(cubic.p3, cubic.p2, cubic.p1, cubic.p0, 1.0 - ratio).reverseDirection()
        }

        fun reverseDirection(): Cubic {
            temp.tpoint0.copyFrom(p0)
            temp.tpoint1.copyFrom(p1)
            temp.tpoint2.copyFrom(p2)
            temp.tpoint3.copyFrom(p3)
            p0.copyFrom(temp.tpoint3)
            p1.copyFrom(temp.tpoint2)
            p2.copyFrom(temp.tpoint1)
            p3.copyFrom(temp.tpoint0)
            return this
        }

        fun setTo(
            p0x: Double, p0y: Double,
            p1x: Double, p1y: Double,
            p2x: Double, p2y: Double,
            p3x: Double, p3y: Double,
        ): Cubic {
            this.p0.setTo(p0x, p0y)
            this.p1.setTo(p1x, p1y)
            this.p2.setTo(p2x, p2y)
            this.p3.setTo(p3x, p3y)
            return this
        }
        fun setTo(p0: IPoint, p1: IPoint, p2: IPoint, p3: IPoint) = setTo(p0.x, p0.y, p1.x, p1.y, p2.x, p2.y, p3.x, p3.y)
        fun copyFrom(other: Cubic): Cubic = setTo(other.p0, other.p1, other.p2, other.p3)
        override fun getBounds(target: Rectangle): Rectangle = cubicBounds(p0.x, p0.y, p1.x, p1.y, p2.x, p2.y, p3.x, p3.y, target, temp)
        override fun calc(t: Double, target: Point): Point = cubicCalc(p0.x, p0.y, p1.x, p1.y, p2.x, p2.y, p3.x, p3.y, t, target)
        fun clone() = Cubic(p0.x, p0.y, p1.x, p1.y, p2.x, p2.y, p3.x, p3.y)

        override fun toString(): String = "Bezier.Cubic($p0, $p1, $p2, $p3)"
    }

    class Temp {
        val tvalues = DoubleArray(6)
        val xvalues = DoubleArray(8)
        val yvalues = DoubleArray(8)
        var tpoint0 = Point()
        var tpoint1 = Point()
        var tpoint2 = Point()
        var tpoint3 = Point()
    }

    companion object {
        operator fun invoke(p0: IPoint, p1: IPoint, p2: IPoint): Bezier.Quad = Bezier.Quad(p0, p1, p2)
        operator fun invoke(p0: IPoint, p1: IPoint, p2: IPoint, p3: IPoint): Bezier.Cubic =
            Bezier.Cubic(p0, p1, p2, p3)


        // http://fontforge.github.io/bezier.html
        //Any quadratic spline can be expressed as a cubic (where the cubic term is zero). The end points of the cubic will be the same as the quadratic's.
        //CP0 = QP0
        //CP3 = QP2
        //The two control points for the cubic are:
        //CP1 = QP0 + 2/3 *(QP1-QP0)
        //CP2 = QP2 + 2/3 *(QP1-QP2)
        // @TODO: Is there a bug here when inlining?
        inline fun <T> quadToCubic(
            x0: Double, y0: Double, xc: Double, yc: Double, x1: Double, y1: Double,
            bezier: (qx0: Double, qy0: Double, qx1: Double, qy1: Double, qx2: Double, qy2: Double, qx3: Double, qy3: Double) -> T
        ): T {
            return bezier(
                x0, y0,
                quadToCubic1(x0, xc, x1), quadToCubic1(y0, yc, y1),
                quadToCubic2(x0, xc, x1), quadToCubic2(y0, yc, y1),
                x1, y1
            )
        }

        @Suppress("UNUSED_PARAMETER")
        fun quadToCubic1(v0: Double, v1: Double, v2: Double) = v0 + (v1 - v0) * (2.0 / 3.0)
        @Suppress("UNUSED_PARAMETER")
        fun quadToCubic2(v0: Double, v1: Double, v2: Double) = v2 + (v1 - v2) * (2.0 / 3.0)

        // @TODO: Make an optimized version!
        fun quadBounds(
            x0: Double, y0: Double,
            xc: Double, yc: Double,
            x1: Double, y1: Double,
            target: Rectangle = Rectangle(),
            temp: Temp = Temp()
        ): Rectangle = cubicBounds(
            x0, y0,
            quadToCubic1(x0, xc, x1), quadToCubic1(y0, yc, y1),
            quadToCubic2(x0, xc, x1), quadToCubic2(y0, yc, y1),
            x1, y1,
            target, temp
        )

        inline fun <T> quadCalc(
            x0: Double, y0: Double,
            xc: Double, yc: Double,
            x1: Double, y1: Double,
            t: Double,
            emit: (x: Double, y: Double) -> T
        ): T {
            //return quadToCubic(x0, y0, xc, yc, x1, y1) { x0, y0, x1, y1, x2, y2, x3, y3 -> cubicCalc(x0, y0, x1, y1, x2, y2, x3, y3, t, emit) }
            val t1 = (1 - t)
            val a = t1 * t1
            val c = t * t
            val b = 2 * t1 * t
            return emit(
                a * x0 + b * xc + c * x1,
                a * y0 + b * yc + c * y1
            )
        }

        fun quadCalc(
            x0: Double, y0: Double,
            xc: Double, yc: Double,
            x1: Double, y1: Double,
            t: Double,
            target: Point = Point()
        ): Point = quadCalc(x0, y0, xc, yc, x1, y1, t) { x, y -> target.setTo(x, y) }

        fun cubicBounds(
            x0: Double, y0: Double, x1: Double, y1: Double,
            x2: Double, y2: Double, x3: Double, y3: Double,
            target: Rectangle = Rectangle(),
            temp: Temp = Temp()
        ): Rectangle {
            var j = 0
            var a: Double
            var b: Double
            var c: Double
            var b2ac: Double
            var sqrtb2ac: Double
            for (i in 0 until 2) {
                if (i == 0) {
                    b = 6 * x0 - 12 * x1 + 6 * x2
                    a = -3 * x0 + 9 * x1 - 9 * x2 + 3 * x3
                    c = 3 * x1 - 3 * x0
                } else {
                    b = 6 * y0 - 12 * y1 + 6 * y2
                    a = -3 * y0 + 9 * y1 - 9 * y2 + 3 * y3
                    c = 3 * y1 - 3 * y0
                }
                if (abs(a) < 1e-12) {
                    if (abs(b) >= 1e-12) {
                        val t = -c / b
                        if (0 < t && t < 1) temp.tvalues[j++] = t
                    }
                } else {
                    b2ac = b * b - 4 * c * a
                    if (b2ac < 0) continue
                    sqrtb2ac = sqrt(b2ac)
                    val t1 = (-b + sqrtb2ac) / (2.0 * a)
                    if (0 < t1 && t1 < 1) temp.tvalues[j++] = t1
                    val t2 = (-b - sqrtb2ac) / (2.0 * a)
                    if (0 < t2 && t2 < 1) temp.tvalues[j++] = t2
                }
            }

            while (j-- > 0) {
                val t = temp.tvalues[j]
                val mt = 1 - t
                temp.xvalues[j] = (mt * mt * mt * x0) + (3 * mt * mt * t * x1) + (3 * mt * t * t * x2) +
                    (t * t * t * x3)
                temp.yvalues[j] = (mt * mt * mt * y0) + (3 * mt * mt * t * y1) + (3 * mt * t * t * y2) +
                    (t * t * t * y3)
            }

            temp.xvalues[temp.tvalues.size + 0] = x0
            temp.xvalues[temp.tvalues.size + 1] = x3
            temp.yvalues[temp.tvalues.size + 0] = y0
            temp.yvalues[temp.tvalues.size + 1] = y3

            return target.setBounds(
                temp.xvalues.minOrElse(0.0),
                temp.yvalues.minOrElse(0.0),
                temp.xvalues.maxOrElse(0.0),
                temp.yvalues.maxOrElse(0.0)
            )
        }

        inline fun <T> cubicCalc(
            x0: Double, y0: Double, x1: Double, y1: Double,
            x2: Double, y2: Double, x3: Double, y3: Double,
            t: Double,
            emit: (x: Double, y: Double) -> T
        ): T {
            val cx = 3f * (x1 - x0)
            val bx = 3f * (x2 - x1) - cx
            val ax = x3 - x0 - cx - bx

            val cy = 3f * (y1 - y0)
            val by = 3f * (y2 - y1) - cy
            val ay = y3 - y0 - cy - by

            val tSquared = t * t
            val tCubed = tSquared * t

            return emit(
                ax * tCubed + bx * tSquared + cx * t + x0,
                ay * tCubed + by * tSquared + cy * t + y0
            )
        }

        // http://stackoverflow.com/questions/7348009/y-coordinate-for-a-given-x-cubic-bezier
        fun cubicCalc(
            x0: Double, y0: Double, x1: Double, y1: Double,
            x2: Double, y2: Double, x3: Double, y3: Double,
            t: Double, target: Point = Point()
        ): Point = cubicCalc(x0, y0, x1, y1, x2, y2, x3, y3, t) { x, y -> target.setTo(x, y) }

        // Suggested number of points
        fun quadNPoints(x0: Double, y0: Double, cx: Double, cy: Double, x1: Double, y1: Double, scale: Double = 1.0): Int {
            return ((Point.distance(x0, y0, cx, cy) + Point.distance(cx, cy, x1, y1)) * scale).toInt().clamp(5, 256)
        }

        // Suggested number of points
        fun cubicNPoints(x0: Double, y0: Double, cx1: Double, cy1: Double, cx2: Double, cy2: Double, x1: Double, y1: Double, scale: Double = 1.0): Int {
            return ((Point.distance(x0, y0, cx1, cy1) + Point.distance(cx1, cy1, cx2, cy2) + Point.distance(cx2, cy2, x1, y1)) * scale).toInt().clamp(5, 256)
        }

        fun cubicCalc(
            p0: Point, p1: Point, p2: Point, p3: Point,
            t: Double, target: Point = Point()
        ): Point = cubicCalc(p0.x, p0.y, p1.x, p1.y, p2.x, p2.y, p3.x, p3.y, t, target)

        fun quadCalc(
            p0: Point, p1: Point, p2: Point,
            t: Double, target: Point = Point()
        ): Point = quadCalc(p0.x, p0.y, p1.x, p1.y, p2.x, p2.y, t, target)

    }
}

fun Bezier.length(steps: Int = 100, temp: Point = Point()): Double {
    val dt = 1.0 / steps
    var oldX = 0.0
    var oldY = 0.0
    var length = 0.0
    for (n in 0..steps) {
        calc(dt * n, temp)
        if (n != 0) {
            length += hypot(oldX - temp.x, oldY - temp.y)
        }
        oldX = temp.x
        oldY = temp.y
    }
    return length
}

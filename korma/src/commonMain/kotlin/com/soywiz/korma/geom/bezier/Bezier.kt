package com.soywiz.korma.geom.bezier

import com.soywiz.kds.iterators.fastForEach
import com.soywiz.korma.geom.BoundsBuilder
import com.soywiz.korma.geom.IPoint
import com.soywiz.korma.geom.IPointArrayList
import com.soywiz.korma.geom.IRectangle
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.PointArrayList
import com.soywiz.korma.geom.PointPool
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.interpolation.interpolate
import com.soywiz.korma.math.clamp
import kotlin.jvm.JvmName
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

//(x0,y0) is start point; (x1,y1),(x2,y2) is control points; (x3,y3) is end point.
// https://pomax.github.io/bezierinfo/
@Deprecated("Use BezierCurve instead")
interface Bezier {
    val order: Int
    fun getBounds(target: Rectangle = Rectangle()): Rectangle
    fun calc(t: Double, target: Point = Point()): Point
    fun length(steps: Int = recommendedDivisions()): Double
    fun recommendedDivisions(): Int = Curve.DEFAULT_STEPS

    @Deprecated("Use BezierCurve instead")
    class Quad(
        p0x: Double = 0.0, p0y: Double = 0.0,
        p1x: Double = 0.0, p1y: Double = 0.0,
        p2x: Double = 0.0, p2y: Double = 0.0,
    ) : Bezier {
        override val order: Int get() = 2

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
        override fun length(steps: Int): Double = quadLength(p0.x, p0.y, p1.x, p1.y, p2.x, p2.y, steps)
        override fun recommendedDivisions(): Int = quadRecommendedSteps(p0.x, p0.y, p1.x, p1.y, p2.x, p2.y)

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

    @Deprecated("Use BezierCurve instead")
    class Cubic(
        p0x: Double = 0.0, p0y: Double = 0.0,
        p1x: Double = 0.0, p1y: Double = 0.0,
        p2x: Double = 0.0, p2y: Double = 0.0,
        p3x: Double = 0.0, p3y: Double = 0.0,
    ) : Bezier {
        override val order: Int get() = 3

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

        fun setTo(p0: IPoint, p1: IPoint, p2: IPoint, p3: IPoint) =
            setTo(p0.x, p0.y, p1.x, p1.y, p2.x, p2.y, p3.x, p3.y)

        fun copyFrom(other: Cubic): Cubic = setTo(other.p0, other.p1, other.p2, other.p3)
        override fun getBounds(target: Rectangle): Rectangle =
            cubicBounds(p0.x, p0.y, p1.x, p1.y, p2.x, p2.y, p3.x, p3.y, target, temp)

        override fun calc(t: Double, target: Point): Point =
            cubicCalc(p0.x, p0.y, p1.x, p1.y, p2.x, p2.y, p3.x, p3.y, t, target)

        override fun length(steps: Int): Double =
            cubicLength(p0.x, p0.y, p1.x, p1.y, p2.x, p2.y, p3.x, p3.y, steps)

        override fun recommendedDivisions(): Int =
            cubicRecommendedSteps(p0.x, p0.y, p1.x, p1.y, p2.x, p2.y, p3.x, p3.y)

        fun clone() = Cubic(p0.x, p0.y, p1.x, p1.y, p2.x, p2.y, p3.x, p3.y)

        override fun toString(): String = "Bezier.Cubic($p0, $p1, $p2, $p3)"
    }

    class Temp {
        val tvalues = DoubleArray(6)
        val xvalues = DoubleArray(8)
        val yvalues = DoubleArray(8)
        val points = PointPool()
        var tpoint0 = Point()
        var tpoint1 = Point()
        var tpoint2 = Point()
        var tpoint3 = Point()
    }

    companion object {
        operator fun invoke(p0: IPoint, p1: IPoint, p2: IPoint): Quad = Quad(p0, p1, p2)
        operator fun invoke(p0: IPoint, p1: IPoint, p2: IPoint, p3: IPoint): Cubic =
            Cubic(p0, p1, p2, p3)


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

        // https://iquilezles.org/articles/bezierbbox/
        @Deprecated("Allocates")
        fun quadBounds(
            x0: Double, y0: Double,
            xc: Double, yc: Double,
            x1: Double, y1: Double,
            target: Rectangle = Rectangle(),
            temp: Temp = Temp()
        ): Rectangle = target.copyFrom(BezierCurve(x0, y0, xc, yc, x1, y1).boundingBox)

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

        @Deprecated("Allocates")
        fun cubicBounds(
            x0: Double, y0: Double, x1: Double, y1: Double,
            x2: Double, y2: Double, x3: Double, y3: Double,
            target: Rectangle = Rectangle(),
            temp: Temp = Temp()
        ): Rectangle {
            return target.copyFrom(BezierCurve(x0, y0, x1, y1, x2, y2, x3, y3).boundingBox)
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

        fun cubicCalc(
            p0: IPoint, p1: IPoint, p2: IPoint, p3: IPoint,
            t: Double, target: Point = Point()
        ): IPoint = cubicCalc(p0.x, p0.y, p1.x, p1.y, p2.x, p2.y, p3.x, p3.y, t, target)

        fun quadCalc(
            p0: IPoint, p1: IPoint, p2: IPoint,
            t: Double, target: Point = Point()
        ): IPoint = quadCalc(p0.x, p0.y, p1.x, p1.y, p2.x, p2.y, t, target)

        fun quadRecommendedSteps(x0: Double, y0: Double, x1: Double, y1: Double, x2: Double, y2: Double): Int =
            Curve.DEFAULT_STEPS

        fun quadLength(x0: Double, y0: Double, x1: Double, y1: Double, x2: Double, y2: Double, steps: Int = quadRecommendedSteps(x0, y0, x1, y1, x2, y2)): Double =
            length(steps) { ratio, consumer -> quadCalc(x0, y0, x1, y1, x2, y2, ratio, consumer) }

        fun cubicRecommendedSteps(x0: Double, y0: Double, x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double): Int =
            Curve.DEFAULT_STEPS

        fun cubicLength(x0: Double, y0: Double, x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double, steps: Int = cubicRecommendedSteps(x0, y0, x1, y1, x2, y2, x3, y3)): Double {
            return length(steps) { ratio, consumer -> cubicCalc(x0, y0, x1, y1, x2, y2, x3, y3, ratio, consumer) }
        }

        inline fun length(steps: Int, calc: (ratio: Double, consumer: (x: Double, y: Double) -> Unit) -> Unit): Double {
            val dt = 1.0 / steps
            var oldX = 0.0
            var oldY = 0.0
            var length = 0.0
            for (n in 0..steps) {
                var tempX = 0.0
                var tempY = 0.0
                calc(dt * n) { x, y ->
                    tempX = x
                    tempY = y
                }
                if (n != 0) {
                    length += hypot(oldX - tempX, oldY - tempY)
                }
                oldX = tempX
                oldY = tempY
            }
            return length
        }
    }
}

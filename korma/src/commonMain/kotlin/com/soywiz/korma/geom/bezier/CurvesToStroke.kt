package com.soywiz.korma.geom.bezier

import com.soywiz.kds.forEachRatio01
import com.soywiz.kds.getCyclic
import com.soywiz.korma.geom.Angle
import com.soywiz.korma.geom.IPoint
import com.soywiz.korma.geom.IPointArrayList
import com.soywiz.korma.geom.Line
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.PointArrayList
import com.soywiz.korma.geom.VectorArrayList
import com.soywiz.korma.geom.absoluteValue
import com.soywiz.korma.geom.degrees
import com.soywiz.korma.geom.fastForEachGeneric
import com.soywiz.korma.geom.interpolate
import com.soywiz.korma.geom.length
import com.soywiz.korma.geom.lineIntersectionPoint
import com.soywiz.korma.geom.minus
import com.soywiz.korma.geom.mutable
import com.soywiz.korma.geom.normalized
import com.soywiz.korma.geom.plus
import com.soywiz.korma.geom.projectedPoint
import com.soywiz.korma.geom.umod
import com.soywiz.korma.geom.vector.LineCap
import com.soywiz.korma.geom.vector.LineJoin
import com.soywiz.korma.interpolation.interpolate
import com.soywiz.korma.math.clamp
import kotlin.math.absoluteValue
import kotlin.math.sign

// @TODO
//private fun Curves.toStrokeCurves(join: LineJoin, startCap: LineCap, endCap: LineCap): Curves {
//    TODO()
//}

enum class StrokePointsMode {
    SCALABLE_POS_NORMAL_WIDTH,
    NON_SCALABLE_POS
}

/**
 * A generic stroke points with either [x, y] or [x, y, dx, dy, scale] components when having separate components,
 * it is possible to later scale the stroke without regenerating it by adjusting the [scale] component
 */
interface StrokePoints {
    val vector: VectorArrayList
    val debugPoints: IPointArrayList
    val debugSegments: List<Line>
    val mode: StrokePointsMode

    fun scale(scale: Double) {
        if (mode == StrokePointsMode.SCALABLE_POS_NORMAL_WIDTH) {
            vector.fastForEachGeneric {
                this.set(it, 4, this.get(it, 4) * scale)
            }
        }
    }
}

class StrokePointsBuilder(val width: Double, override val mode: StrokePointsMode = StrokePointsMode.NON_SCALABLE_POS, val generateDebug: Boolean = false) : StrokePoints {
    val NSTEPS = 20

    override val vector: VectorArrayList = VectorArrayList(dimensions = when (mode) {
        StrokePointsMode.SCALABLE_POS_NORMAL_WIDTH -> 5 // x, y, dx, dy, length
        StrokePointsMode.NON_SCALABLE_POS -> 2 // x, y
    })

    override val debugPoints: PointArrayList = PointArrayList()
    override val debugSegments: ArrayList<Line> = arrayListOf()

    fun addPoint(pos: IPoint, normal: IPoint, width: Double) = when (mode) {
        StrokePointsMode.SCALABLE_POS_NORMAL_WIDTH -> vector.add(pos.x, pos.y, normal.x, normal.y, width)
        StrokePointsMode.NON_SCALABLE_POS -> vector.add(pos.x + normal.x * width, pos.y + normal.y * width)
    }

    fun addPointRelative(center: IPoint, pos: IPoint, sign: Double = 1.0) {
        addPoint(center, (pos - center).normalized, (pos - center).length * sign)
    }

    fun addTwoPoints(pos: IPoint, normal: IPoint, width: Double) {
        addPoint(pos, normal, width)
        addPoint(pos, normal, -width)
    }

    fun addJoin(curr: Curve, next: Curve, kind: LineJoin, miterLimitRatio: Double) {
        val commonPoint = curr.calc(1.0)
        val currTangent = curr.tangent(1.0)
        val currNormal = curr.normal(1.0)
        val nextTangent = next.tangent(0.0)
        val nextNormal = next.normal(0.0)

        val currLine0 = Line.fromPointAndDirection(commonPoint + currNormal * width, currTangent)
        val currLine1 = Line.fromPointAndDirection(commonPoint + currNormal * -width, currTangent)

        val nextLine0 = Line.fromPointAndDirection(commonPoint + nextNormal * width, nextTangent)
        val nextLine1 = Line.fromPointAndDirection(commonPoint + nextNormal * -width, nextTangent)

        val intersection0 = Line.lineIntersectionPoint(currLine0, nextLine0)
        val intersection1 = Line.lineIntersectionPoint(currLine1, nextLine1)
        if (intersection0 == null || intersection1 == null) {
            //println("direction=$direction, currTangent=$currTangent, nextTangent=$nextTangent")
            val signChanged = currTangent.x.sign != nextTangent.x.sign || currTangent.y.sign != nextTangent.y.sign
            addTwoPoints(commonPoint, currNormal, if (signChanged) -width else width)
            return
        }

        val direction = Point.crossProduct(currTangent, nextTangent)
        val miterLength = Point.distance(intersection0, intersection1)
        val miterLimit = miterLimitRatio * width


        //println("miterLength=$miterLength, miterLimit=$miterLimit, sign=$direction")

        // Miter
        val angle = Angle.atan2(nextTangent) - Angle.atan2(currTangent)
        //if (angle.absoluteValue < 15.degrees) {
        //    val d0 = intersection0 - commonPoint
        //    val d1 = commonPoint - intersection1
        //    addPoint(commonPoint, d0.normalized, d0.length)
        //    addPoint(commonPoint, d1.normalized, -d1.length)
        //    return
        //}

        //println("angle=$angle, currTangent=$currTangent, nextTangent=$nextTangent")

        if (kind != LineJoin.MITER || miterLength > miterLimit) {
        //run {
            //val angle = Angle.between(nextTangent * 10.0, currTangent * 10.0)
            val p1 = if (direction < 0.0) currLine0.projectedPoint(commonPoint) else nextLine1.projectedPoint(commonPoint)
            val p2 = if (direction < 0.0) nextLine0.projectedPoint(commonPoint) else currLine1.projectedPoint(commonPoint)
            // @TODO: We should try to find the common edge (except when the two lines overlaps), to avoid overlapping in normal curves

            //println("direction=$direction")
            var p3: IPoint? = when {
                //angle.absoluteValue > 190.degrees -> null
                //else -> null
                direction <= 0.0 -> Line.lineIntersectionPoint(currLine1, nextLine1)
                else -> Line.lineIntersectionPoint(currLine0, nextLine0)
            }

            val p4Line = if (direction < 0.0) nextLine1 else nextLine0
            val p4 = p4Line.projectedPoint(commonPoint)
            //val p5 = Line.fromPointAndDirection(commonPoint, currTangent).getLineIntersectionPoint(p4Line)
            if (p3 == null) {
                p3 = p4
            }

            //val d3 = Point.distance(commonPoint, p3!!)
            //val d4 = Point.distance(commonPoint, p4)

            //if (p5 != null) {
            val angleB = (angle + 180.degrees).absoluteValue
            val angle2 = (angle umod 180.degrees).absoluteValue
            val angle3 = if (angle2 > 90.degrees) 180.degrees - angle2 else angle2
            val ratio = (angle3.ratio.absoluteValue * 4).clamp(0.0, 1.0)
            val p5 = ratio.interpolate(p4, p3!!.mutable)
            //println("angle3=$angle3, angle2=$angle2, ratio=$ratio")
            //}
            //p3 = if (Point.distance(p3, p4) < width) p3 else p4

            if (generateDebug) {
                debugSegments.add(nextLine1.scalePoints(1000.0).clone())
                debugSegments.add(currLine1.scalePoints(1000.0).clone())
                debugSegments.add(nextLine0.scalePoints(1000.0).clone())
                debugSegments.add(currLine0.scalePoints(1000.0).clone())
                debugSegments.add(Line.fromPointAndDirection(commonPoint, currTangent).scalePoints(1000.0).clone())
                debugSegments.add(Line.fromPointAndDirection(commonPoint, nextTangent).scalePoints(1000.0).clone())
                debugPoints.add(p3)
                debugPoints.add(p4)
                debugPoints.add(p5)
            }

            //println("angleB=$angleB")

            // @TODO: We cannot do this with the tangent lines, we should actually intersect the outline curves for this to work as expected
            //val p6 = p3
            //val p6 = if (angleB < 45.degrees) p5 else p3
            val p6 = p5
            //val p6 = p3

            if (direction < 0.0) {
                addPointRelative(commonPoint, p1)
                addPointRelative(commonPoint, p6)
                addPointRelative(commonPoint, p2)
                addPointRelative(commonPoint, p6)
            } else {
                addPointRelative(commonPoint, p6)
                addPointRelative(commonPoint, p2)
                addPointRelative(commonPoint, p6)
                addPointRelative(commonPoint, p1)
            }
            //addPoint(p1, Point(0, 0), 0.0)
            //addCurvePointsCap(p2, p1, 0.5)
            //addPoint(p2, Point(0, 0), 0.0)
            return
        }

        //if (false) {
        val d0 = intersection0 - commonPoint
        val d1 = commonPoint - intersection1

        addPoint(commonPoint, d0.normalized, d0.length)
        addPoint(commonPoint, d1.normalized, -d1.length)
    }

    fun addCap(curr: Curve, ratio: Double, kind: LineCap) {
        when (kind) {
            LineCap.SQUARE, LineCap.ROUND -> {
                val derivate = curr.normal(ratio).setToNormal().also { if (ratio == 1.0) it.neg() }
                when (kind) {
                    LineCap.SQUARE -> {
                        //val w = if (ratio == 1.0) -width else width
                        addTwoPoints(curr.calc(ratio) + derivate * width, curr.normal(ratio), width) // Not right
                    }
                    LineCap.ROUND -> {
                        val mid = curr.calc(ratio)
                        val normal = curr.normal(ratio)
                        val p0 = mid + normal * width
                        val p3 = mid + normal * -width
                        val a = if (ratio == 0.0) p0 else p3
                        val b = if (ratio == 0.0) p3 else p0
                        addCurvePointsCap(a, b, ratio, mid)
                    }
                    else -> error("Can't happen")
                }
            }
            LineCap.BUTT -> {
                addTwoPoints(curr.calc(ratio), curr.normal(ratio), width)
            }
        }
    }

    fun addCurvePointsCap(p0: IPoint, p3: IPoint, ratio: Double, mid: IPoint = Point.middle(p0, p3), nsteps: Int = NSTEPS) {
        val angleStart = Angle.between(mid, p0)
        val angleEnd = Angle.between(mid, p3)

        if (ratio == 1.0) addTwoPoints(mid, Point.fromPolar(angleEnd), width)
        val addAngle = if (Point.crossProduct(p0, p3) <= 0.0) Angle.ZERO else Angle.HALF
        forEachRatio01(nsteps, include0 = true, include1 = true) {
            val angle = it.interpolate(angleStart, angleEnd)
            val dir = Point.fromPolar(angle + addAngle)
            addPoint(mid, dir, 0.0)
            addPoint(mid, dir, width)
        }
        if (ratio == 0.0) addTwoPoints(mid, Point.fromPolar(angleStart), width)
    }

    // @TODO: instead of nsteps we should have some kind of threshold regarding to how much information do we lose at 1:1 scale
    fun addCurvePoints(curr: Curve, nsteps: Int = (curr.length() / 10.0).clamp(10.0, 100.0).toInt()) {
        // @TODO: Here we could generate curve information to render in the shader with a plain simple quadratic bezier to reduce the number of points and make the curve as accurate as possible
        forEachRatio01(nsteps, include0 = false, include1 = false) {
            addTwoPoints(curr.calc(it), curr.normal(it), width)
        }
    }

    fun addAllCurvesPoints(curves: Curves, join: LineJoin = LineJoin.MITER, startCap: LineCap = LineCap.BUTT, endCap: LineCap = LineCap.BUTT, miterLimit: Double = 10.0) {
        val closed = curves.closed
        val curves = curves.curves
        for (n in curves.indices) {
            val curr = curves.getCyclic(n + 0)
            val next = curves.getCyclic(n + 1)

            // Generate start cap
            if (n == 0) {
                if (closed) {
                    addJoin(curves.getCyclic(n - 1), curr, join, miterLimit)
                } else {
                    addCap(curr, 0.0, startCap)
                }
            }

            // Generate intermediate points for curves (no for plain lines)
            if (curr.order != 1) {
                addCurvePoints(curr)
            }

            // Generate join
            if (n < curves.size - 1) {
                addJoin(curr, next, join, miterLimit)
            }
            // Generate end cap
            else {
                //println("closed=$closed")
                if (closed) {
                    addJoin(curr, next, join, miterLimit)
                } else {
                    addCap(curr, 1.0, endCap)
                }
            }
        }
    }
}

/** Useful for drawing */
fun Curves.toStrokePoints(width: Double, join: LineJoin = LineJoin.MITER, startCap: LineCap = LineCap.BUTT, endCap: LineCap = LineCap.BUTT, miterLimit: Double = 10.0, mode: StrokePointsMode = StrokePointsMode.NON_SCALABLE_POS, generateDebug: Boolean = false): StrokePoints {
    //println("closed: $closed")
    return StrokePointsBuilder(width, mode, generateDebug).also {
        it.addAllCurvesPoints(this, join, startCap, endCap, miterLimit)
    }
}

fun List<Curves>.toStrokePointsList(width: Double, join: LineJoin = LineJoin.MITER, startCap: LineCap = LineCap.BUTT, endCap: LineCap = LineCap.BUTT, miterLimit: Double = 10.0, mode: StrokePointsMode = StrokePointsMode.NON_SCALABLE_POS, generateDebug: Boolean = false): List<StrokePoints> =
    this.map { it.toStrokePoints(width, join, startCap, endCap, miterLimit, mode, generateDebug) }

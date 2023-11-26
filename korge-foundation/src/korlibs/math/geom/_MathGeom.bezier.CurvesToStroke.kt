@file:Suppress("PackageDirectoryMismatch")

package korlibs.math.geom.bezier

import korlibs.datastructure.*
import korlibs.math.*
import korlibs.math.geom.*
import korlibs.math.geom.vector.*
import korlibs.math.interpolation.*
import kotlin.math.*

// @TODO
//private fun Curves.toStrokeCurves(join: LineJoin, startCap: LineCap, endCap: LineCap): Curves {
//    TODO()
//}

enum class StrokePointsMode {
    SCALABLE_POS_NORMAL_WIDTH,
    NON_SCALABLE_POS
}

/**
 * A generic stroke points with either [x, y] or [x, y, dx, dy, dist, distMax] components when having separate components,
 * it is possible to later scale the stroke without regenerating it by adjusting the [scale] component
 */
interface StrokePoints {
    val vector: DoubleVectorArrayList
    val debugPoints: PointList
    val debugSegments: List<MLine>
    val mode: StrokePointsMode

    fun scale(scale: Double) {
        if (mode == StrokePointsMode.SCALABLE_POS_NORMAL_WIDTH) {
            vector.fastForEachGeneric {
                this[it, 4] *= scale
                this[it, 5] *= scale
            }
        }
    }
}

class StrokePointsBuilder(
    val width: Double,
    override val mode: StrokePointsMode = StrokePointsMode.NON_SCALABLE_POS,
    val generateDebug: Boolean = false
) : StrokePoints {
    val NSTEPS = 20

    override val vector: DoubleVectorArrayList = DoubleVectorArrayList(dimensions = when (mode) {
        StrokePointsMode.SCALABLE_POS_NORMAL_WIDTH -> 6 // x, y, dx, dy, dist, distMax
        StrokePointsMode.NON_SCALABLE_POS -> 2 // x, y
    })

    override val debugPoints: PointArrayList = PointArrayList()
    override val debugSegments: ArrayList<MLine> = arrayListOf()

    override fun toString(): String = "StrokePointsBuilder($width, $vector)"

    fun addPoint(pos: Point, normal: Point, width: Double, maxWidth: Double = width) {
        //if (!pos.x.isFinite() || !normal.x.isFinite()) TODO("NaN detected pos=$pos, normal=$normal, width=$width, maxWidth=$maxWidth")
        when (mode) {
            StrokePointsMode.SCALABLE_POS_NORMAL_WIDTH -> vector.add(pos.x, pos.y, normal.x, normal.y, width, maxWidth.absoluteValue)
            StrokePointsMode.NON_SCALABLE_POS -> vector.add(pos.x + normal.x * width, pos.y + normal.y * width)
        }
    }

    fun addPointRelative(center: Point, pos: Point, sign: Double = 1.0) {
        val dist = pos - center
        val normal = if (sign < 0.0) -dist else dist
        //if (!center.x.isFinite() || !normal.x.isFinite()) TODO("Non finite value detected detected: center=$center, pos=$pos, sign=$sign, dist=$dist, normal=$normal")
        addPoint(center, normal.normalized, dist.length * sign)
    }

    fun addTwoPoints(pos: Point, normal: Point, width: Double) {
        addPoint(pos, normal, width)
        addPoint(pos, normal, -width)
    }

    fun addJoin(curr: Curve, next: Curve, kind: LineJoin, miterLimitRatio: Double) {
        val commonPoint = curr.calc(Ratio.ONE)
        val currTangent = curr.tangent(Ratio.ONE)
        val currNormal = curr.normal(Ratio.ONE)
        val nextTangent = next.tangent(Ratio.ZERO)
        val nextNormal = next.normal(Ratio.ZERO)

        val currLine0 = MLine.fromPointAndDirection(commonPoint + currNormal * width, currTangent)
        val currLine1 = MLine.fromPointAndDirection(commonPoint + currNormal * -width, currTangent)

        val nextLine0 = MLine.fromPointAndDirection(commonPoint + nextNormal * width, nextTangent)
        val nextLine1 = MLine.fromPointAndDirection(commonPoint + nextNormal * -width, nextTangent)

        //if (!nextLine1.dx.isFinite()) println((next as Bezier).roundDecimalPlaces(2))

        val intersection0 = MLine.lineIntersectionPoint(currLine0, nextLine0)
        val intersection1 = MLine.lineIntersectionPoint(currLine1, nextLine1)
        if (intersection0 == null || intersection1 == null) { // || !intersection0.x.isFinite() || !intersection1.x.isFinite()) {
            //println("currTangent=$currTangent, nextTangent=$nextTangent")
            //val signChanged = currTangent.x.sign != nextTangent.x.sign || currTangent.y.sign != nextTangent.y.sign
            //addTwoPoints(commonPoint, currNormal, if (signChanged) width else -width)
            addTwoPoints(commonPoint, currNormal, width)
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
            val p1 = if (direction <= 0.0) currLine0.projectedPoint(commonPoint) else nextLine1.projectedPoint(commonPoint)
            val p2 = if (direction <= 0.0) nextLine0.projectedPoint(commonPoint) else currLine1.projectedPoint(commonPoint)
            // @TODO: We should try to find the common edge (except when the two lines overlaps), to avoid overlapping in normal curves

            //println("direction=$direction")
            var p3: Point? = when {
                //angle.absoluteValue > 190.degrees -> null
                //else -> null
                direction <= 0.0 -> MLine.lineIntersectionPoint(currLine1, nextLine1)
                else -> MLine.lineIntersectionPoint(currLine0, nextLine0)
            }

            val p4Line = if (direction < 0.0) nextLine1 else nextLine0
            val p4 = p4Line.projectedPoint(commonPoint)
            //val p5 = Line.fromPointAndDirection(commonPoint, currTangent).getLineIntersectionPoint(p4Line)
            if (p3 == null) {
                p3 = p4
            }

            val angleB = (angle + 180.degrees).absoluteValue
            val angle2 = (angle umod 180.degrees).absoluteValue
            val angle3 = if (angle2 >= 90.degrees) 180.degrees - angle2 else angle2
            val ratio = (angle3.ratio.absoluteValue * 4.0).toRatioClamped()
            val p5 = ratio.toRatio().interpolate(p4, p3!!)

            //val p5 = p3

            if (generateDebug) {
                debugSegments.add(nextLine1.scalePoints(1000.0).clone())
                debugSegments.add(currLine1.scalePoints(1000.0).clone())
                debugSegments.add(nextLine0.scalePoints(1000.0).clone())
                debugSegments.add(currLine0.scalePoints(1000.0).clone())
                debugSegments.add(MLine.fromPointAndDirection(commonPoint, currTangent).scalePoints(1000.0).clone())
                debugSegments.add(MLine.fromPointAndDirection(commonPoint, nextTangent).scalePoints(1000.0).clone())
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
            //val p6 = p4

            if (direction < 0.0) {
                addPointRelative(commonPoint, p1)
                addPointRelative(commonPoint, p6, -1.0)
                addPointRelative(commonPoint, p2)
                addPointRelative(commonPoint, p6, -1.0)
            } else {
                addPointRelative(commonPoint, p6)
                addPointRelative(commonPoint, p2, -1.0)
                addPointRelative(commonPoint, p6)
                addPointRelative(commonPoint, p1, -1.0)
            }
            //addPoint(p1, Point(0, 0), 0.0)
            //addCurvePointsCap(p2, p1, 0.5)
            //addPoint(p2, Point(0, 0), 0.0)
            return
        }

        //if (false) {
        val d0 = intersection0 - commonPoint
        val d1 = commonPoint - intersection1

        addPoint(commonPoint, d0.normalized, d0.length.toDouble(), d0.length.absoluteValue.toDouble())
        addPoint(commonPoint, d1.normalized, -d1.length.toDouble(), d1.length.absoluteValue.toDouble())
    }

    fun addCap(curr: Curve, ratio: Ratio, kind: LineCap) {
        when (kind) {
            LineCap.SQUARE, LineCap.ROUND -> {
                val derivate = curr.normal(ratio).toNormal().let { if (ratio == Ratio.ONE) -it else it }
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
                        val a = if (ratio == Ratio.ZERO) p0 else p3
                        val b = if (ratio == Ratio.ZERO) p3 else p0
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

    fun addCurvePointsCap(p0: Point, p3: Point, ratio: Ratio, mid: Point = Point.middle(p0, p3), nsteps: Int = NSTEPS) {
        val angleStart = Angle.between(mid, p0)
        val angleEnd = Angle.between(mid, p3)

        if (ratio == Ratio.ONE) addTwoPoints(mid, Point.polar(angleEnd), width)
        val addAngle = if (Point.crossProduct(p0, p3) <= 0.0) Angle.ZERO else Angle.HALF
        Ratio.forEachRatio(nsteps, include0 = true, include1 = true) {
            val angle = it.interpolateAngleDenormalized(angleStart, angleEnd)
            val dir = Point.polar(angle + addAngle)
            addPoint(mid, dir, 0.0, width)
            addPoint(mid, dir, width, width)
        }
        if (ratio == Ratio.ZERO) addTwoPoints(mid, Point.polar(angleStart), width)
    }

    // @TODO: instead of nsteps we should have some kind of threshold regarding to how much information do we lose at 1:1 scale
    fun addCurvePoints(curr: Curve, nsteps: Int = (curr.length / 10.0).clamp(10.0, 100.0).toInt()) {
        // @TODO: Here we could generate curve information to render in the shader with a plain simple quadratic bezier to reduce the number of points and make the curve as accurate as possible
        Ratio.forEachRatio(nsteps, include0 = false, include1 = false) {
            addTwoPoints(curr.calc(it), curr.normal(it), width)
        }
    }

    fun addAllCurvesPoints(
        curves: Curves,
        join: LineJoin = LineJoin.MITER,
        startCap: LineCap = LineCap.BUTT,
        endCap: LineCap = LineCap.BUTT,
        miterLimit: Double = 10.0,
        forceClosed: Boolean? = null
    ) {
        val closed = forceClosed ?: curves.closed
        val curves = curves.beziers
        for (n in curves.indices) {
            val curr = curves.getCyclic(n + 0)
            val next = curves.getCyclic(n + 1)

            // Generate start cap
            if (n == 0) {
                if (closed) {
                    addJoin(curves.getCyclic(n - 1), curr, join, miterLimit)
                } else {
                    addCap(curr, Ratio.ZERO, startCap)
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
                    addCap(curr, Ratio.ONE, endCap)
                }
            }
        }
    }
}

fun VectorPath.toStrokePointsList(
    info: StrokeInfo,
    mode: StrokePointsMode = StrokePointsMode.NON_SCALABLE_POS,
    generateDebug: Boolean = false,
    forceClosed: Boolean? = null,
): List<StrokePoints> = toCurvesList().toStrokePointsList(info, mode, generateDebug, forceClosed)

fun Curves.toStrokePointsList(
    info: StrokeInfo,
    mode: StrokePointsMode = StrokePointsMode.NON_SCALABLE_POS,
    generateDebug: Boolean = false,
    forceClosed: Boolean? = null,
): List<StrokePoints> = listOf(this).toStrokePointsList(info, mode, generateDebug, forceClosed)

fun List<Curves>.toStrokePointsList(
    info: StrokeInfo,
    mode: StrokePointsMode = StrokePointsMode.NON_SCALABLE_POS,
    generateDebug: Boolean = false,
    forceClosed: Boolean? = null,
): List<StrokePoints> = toStrokePointsList(
    width = info.thickness,
    join = info.join,
    startCap = info.startCap,
    endCap = info.endCap,
    miterLimit = info.miterLimit,
    mode = mode,
    lineDash = info.dash,
    lineDashOffset = info.dashOffset,
    generateDebug = generateDebug,
    forceClosed = forceClosed
)

/** Useful for drawing */
fun Curves.toStrokePointsList(
    width: Double,
    join: LineJoin = LineJoin.MITER,
    startCap: LineCap = LineCap.BUTT,
    endCap: LineCap = LineCap.BUTT,
    miterLimit: Double = 10.0,
    mode: StrokePointsMode = StrokePointsMode.NON_SCALABLE_POS,
    lineDash: DoubleList? = null,
    lineDashOffset: Double = 0.0,
    generateDebug: Boolean = false
): List<StrokePoints> =
    listOf(this).toStrokePointsList(width, join, startCap, endCap, miterLimit, mode, lineDash, lineDashOffset, generateDebug)

fun List<Curves>.toStrokePointsList(
    width: Double,
    join: LineJoin = LineJoin.MITER,
    startCap: LineCap = LineCap.BUTT,
    endCap: LineCap = LineCap.BUTT,
    miterLimit: Double = 10.0,
    mode: StrokePointsMode = StrokePointsMode.NON_SCALABLE_POS,
    lineDash: DoubleList? = null,
    lineDashOffset: Double = 0.0,
    generateDebug: Boolean = false,
    forceClosed: Boolean? = null,
): List<StrokePoints> {
    val curvesList = when {
        lineDash != null -> this.flatMap { it.toDashes(lineDash.toDoubleArray(), lineDashOffset) }
        else -> this
    }
    return curvesList
        .map { curves ->
            StrokePointsBuilder(width / 2.0, mode, generateDebug).also {
                it.addAllCurvesPoints(curves, join, startCap, endCap, miterLimit, forceClosed)
            }
        }
}

/*

    private fun renderStroke(
        stateTransform: Matrix,
        strokePath: VectorPath,
        paint: Paint,
        globalAlpha: Double,
        lineWidth: Double,
        scaleMode: LineScaleMode,
        startCap: LineCap,
        endCap: LineCap,
        join: LineJoin,
        miterLimit: Double,
        forceClosed: Boolean? = null,
        stencil: AG.StencilState? = null
    ) {
        //val m0 = root.globalMatrix
        //val mt0 = m0.toTransform()
        //val m = globalMatrix
        //val mt = m.toTransform()

        val scaleWidth = scaleMode.anyScale
        //val lineScale = mt.scaleAvg.absoluteValue

        val lineScale = 1.0
        //println("lineScale=$lineScale")
        val flineWidth = if (scaleWidth) lineWidth * lineScale else lineWidth
        val lineWidth = if (antialiased) (flineWidth * 0.5) + 0.25 else flineWidth * 0.5

        //val lineWidth = 0.2
        //val lineWidth = 20.0
        val fLineWidth = max((lineWidth).toFloat(), 1.5f)

        //val cacheKey = StrokeRenderCacheKey(lineWidth, strokePath, m)

        //val data = strokeCache.getOrPut(cacheKey) {
        val data = run {
            //val pathList = strokePath.toPathPointList(m, emitClosePoint = false)
            val pathList = strokePath.toPathPointList(null, emitClosePoint = false)
            //println(pathList.size)
            for (ppath in pathList) {
                gpuShapeViewCommands.verticesStart()
                val loop = forceClosed ?: ppath.closed
                //println("Points: " + ppath.toList())
                val end = if (loop) ppath.size + 1 else ppath.size
                //val end = if (loop) ppath.size else ppath.size

                for (n in 0 until end) pointsScope {
                    val isFirst = n == 0
                    val isLast = n == ppath.size - 1
                    val isFirstOrLast = isFirst || isLast
                    val a = ppath.getCyclic(n - 1)
                    val b = ppath.getCyclic(n) // Current point
                    val c = ppath.getCyclic(n + 1)
                    val orientation = Point.orientation(a, b, c).sign.toInt()
                    //val angle = Angle.between(b - a, c - a)
                    //println("angle = $angle")

                    ab.setTo(a, b, lineWidth, this)
                    bc.setTo(b, c, lineWidth, this)

                    when {
                        // Start/End caps
                        !loop && isFirstOrLast -> {
                            val start = n == 0

                            val cap = if (start) startCap else endCap
                            val index = if (start) 0 else 1
                            val segment = if (start) bc else ab
                            val p1 = segment.p1(index)
                            val p0 = segment.p0(index)
                            val iangle = if (start) segment.angleSE - 180.degrees else segment.angleSE

                            when (cap) {
                                LineCap.BUTT -> pointsAdd(p1, p0, fLineWidth)
                                LineCap.SQUARE -> {
                                    val p1s = Point(p1, iangle, lineWidth)
                                    val p0s = Point(p0, iangle, lineWidth)
                                    pointsAdd(p1s, p0s, fLineWidth)
                                }
                                LineCap.ROUND -> {
                                    val p1s = Point(p1, iangle, lineWidth * 1.5)
                                    val p0s = Point(p0, iangle, lineWidth * 1.5)
                                    pointsAddCubicOrLine(
                                        this, p0, p0, p0s, p1s, p1, lineWidth,
                                        reverse = false,
                                        start = start
                                    )
                                }
                            }
                        }
                        // Joins
                        else -> {
                            val m0 = Line.getIntersectXY(ab.s0, ab.e0, bc.s0, bc.e0, Point.ZERO) // Outer (CW)
                            val m1 = Line.getIntersectXY(ab.s1, ab.e1, bc.s1, bc.e1, Point.ZERO) // Inner (CW)
                            val e1 = m1 ?: ab.e1
                            val e0 = m0 ?: ab.e0
                            val round = join == LineJoin.ROUND
                            val dorientation = when {
                                (join == LineJoin.MITER && e1.distanceTo(b) <= (miterLimit * lineWidth)) -> 0
                                else -> orientation
                            }

                            if (loop && isFirst) {
                                //println("forientation=$forientation")
                                when (dorientation) {
                                    -1 -> pointsAdd(e1, bc.s0, fLineWidth)
                                    0 -> pointsAdd(e1, e0, fLineWidth)
                                    +1 -> pointsAdd(bc.s1, e0, fLineWidth)
                                }
                            } else {
                                //println("dorientation=$dorientation")
                                when (dorientation) {
                                    // Turn right
                                    -1 -> {
                                        val fp = m1 ?: ab.e1
                                        //points.addCubicOrLine(this, true, fp, p0, p0, p1, p1, lineWidth, cubic = false)
                                        if (round) {
                                            pointsAddCubicOrLine(
                                                this, fp,
                                                ab.e0, ab.e0s, bc.s0s, bc.s0,
                                                lineWidth, reverse = true
                                            )
                                        } else {
                                            pointsAdd(fp, ab.e0, fLineWidth)
                                            pointsAdd(fp, bc.s0, fLineWidth)
                                        }
                                    }
                                    // Miter
                                    0 -> pointsAdd(e1, e0, fLineWidth)
                                    // Turn left
                                    1 -> {
                                        val fp = m0 ?: ab.e0
                                        if (round) {
                                            pointsAddCubicOrLine(
                                                this, fp,
                                                ab.e1, ab.e1s, bc.s1s, bc.s1,
                                                lineWidth, reverse = false
                                            )
                                        } else {
                                            pointsAdd(ab.e1, fp, fLineWidth)
                                            pointsAdd(bc.s1, fp, fLineWidth)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                val info = GpuShapeViewPrograms.paintToShaderInfo(
                    stateTransform = stateTransform,
                    paint = paint,
                    globalAlpha = globalAlpha,
                    lineWidth = lineWidth,
                )

                gpuShapeViewCommands.setScissor(null)
                gpuShapeViewCommands.draw(AGDrawType.TRIANGLE_STRIP, info, stencil = stencil)
            }
        }

        //println("vertexCount=$vertexCount")
    }
    //private var lastPointsString = ""

 */

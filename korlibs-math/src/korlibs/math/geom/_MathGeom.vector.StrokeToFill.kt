@file:Suppress("PackageDirectoryMismatch")

package korlibs.math.geom.vector

import korlibs.datastructure.*
import korlibs.datastructure.iterators.*
import korlibs.math.*
import korlibs.math.annotations.*
import korlibs.math.geom.*
import korlibs.math.geom.bezier.*
import korlibs.math.geom.shape.*
import korlibs.math.interpolation.*

// @TODO: Implement LineCap + LineJoin
// @TODO: Use Curves and reuse code from [CurvesToStrokes]
/**
 * @TODO: Use [korlibs.math.geom.bezier.Curves] and reuse code from [toStrokePointLi]
 */
@OptIn(KormaExperimental::class)
class StrokeToFill {
    private var weight: Int = 1
    private lateinit var outFill: VectorPath
    private var startCap: LineCap = LineCap.BUTT
    private var endCap: LineCap = LineCap.BUTT
    private var joins: LineJoin = LineJoin.BEVEL
    private var miterLimit: Double = 4.0 // ratio of the width
    internal val strokePoints = PointIntArrayList(1024)
    internal val doJointList = IntArrayList(1024)
    internal val fillPoints = Array(2) { PointIntArrayList(1024) }
    internal val fillPointsLeft = fillPoints[0]
    internal val fillPointsRight = fillPoints[1]

    private val prevEdge = MEdge()
    private val prevEdgeLeft = MEdge()
    private val prevEdgeRight = MEdge()

    private val currEdge = MEdge()
    private val currEdgeLeft = MEdge()
    private val currEdgeRight = MEdge()

    internal fun MEdge.setEdgeDisplaced(edge: MEdge, width: Int, angle: Angle): MEdge = this.apply {
        val ldx = (width * angle.cosine)
        val ldy = (width * angle.sine)
        this.setTo((edge.ax + ldx).toInt(), (edge.ay + ldy).toInt(), (edge.bx + ldx).toInt(), (edge.by + ldy).toInt(), edge.wind)
    }

    internal enum class EdgePoint(val n: Int) { A(0), B(1) }

    internal fun PointIntArrayList.addEdgePointA(e: MEdge) = add(e.ax, e.ay)
    internal fun PointIntArrayList.addEdgePointB(e: MEdge) = add(e.bx, e.by)
    internal fun PointIntArrayList.addEdgePointAB(e: MEdge, point: EdgePoint) = if (point == EdgePoint.A) addEdgePointA(e) else addEdgePointB(e)
    internal fun PointIntArrayList.add(e: Point) { add(e.x.toInt(), e.y.toInt()) }

    internal fun doJoin(out: PointIntArrayList, mainPrev: MEdge, mainCurr: MEdge, prev: MEdge, curr: MEdge, join: LineJoin, miterLimit: Double, scale: Double, forcedMiter: Boolean) {
        val rjoin = if (forcedMiter) LineJoin.MITER else join
        when (rjoin) {
            LineJoin.MITER -> {
                val intersection2 = Point(mainPrev.bx, mainPrev.by)
                val intersection = MEdge.getIntersectXY(prev, curr)
                if (intersection != null) {
                    val dist = Point.distance(intersection, intersection2)
                    if (forcedMiter || dist <= miterLimit) {
                        out.add(intersection.toInt())
                    } else {
                        out.addEdgePointB(prev)
                        out.addEdgePointA(curr)
                    }
                }
            }
            LineJoin.BEVEL -> {
                out.addEdgePointB(prev)
                out.addEdgePointA(curr)
            }
            LineJoin.ROUND -> {
                val i = MEdge.getIntersectXY(prev, curr)
                if (i != null) {
                    val count = (Point.distance(prev.bx, prev.by, curr.ax, curr.ay) * scale).toInt().clamp(4, 64)
                    for (n in 0..count) {
                        out.add(Bezier.quadCalc(
                            Point(prev.bx.toDouble(), prev.by.toDouble()),
                            Point(i.x, i.y),
                            Point(curr.ax.toDouble(), curr.ay.toDouble()),
                            Ratio(n.toDouble() / count)
                        ))
                    }
                } else {
                    out.addEdgePointB(prev)
                    out.addEdgePointA(curr)
                }
            }
        }
    }

    internal fun doCap(l: PointIntArrayList, r: PointIntArrayList, left: MEdge, right: MEdge, epoint: EdgePoint, cap: LineCap, scale: Double) {
        val angle = if (epoint == EdgePoint.A) -left.angle else +left.angle
        val lx = left.getX(epoint.n)
        val ly = left.getY(epoint.n)
        val rx = right.getX(epoint.n)
        val ry = right.getY(epoint.n)
        when (cap) {
            LineCap.BUTT -> {
                l.add(lx, ly)
                r.add(rx, ry)
            }
            LineCap.ROUND, LineCap.SQUARE -> {
                val ax = (angle.cosine * weight / 2).toInt()
                val ay = (angle.sine * weight / 2).toInt()
                val lx2 = lx + ax
                val ly2 = ly + ay
                val rx2 = rx + ax
                val ry2 = ry + ay
                if (cap == LineCap.SQUARE) {
                    l.add(lx2, ly2)
                    r.add(rx2, ry2)
                } else {
                    val count = (MPoint.distance(lx, ly, rx, ry) * scale).toInt().clamp(4, 64)
                    l.add(lx, ly)
                    for (n in 0 .. count) {
                        val m = if (epoint == EdgePoint.A) n else count - n
                        val ratio = Ratio(m.toDouble() / count)
                        r.add(
                            Bezier.cubicCalc(
                                Point(lx.toDouble(), ly.toDouble()),
                                Point(lx2.toDouble(), ly2.toDouble()),
                                Point(rx2.toDouble(), ry2.toDouble()),
                                Point(rx.toDouble(), ry.toDouble()),
                                ratio
                            )
                        )
                    }
                }
            }
        }
    }

    internal fun computeStroke(scale: Double, closed: Boolean) {
        if (strokePoints.isEmpty()) return

        val weightD2 = weight / 2
        fillPointsLeft.clear()
        fillPointsRight.clear()
        val sp = strokePoints
        val nstrokePoints = sp.size

        for (n in 0 until nstrokePoints) {
            val isFirst = n == 0
            val isLast = n == nstrokePoints - 1
            val isMiddle = !isFirst && (!isLast || closed)
            val n1 = when {
                isLast -> if (closed) 1 else n
                else -> n + 1
            }

            prevEdge.copyFrom(currEdge)
            prevEdgeLeft.copyFrom(currEdgeLeft)
            prevEdgeRight.copyFrom(currEdgeRight)

            val doJoin = doJointList.getAt(n) != 0
            currEdge.setTo(sp.getX(n), sp.getY(n), sp.getX(n1), sp.getY(n1), +1)
            currEdgeLeft.setEdgeDisplaced(currEdge, weightD2, currEdge.angle - 90.degrees)
            currEdgeRight.setEdgeDisplaced(currEdge, weightD2, currEdge.angle + 90.degrees)

            when {
                isFirst -> {
                    doCap(fillPointsLeft, fillPointsRight, currEdgeLeft, currEdgeRight, EdgePoint.A, if (closed) LineCap.BUTT else startCap, scale)
                }
                isMiddle -> {
                    val angle = MEdge.angleBetween(prevEdge, currEdge)
                    //val leftAngle = !(angle > 0.degrees && angle < 180.degrees)
                    val leftAngle = angle > 0.degrees

                    if (doJoin) {
                        doJoin(fillPointsLeft, prevEdge, currEdge, prevEdgeLeft, currEdgeLeft, joins, miterLimit, scale, leftAngle)
                        doJoin(fillPointsRight, prevEdge, currEdge, prevEdgeRight, currEdgeRight, joins, miterLimit, scale, !leftAngle)
                    } else {
                        fillPointsLeft.addEdgePointA(currEdgeLeft)
                        fillPointsRight.addEdgePointA(currEdgeRight)
                    }
                }
                isLast -> {
                    if (closed) {
                        doCap(fillPointsLeft, fillPointsRight, currEdgeLeft, currEdgeRight, EdgePoint.B, LineCap.BUTT, scale)
                    } else {
                        doCap(fillPointsLeft, fillPointsRight, prevEdgeLeft, prevEdgeRight, EdgePoint.B, endCap, scale)
                    }
                }
            }
        }

        for (n in 0 until fillPointsLeft.size) {
            val x = fillPointsLeft.getX(n)
            val y = fillPointsLeft.getY(n)
            if (n == 0) {
                outFill.moveTo(Point(x * scale, y * scale))
            } else {
                outFill.lineTo(Point(x * scale, y * scale))
            }
        }
        // Draw the rest of the points
        for (n in 0 until fillPointsRight.size) {
            val m = fillPointsRight.size - n - 1
            outFill.lineTo(Point(fillPointsRight.getX(m) * scale, fillPointsRight.getY(m) * scale))
        }
        outFill.close()
        outFill.winding = Winding.NON_ZERO
        //outFill.winding = Winding.EVEN_ODD
        strokePoints.clear()
        doJointList.clear()
    }


    fun set(outFill: VectorPath, weight: Int, startCap: LineCap, endCap: LineCap, joins: LineJoin, miterLimit: Double) {
        this.outFill = outFill
        this.weight = weight
        this.startCap = startCap
        this.endCap = endCap
        this.joins = joins
        this.miterLimit = miterLimit * weight
    }

    fun strokeFill(
        stroke: VectorPath,
        lineWidth: Double, joins: LineJoin, startCap: LineCap, endCap: LineCap, miterLimit: Double, outFill: VectorPath
    ) {
        val scale = RastScale.RAST_FIXED_SCALE
        val iscale = 1.0 / RastScale.RAST_FIXED_SCALE
        set(outFill, (lineWidth * scale).toInt(), startCap, endCap, joins, miterLimit)
        stroke.emitPoints2(
            flush = { close ->
                if (close) computeStroke(iscale, true)
            },
            joint = {
                doJointList[doJointList.size - 1] = 1
            }
        ) { p, move ->
            if (move) computeStroke(iscale, false)
            strokePoints.add((p.x * scale).toInt(), (p.y * scale).toInt())
            doJointList.add(0)
        }
        computeStroke(iscale, false)
    }
}

fun VectorPath.strokeToFill(
    info: StrokeInfo,
    temp: StrokeToFill = StrokeToFill(),
    outFill: VectorPath = VectorPath(winding = Winding.NON_ZERO),
): VectorPath = strokeToFill(
    info.thickness,
    info.join,
    info.startCap,
    info.endCap,
    info.miterLimit,
    info.dash,
    info.dashOffset,
    temp, outFill
)

fun VectorPath.strokeToFill(
    lineWidth: Double,
    joins: LineJoin = LineJoin.MITER,
    startCap: LineCap = LineCap.BUTT,
    endCap: LineCap = startCap,
    miterLimit: Double = 4.0,
    lineDash: DoubleList? = null,
    lineDashOffset: Double = 0.0,
    temp: StrokeToFill = StrokeToFill(),
    outFill: VectorPath = VectorPath(winding = Winding.NON_ZERO),
): VectorPath {
    val strokePaths = when {
        lineDash != null -> this.toCurvesList()
            .flatMap { it.toDashes(lineDash.toDoubleArray(), lineDashOffset) }
            .map { it.toVectorPath() }
        else -> listOf(this)
    }
    strokePaths.fastForEach { strokePath ->
        temp.strokeFill(
            strokePath, lineWidth, joins, startCap, endCap, miterLimit, outFill
        )
    }
    return outFill
}

package com.soywiz.korma.geom.vector

import com.soywiz.kds.*
import com.soywiz.korma.annotations.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.bezier.*
import com.soywiz.korma.geom.shape.*
import com.soywiz.korma.math.*

// @TODO: Implement LineCap + LineJoin
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

    private val prevEdge = Edge()
    private val prevEdgeLeft = Edge()
    private val prevEdgeRight = Edge()

    private val currEdge = Edge()
    private val currEdgeLeft = Edge()
    private val currEdgeRight = Edge()

    internal fun Edge.setEdgeDisplaced(edge: Edge, width: Int, angle: Angle) = this.apply {
        val ldx = (width * angle.cosine)
        val ldy = (width * angle.sine)
        this.setTo((edge.ax + ldx).toInt(), (edge.ay + ldy).toInt(), (edge.bx + ldx).toInt(), (edge.by + ldy).toInt(), edge.wind)
    }

    internal enum class EdgePoint(val n: Int) { A(0), B(1) }

    internal fun PointIntArrayList.addEdgePointA(e: Edge) = add(e.ax, e.ay)
    internal fun PointIntArrayList.addEdgePointB(e: Edge) = add(e.bx, e.by)
    internal fun PointIntArrayList.addEdgePointAB(e: Edge, point: EdgePoint) = if (point == EdgePoint.A) addEdgePointA(e) else addEdgePointB(e)
    internal fun PointIntArrayList.add(e: Point?) = run { if (e != null) add(e.x.toInt(), e.y.toInt()) }
    internal fun PointIntArrayList.add(x: Double, y: Double) = run { add(x.toInt(), y.toInt()) }

    private val tempP1 = Point()
    private val tempP2 = Point()
    private val tempP3 = Point()

    internal fun doJoin(out: PointIntArrayList, mainPrev: Edge, mainCurr: Edge, prev: Edge, curr: Edge, join: LineJoin, miterLimit: Double, scale: Double, forcedMiter: Boolean) {
        val rjoin = if (forcedMiter) LineJoin.MITER else join
        when (rjoin) {
            LineJoin.MITER -> {
                val intersection2 = tempP1.setTo(mainPrev.bx, mainPrev.by)
                val intersection = Edge.getIntersectXY(prev, curr, tempP3)
                if (intersection != null) {
                    val dist = Point.distance(intersection, intersection2)
                    if (forcedMiter || dist <= miterLimit) {
                        out.add(intersection)
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
                val i = Edge.getIntersectXY(prev, curr, tempP3)
                if (i != null) {
                    val count = (Point.distance(prev.bx, prev.by, curr.ax, curr.ay) * scale).toInt().clamp(4, 64)
                    for (n in 0..count) {
                        out.add(Bezier.quadCalc(prev.bx.toDouble(), prev.by.toDouble(), i.x, i.y, curr.ax.toDouble(), curr.ay.toDouble(), n.toDouble() / count, tempP2))
                    }
                } else {
                    out.addEdgePointB(prev)
                    out.addEdgePointA(curr)
                }
            }
        }
    }

    internal fun doCap(l: PointIntArrayList, r: PointIntArrayList, left: Edge, right: Edge, epoint: EdgePoint, cap: LineCap, scale: Double) {
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
                    val count = (Point.distance(lx, ly, rx, ry) * scale).toInt().clamp(4, 64)
                    l.add(lx, ly)
                    for (n in 0 .. count) {
                        val m = if (epoint == EdgePoint.A) n else count - n
                        val ratio = m.toDouble() / count
                        r.add(
                            Bezier.cubicCalc(
                            lx.toDouble(), ly.toDouble(),
                            lx2.toDouble(), ly2.toDouble(),
                            rx2.toDouble(), ry2.toDouble(),
                            rx.toDouble(), ry.toDouble(),
                            ratio,
                            tempP2
                        ))
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
                    val angle = Edge.angleBetween(prevEdge, currEdge)
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
                outFill.moveTo(x * scale, y * scale)
            } else {
                outFill.lineTo(x * scale, y * scale)
            }
        }
        // Draw the rest of the points
        for (n in 0 until fillPointsRight.size) {
            val m = fillPointsRight.size - n - 1
            outFill.lineTo(fillPointsRight.getX(m) * scale, fillPointsRight.getY(m) * scale)
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
        ) { x, y, move ->
            if (move) computeStroke(iscale, false)
            strokePoints.add((x * scale).toInt(), (y * scale).toInt())
            doJointList.add(0)
        }
        computeStroke(iscale, false)
    }
}

fun VectorPath.strokeToFill(
    lineWidth: Double,
    joins: LineJoin = LineJoin.MITER,
    startCap: LineCap = LineCap.BUTT,
    endCap: LineCap = startCap,
    miterLimit: Double = 4.0,
    temp: StrokeToFill = StrokeToFill(),
    outFill: VectorPath = VectorPath(winding = Winding.NON_ZERO)
): VectorPath {
    temp.strokeFill(
        this@strokeToFill, lineWidth, joins, startCap, endCap, miterLimit, outFill
    )
    return outFill
}

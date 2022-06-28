package com.soywiz.korma.geom.vector

import com.soywiz.kds.DoubleArrayList
import com.soywiz.kds.Extra
import com.soywiz.kds.IntArrayList
import com.soywiz.kds.extraProperty
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.korma.annotations.KormaExperimental
import com.soywiz.korma.geom.BoundsBuilder
import com.soywiz.korma.geom.IPoint
import com.soywiz.korma.geom.Line
import com.soywiz.korma.geom.LineIntersection
import com.soywiz.korma.geom.Matrix
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.PointArrayList
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.bezier.Bezier
import com.soywiz.korma.geom.bezier.Curves
import com.soywiz.korma.geom.bezier.toCurves
import com.soywiz.korma.internal.niceStr
import com.soywiz.korma.math.isAlmostEquals
import kotlin.native.concurrent.ThreadLocal

// @TODO: ThreadLocal on JVM
@ThreadLocal
private val tempMatrix: Matrix = Matrix()

interface IVectorPath : VectorBuilder {
    fun toSvgString(): String
}

@OptIn(KormaExperimental::class)
class VectorPath(
    val commands: IntArrayList = IntArrayList(),
    val data: DoubleArrayList = DoubleArrayList(),
    var winding: Winding = Winding.DEFAULT,
) : IVectorPath, Extra by Extra.Mixin() {
    var assumeConvex: Boolean = false
    var version: Int = 0

    fun clone(): VectorPath = VectorPath(IntArrayList(commands), DoubleArrayList(data), winding)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is VectorPath && this.commands == other.commands && this.data == other.data && this.winding == other.winding
    }
    override fun hashCode(): Int = commands.hashCode() + (data.hashCode() * 13) + (winding.ordinal * 111)

    companion object {
        private val identityMatrix = Matrix()

        inline operator fun invoke(winding: Winding = Winding.DEFAULT, callback: VectorPath.() -> Unit): VectorPath = VectorPath(winding = winding).apply(callback)

        fun intersects(left: VectorPath, leftTransform: Matrix, right: VectorPath, rightTransform: Matrix): Boolean =
            left.intersectsWith(leftTransform, right, rightTransform)

        fun intersects(left: VectorPath, right: VectorPath): Boolean = left.intersectsWith(right)
    }

    interface Visitor {
        fun close()
        fun moveTo(x: Double, y: Double)
        fun lineTo(x: Double, y: Double)
        fun quadTo(cx: Double, cy: Double, ax: Double, ay: Double)
        fun cubicTo(cx1: Double, cy1: Double, cx2: Double, cy2: Double, ax: Double, ay: Double)
    }

    inline fun visitCmds(
        moveTo: (x: Double, y: Double) -> Unit,
        lineTo: (x: Double, y: Double) -> Unit,
        quadTo: (x1: Double, y1: Double, x2: Double, y2: Double) -> Unit,
        cubicTo: (x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double) -> Unit,
        close: () -> Unit
    ) {
        var n = 0
        commands.fastForEach { cmd ->
            when (cmd) {
                Command.MOVE_TO -> moveTo(data[n++], data[n++])
                Command.LINE_TO -> lineTo(data[n++], data[n++])
                Command.QUAD_TO -> quadTo(data[n++], data[n++], data[n++], data[n++])
                Command.CUBIC_TO -> cubicTo(data[n++], data[n++], data[n++], data[n++], data[n++], data[n++])
                Command.CLOSE -> close()
            }
        }
    }

    inline fun visitEdges(
        line: (x0: Double, y0: Double, x1: Double, y1: Double) -> Unit,
        quad: (x0: Double, y0: Double, x1: Double, y1: Double, x2: Double, y2: Double) -> Unit,
        cubic: (x0: Double, y0: Double, x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double) -> Unit,
        close: () -> Unit = {},
        move: (x: Double, y: Double) -> Unit = { x, y -> },
        dummy: Unit = Unit // Prevents tailing lambda
    ) {
        var mx = 0.0
        var my = 0.0
        var lx = 0.0
        var ly = 0.0
        visitCmds(
            moveTo = { x, y ->
                mx = x; my = y
                lx = x; ly = y
                move(x, y)
            },
            lineTo = { x, y ->
                line(lx, ly, x, y)
                lx = x; ly = y
            },
            quadTo = { x1, y1, x2, y2 ->
                quad(lx, ly, x1, y1, x2, y2)
                lx = x2; ly = y2
            },
            cubicTo = { x1, y1, x2, y2, x3, y3 ->
                cubic(lx, ly, x1, y1, x2, y2, x3, y3)
                lx = x3; ly = y3
            },
            close = {
                if (!lx.isAlmostEquals(mx) || !ly.isAlmostEquals(my)) {
                    line(lx, ly, mx, my)
                }
                close()
            }
        )
    }

    fun getAllLines(): List<Line> = scanline.getAllLines()

    inline fun visitEdgesSimple(
        line: (x0: Double, y0: Double, x1: Double, y1: Double) -> Unit,
        cubic: (x0: Double, y0: Double, x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double) -> Unit,
        close: () -> Unit
    ) = visitEdges(
        line,
        { x0, y0, x1, y1, x2, y2 ->
            //val cx1 = Bezier.quadToCubic1(x0, x1, x2)
            //val cy1 = Bezier.quadToCubic1(y0, y1, y2)
            //val cx2 = Bezier.quadToCubic2(x0, x1, x2)
            //val cy2 = Bezier.quadToCubic2(y0, y1, y2)
            //cubic(x0, y0, cx1, cy1, cx2, cy2, x2, y2)
            Bezier.quadToCubic(x0, y0, x1, y1, x2, y2) { qx0, qy0, qx1, qy1, qx2, qy2, qx3, qy3 ->
                cubic(qx0, qy0, qx1, qy1, qx2, qy2, qx3, qy3)
            }
        },
        cubic,
        close,
    )

    fun visit(visitor: Visitor) {
        visitCmds(
            moveTo = visitor::moveTo,
            lineTo = visitor::lineTo,
            quadTo = visitor::quadTo,
            cubicTo = visitor::cubicTo,
            close = visitor::close
        )
    }

    fun clear() {
        commands.clear()
        data.clear()
        lastXY(0.0, 0.0)
        version = 0
        scanline.version = version - 1  // ensure scanline will be updated after this "clear" operation
    }

    fun setFrom(other: VectorPath) {
        clear()
        appendFrom(other)
    }

    fun appendFrom(other: VectorPath) {
        this.commands.add(other.commands)
        this.data.add(other.data)
        lastXY(other.lastX, other.lastY)
        version++
    }

    override var lastX = 0.0
    override var lastY = 0.0

    fun lastXY(x: Double, y: Double) {
        this.lastX = x
        this.lastY = y
    }

    override fun moveTo(x: Double, y: Double) {
        if (commands.isNotEmpty() && commands.last() == Command.MOVE_TO) {
            if (lastX == x && lastY == y) return
        }
        commands.add(Command.MOVE_TO)
        data.add(x, y)
        lastXY(x, y)
        version++
    }

    override fun lineTo(x: Double, y: Double) {
        if (ensureMoveTo(x, y)) return
        if (x == lastX && y == lastY) {
            return
        }
        commands.add(Command.LINE_TO)
        data.add(x, y)
        lastXY(x, y)
        version++
    }

    val isLastCommandClose: Boolean get() = if (commands.isEmpty()) false else commands.last() == Command.CLOSE

    fun removeLastCommand() {
        if (commands.isEmpty()) return
        val command = commands.removeAt(commands.size - 1)
        val commandParams = Command.getParamCount(command)
        data.removeAt(data.size - commandParams, commandParams)
    }

    fun optimizeLastCommand() {
        if (commands.size < 3) return
        val commandC = commands[commands.size - 3]
        val commandA = commands[commands.size - 2]
        val commandB = commands[commands.size - 1]
        if (commandA == Command.LINE_TO && commandB == Command.LINE_TO && commandC != Command.CLOSE) {
            val x0 = data[data.size - 6]
            val y0 = data[data.size - 5]
            val x = data[data.size - 4]
            val y = data[data.size - 3]
            val x1 = data[data.size - 2]
            val y1 = data[data.size - 1]
            if (Point.isCollinear(x0, y0, x, y, x1, y1)) {
                removeLastCommand()
                data[data.size - 2] = x1
                data[data.size - 1] = y1
            }
        }
    }

    override fun quadTo(cx: Double, cy: Double, ax: Double, ay: Double) {
        ensureMoveTo(cx, cy)
        commands.add(Command.QUAD_TO)
        data.add(cx, cy, ax, ay)
        lastXY(ax, ay)
        version++
    }

    override fun cubicTo(cx1: Double, cy1: Double, cx2: Double, cy2: Double, ax: Double, ay: Double) {
        ensureMoveTo(cx1, cy1)
        commands.add(Command.CUBIC_TO)
        data.add(cx1, cy1, cx2, cy2, ax, ay)
        lastXY(ax, ay)
        version++
    }

    override fun close() {
        commands.add(Command.CLOSE)
        version++
    }

    override val totalPoints: Int get() = data.size / 2

    private fun ensureMoveTo(x: Double, y: Double): Boolean {
        if (isNotEmpty()) return false
        moveTo(x, y)
        return true
    }

    fun getBounds(out: Rectangle = Rectangle(), bb: BoundsBuilder = BoundsBuilder()): Rectangle {
        bb.reset()
        bb.add(this)
        return bb.getBounds(out)
    }

    @Deprecated("Use toPathList that aproximates curves")
    fun getPoints(): List<IPoint> = getPointList().toList()

    @Deprecated("Use toPathList that aproximates curves")
    fun getPointList(): PointArrayList {
        val points = PointArrayList()
        this.visitCmds(
            moveTo = { x, y -> points.add(x, y) },
            lineTo = { x, y -> points.add(x, y) },
            quadTo = { x1, y1, x2, y2 -> points.add(x2, y2) },
            cubicTo = { x1, y1, x2, y2, x3, y3 -> points.add(x3, y3) },
            close = { }
        )
        return points
    }

    // http://erich.realtimerendering.com/ptinpoly/
    // http://stackoverflow.com/questions/217578/how-can-i-determine-whether-a-2d-point-is-within-a-polygon/2922778#2922778
    // https://www.particleincell.com/2013/cubic-line-intersection/
    // I run a semi-infinite ray horizontally (increasing x, fixed y) out from the test point, and count how many edges it crosses.
    // At each crossing, the ray switches between inside and outside. This is called the Jordan curve theorem.
    fun containsPoint(p: Point): Boolean = containsPoint(p.x, p.y, this.winding)
    fun containsPoint(x: Double, y: Double): Boolean = containsPoint(x, y, this.winding)
    fun containsPoint(x: Int, y: Int): Boolean = containsPoint(x.toDouble(), y.toDouble())
    fun containsPoint(x: Float, y: Float): Boolean = containsPoint(x.toDouble(), y.toDouble())

    private var _scanline: PolygonScanline? = null

    @KormaExperimental
    val scanline: PolygonScanline get() {
        if (_scanline == null) _scanline = PolygonScanline()
        val scanline = _scanline!!

        if (scanline.version != this.version) {
            scanline.reset()
            scanline.add(this)
            scanline.version = this.version
        }

        return _scanline!!
    }
    fun containsPoint(x: Double, y: Double, winding: Winding): Boolean = scanline.containsPoint(x, y, winding)
    fun containsPoint(x: Int, y: Int, winding: Winding): Boolean = containsPoint(x.toDouble(), y.toDouble(), winding)
    fun containsPoint(x: Float, y: Float, winding: Winding): Boolean = containsPoint(x.toDouble(), y.toDouble(), winding)

    fun intersectsWith(right: VectorPath): Boolean = intersectsWith(identityMatrix, right, identityMatrix)

    fun intersectsWith(leftMatrix: Matrix, right: VectorPath, rightMatrix: Matrix): Boolean {
        val left = this
        val leftScanline = left.scanline
        val rightScanline = right.scanline

        tempMatrix.invert(rightMatrix)
        tempMatrix.premultiply(leftMatrix)

        leftScanline.forEachPoint { x, y ->
            val tx = tempMatrix.transformX(x, y)
            val ty = tempMatrix.transformY(x, y)
            //println("LEFT: $tx, $ty")
            if (rightScanline.containsPoint(tx, ty)) return true
        }

        tempMatrix.invert(leftMatrix)
        tempMatrix.premultiply(rightMatrix)

        rightScanline.forEachPoint { x, y ->
            val tx = tempMatrix.transformX(x, y)
            val ty = tempMatrix.transformY(x, y)
            if (leftScanline.containsPoint(tx, ty)) return true
        }
        return false
    }

    fun getLineIntersection(line: Line, out: LineIntersection = LineIntersection()): LineIntersection? {
        // Directs from outside the shape, to inside the shape
//        if (this.containsPoint(line.b) && !this.containsPoint(line.a)) {
            return this.scanline.getLineIntersection(line, out)
        //}
        //return null
    }

    //private val p1 = Point()
    //private val p2 = Point()
    //fun numberOfIntersections(x: Double, y: Double): Int {
    //    val testx = x
    //    val testy = y
    //    var intersections = 0
    //    visitEdges(
    //        line = { x0, y0, x1, y1 -> intersections += HorizontalLine.intersectionsWithLine(testx, testy, x0, y0, x1, y1) },
    //        quad = { x0, y0, x1, y1, x2, y2 -> intersections += HorizontalLine.interesectionsWithQuadBezier(testx, testy, x0, y0, x1, y1, x2, y2, p1, p2) },
    //        cubic = { x0, y0, x1, y1, x2, y2, x3, y3 -> intersections += HorizontalLine.intersectionsWithCubicBezier(testx, testy, x0, y0, x1, y1, x2, y2, x3, y3, p1, p2) },
    //        close = {}
    //    )
    //    return intersections
    //}
    //fun numberOfIntersections(x: Float, y: Float): Int = numberOfIntersections(x.toDouble(), y.toDouble())
    //fun numberOfIntersections(x: Int, y: Int): Int = numberOfIntersections(x.toDouble(), y.toDouble())

    class Stats {
        val stats = IntArray(5)
        val moveTo get() = stats[Command.MOVE_TO]
        val lineTo get() = stats[Command.LINE_TO]
        val quadTo get() = stats[Command.QUAD_TO]
        val cubicTo get() = stats[Command.CUBIC_TO]
        val close get() = stats[Command.CLOSE]
        fun reset() {
            for (n in stats.indices) stats[n] = 0
        }
        override fun toString(): String = "Stats(moveTo=$moveTo, lineTo=$lineTo, quadTo=$quadTo, cubicTo=$cubicTo, close=$close)"
    }

    fun readStats(out: Stats = Stats()): Stats {
        out.reset()
        for (cmd in commands) out.stats[cmd]++
        return out
    }

    object Command {
        const val MOVE_TO = 0
        const val LINE_TO = 1
        const val QUAD_TO = 2
        const val CUBIC_TO = 3
        const val CLOSE = 4

        fun getParamCount(command: Int): Int = when (command) {
            MOVE_TO, LINE_TO -> 2
            QUAD_TO -> 4
            CUBIC_TO -> 6
            CLOSE -> 0
            else -> 0
        }
    }

    fun write(path: VectorPath, transform: Matrix = identityMatrix) {
        this.commands += path.commands
        if (transform.isIdentity()) {
            this.data += path.data
            lastXY(path.lastX, path.lastY)
        } else {
            @Suppress("ReplaceManualRangeWithIndicesCalls")
            for (n in 0 until path.data.size step 2) {
                val x = path.data[n + 0]
                val y = path.data[n + 1]
                this.data += transform.transformX(x, y)
                this.data += transform.transformY(x, y)
            }
            lastXY(
                transform.transformX(path.lastX, path.lastY),
                transform.transformY(path.lastX, path.lastY)
            )
        }
        version++
    }

    //typealias Winding = com.soywiz.korma.geom.vector.Winding
    //typealias LineJoin = com.soywiz.korma.geom.vector.LineJoin
    //typealias LineCap = com.soywiz.korma.geom.vector.LineCap

    override fun toSvgString(): String = buildString {
        visitCmds(
            moveTo = { x, y -> append("M${x.niceStr},${y.niceStr} ") },
            lineTo = { x, y -> append("L${x.niceStr},${y.niceStr} ") },
            quadTo = { x1, y1, x2, y2 -> append("Q${x1.niceStr},${y1.niceStr},${x2.niceStr},${y2.niceStr} ") },
            cubicTo = { x1, y1, x2, y2, x3, y3 -> append("C${x1.niceStr},${y1.niceStr},${x2.niceStr},${y2.niceStr},${x3.niceStr},${y3.niceStr} ") },
            close = { append("Z ") }
        )
    }.trimEnd()
    override fun toString(): String = "VectorPath(${toSvgString()})"

    fun scale(sx: Double, sy: Double = sx): VectorPath {
        for (n in 0 until data.size step 2) {
            data[n + 0] *= sx
            data[n + 1] *= sy
        }
        version++
        return this
    }

    fun floor(): VectorPath {
        for (n in 0 until data.size) data[n] = kotlin.math.floor(data[n])
        version++
        return this
    }

    fun round(): VectorPath {
        for (n in 0 until data.size) data[n] = kotlin.math.round(data[n])
        version++
        return this
    }

    fun ceil(): VectorPath {
        for (n in 0 until data.size) data[n] = kotlin.math.ceil(data[n])
        version++
        return this
    }
}

fun VectorBuilder.path(path: VectorPath?) {
    if (path != null) write(path)
}

fun VectorBuilder.write(path: VectorPath) {
    path.visitCmds(
        moveTo = { x, y -> moveTo(x, y) },
        lineTo = { x, y -> lineTo(x, y) },
        quadTo = { x0, y0, x1, y1 -> quadTo(x0, y0, x1, y1) },
        cubicTo = { x0, y0, x1, y1, x2, y2 -> cubicTo(x0, y0, x1, y1, x2, y2) },
        close = { close() }
    )
}

fun VectorBuilder.moveTo(x: Double, y: Double, m: Matrix?) = if (m != null) moveTo(m.transformX(x, y), m.transformY(x, y)) else moveTo(x, y)
fun VectorBuilder.lineTo(x: Double, y: Double, m: Matrix?) = if (m != null) lineTo(m.transformX(x, y), m.transformY(x, y)) else lineTo(x, y)
fun VectorBuilder.quadTo(cx: Double, cy: Double, ax: Double, ay: Double, m: Matrix?) =
    if (m != null) {
        quadTo(
            m.transformX(cx, cy), m.transformY(cx, cy),
            m.transformX(ax, ay), m.transformY(ax, ay)
        )
    } else {
        quadTo(cx, cy, ax, ay)
    }
fun VectorBuilder.cubicTo(cx1: Double, cy1: Double, cx2: Double, cy2: Double, ax: Double, ay: Double, m: Matrix?) =
    if (m != null) {
        cubicTo(
            m.transformX(cx1, cy1), m.transformY(cx1, cy1),
            m.transformX(cx2, cy2), m.transformY(cx2, cy2),
            m.transformX(ax, ay), m.transformY(ax, ay)
        )
    } else {
        cubicTo(cx1, cy1, cx2, cy2, ax, ay)
    }


fun VectorBuilder.path(path: VectorPath, m: Matrix?) {
    write(path, m)
}

fun VectorBuilder.write(path: VectorPath, m: Matrix?) {
    path.visitCmds(
        moveTo = { x, y -> moveTo(x, y, m) },
        lineTo = { x, y -> lineTo(x, y, m) },
        quadTo = { x0, y0, x1, y1 -> quadTo(x0, y0, x1, y1, m) },
        cubicTo = { x0, y0, x1, y1, x2, y2 -> cubicTo(x0, y0, x1, y1, x2, y2, m) },
        close = { close() }
    )
}

fun BoundsBuilder.add(path: VectorPath, transform: Matrix? = null) {
    path.getCurvesList().fastForEach { curves ->
        curves.beziers.fastForEach { bezier ->
            addEvenEmpty(bezier.getBounds(this.tempRect, transform))
        }
    }

    //println("BoundsBuilder.add.path: " + bb.getBounds())
}

fun VectorPath.applyTransform(m: Matrix?): VectorPath {
    if (m != null) {
        @Suppress("ReplaceManualRangeWithIndicesCalls")
        for (n in 0 until data.size step 2) {
            val x = data.getAt(n + 0)
            val y = data.getAt(n + 1)
            data[n + 0] = m.transformX(x, y)
            data[n + 1] = m.transformY(x, y)
        }
    }
    return this
}

@ThreadLocal
private var VectorPath._curvesCacheVersion by extraProperty { -1 }
@ThreadLocal
private var VectorPath._curvesCache by extraProperty<List<Curves>?> { null }

fun VectorPath.getCurvesList(): List<Curves> {
    if (_curvesCacheVersion != version) {
        _curvesCacheVersion = version
        _curvesCache = arrayListOf<Curves>().also { out ->
            var currentClosed = false
            var current = arrayListOf<Bezier>()
            fun flush() {
                if (current.isEmpty()) return
                out.add(Curves(current, currentClosed).also { it.assumeConvex = assumeConvex })
                currentClosed = false
                current = arrayListOf()
            }
            visitEdges(
                line = { x0, y0, x1, y1 -> current += Bezier(x0, y0, x1, y1) },
                quad = { x0, y0, x1, y1, x2, y2 -> current += Bezier(x0, y0, x1, y1, x2, y2) },
                cubic = { x0, y0, x1, y1, x2, y2, x3, y3 -> current += Bezier(x0, y0, x1, y1, x2, y2, x3, y3) },
                move = { x, y -> flush() },
                close = {
                    currentClosed = true
                    flush()
                }
            )
            flush()
        }
    }
    return _curvesCache!!
}

fun VectorPath.getCurves(): Curves {
    val curvesList = getCurvesList()
    return curvesList.flatMap { it.beziers }.toCurves(curvesList.lastOrNull()?.closed ?: false)
}

fun VectorPath.toCurves(): Curves = getCurves()
fun VectorPath.toCurvesList(): List<Curves> = getCurvesList()

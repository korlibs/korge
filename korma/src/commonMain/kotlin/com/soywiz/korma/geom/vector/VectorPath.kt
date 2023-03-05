package com.soywiz.korma.geom.vector

import com.soywiz.kds.*
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.kmem.*
import com.soywiz.korma.annotations.KormaExperimental
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.bezier.Bezier
import com.soywiz.korma.geom.bezier.Curves
import com.soywiz.korma.geom.bezier.toCurves
import com.soywiz.korma.geom.trapezoid.*
import com.soywiz.korma.internal.niceStr
import com.soywiz.korma.math.isAlmostEquals
import com.soywiz.korma.math.roundDecimalPlaces
import kotlin.native.concurrent.ThreadLocal

interface IVectorPath : VectorBuilder {
    fun toSvgString(): String
}

@OptIn(KormaExperimental::class)
class VectorPath(
    val commands: IntArrayList = IntArrayList(),
    val data: FloatArrayList = FloatArrayList(),
    var winding: Winding = Winding.DEFAULT,
    var optimize: Boolean = true,
) : IVectorPath, Extra by Extra.Mixin() {
    var assumeConvex: Boolean = false
    var version: Int = 0

    fun clone(): VectorPath = VectorPath(IntArrayList(commands), FloatArrayList(data), winding)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is VectorPath && this.commands == other.commands && this.data == other.data && this.winding == other.winding
    }
    override fun hashCode(): Int = commands.hashCode() + (data.hashCode() * 13) + (winding.ordinal * 111)

    companion object {
        private val identityMatrix = MMatrix()

        inline operator fun invoke(winding: Winding = Winding.DEFAULT, callback: VectorPath.() -> Unit): VectorPath = VectorPath(winding = winding).apply(callback)

        fun intersects(left: VectorPath, leftTransform: MMatrix, right: VectorPath, rightTransform: MMatrix): Boolean =
            left.intersectsWith(leftTransform, right, rightTransform)

        fun intersects(left: VectorPath, right: VectorPath): Boolean = left.intersectsWith(right)
    }

    interface Visitor {
        fun close() = Unit
        fun moveTo(p: Point) = Unit
        fun lineTo(p: Point) = Unit
        fun quadTo(c: Point, a: Point) = Unit
        fun cubicTo(c1: Point, c2: Point, a: Point) = Unit
    }

    inline fun visitCmds(
        moveTo: (p: Point) -> Unit,
        lineTo: (p: Point) -> Unit,
        quadTo: (c: Point, a: Point) -> Unit,
        cubicTo: (c1: Point, c2: Point, A: Point) -> Unit,
        close: () -> Unit
    ) {
        var n = 0
        commands.fastForEach { cmd ->
            when (cmd) {
                Command.MOVE_TO -> moveTo(Point(data[n++], data[n++]))
                Command.LINE_TO -> lineTo(Point(data[n++], data[n++]))
                Command.QUAD_TO -> quadTo(Point(data[n++], data[n++]), Point(data[n++], data[n++]))
                Command.CUBIC_TO -> cubicTo(Point(data[n++], data[n++]), Point(data[n++], data[n++]), Point(data[n++], data[n++]))
                Command.CLOSE -> close()
            }
        }
    }

    inline fun visitEdges(
        line: (p0: Point, p1: Point) -> Unit,
        quad: (p0: Point, p1: Point, p2: Point) -> Unit,
        cubic: (p0: Point, p1: Point, p2: Point, p3: Point) -> Unit,
        close: () -> Unit = {},
        move: (p: Point) -> Unit = { },
        dummy: Unit = Unit, // Prevents tailing lambda
        optimizeClose: Boolean = true
    ) {
        var m = Point()
        var l = Point()
        visitCmds(
            moveTo = {
                m = it; l = it
                move(it)
            },
            lineTo = {
                line(l, it)
                l = it
            },
            quadTo = { p1, p2 ->
                quad(l, p1, p2)
                l = p2
            },
            cubicTo = { p1, p2, p3 ->
                cubic(l, p1, p2, p3)
                l = p3
            },
            close = {
                val equal = when {
                    optimizeClose -> l.isAlmostEquals(m)
                    else -> l == m
                }
                if (!equal) {
                    line(l, m)
                }
                close()
            }
        )
    }

    inline fun visitEdgesSimple(
        line: (p1: Point, p2: Point) -> Unit,
        cubic: (p1: Point, p2: Point, p3: Point, p4: Point) -> Unit,
        close: () -> Unit
    ) = visitEdges(
        line,
        { p1, p2, p3 ->
            //val cx1 = Bezier.quadToCubic1(x0, x1, x2)
            //val cy1 = Bezier.quadToCubic1(y0, y1, y2)
            //val cx2 = Bezier.quadToCubic2(x0, x1, x2)
            //val cy2 = Bezier.quadToCubic2(y0, y1, y2)
            //cubic(x0, y0, cx1, cy1, cx2, cy2, x2, y2)
            Bezier.quadToCubic(p1.xD, p1.yD, p2.xD, p2.yD, p3.xD, p3.yD) { qx0, qy0, qx1, qy1, qx2, qy2, qx3, qy3 ->
                cubic(Point(qx0, qy0), Point(qx1, qy1), Point(qx2, qy2), Point(qx3, qy3))
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
        lastPos = Point()
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
        this.lastPos = other.lastPos
        version++
    }

    override var lastPos = Point()

    override fun moveTo(p: Point) {
        if (commands.isNotEmpty() && commands.last() == Command.MOVE_TO) {
            if (lastPos == p) return
        }
        commands.add(Command.MOVE_TO)
        data.add(p.xF, p.yF)
        lastPos = p
        version++
    }

    override fun lineTo(p: Point) {
        if (ensureMoveTo(p) && optimize) return
        if (p == lastPos && optimize) return
        commands.add(Command.LINE_TO)
        data.add(p.x, p.y)
        lastPos = p
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

    override fun quadTo(c: Point, a: Point) {
        ensureMoveTo(c)
        commands.add(Command.QUAD_TO)
        data.add(c.x, c.y, a.x, a.y)
        lastPos = a
        version++
    }

    override fun cubicTo(c1: Point, c2: Point, a: Point) {
        ensureMoveTo(c1)
        commands.add(Command.CUBIC_TO)
        data.add(c1.x, c1.y, c2.x, c2.y, a.x, a.y)
        lastPos = a
        version++
    }

    override fun close() {
        commands.add(Command.CLOSE)
        version++
    }

    override val totalPoints: Int get() = data.size / 2

    private fun ensureMoveTo(p: Point): Boolean {
        if (isNotEmpty()) return false
        moveTo(p)
        return true
    }

    fun getBounds(out: MRectangle = MRectangle(), bb: BoundsBuilder = BoundsBuilder()): MRectangle {
        bb.reset()
        bb.add(this)
        return bb.getBounds(out)
    }

    // http://erich.realtimerendering.com/ptinpoly/
    // http://stackoverflow.com/questions/217578/how-can-i-determine-whether-a-2d-point-is-within-a-polygon/2922778#2922778
    // https://www.particleincell.com/2013/cubic-line-intersection/
    // I run a semi-infinite ray horizontally (increasing x, fixed y) out from the test point, and count how many edges it crosses.
    // At each crossing, the ray switches between inside and outside. This is called the Jordan curve theorem.
    fun containsPoint(x: Double, y: Double): Boolean = trapezoids.containsPoint(x, y, this.winding)
    fun containsPoint(p: Point): Boolean = containsPoint(p.xD, p.yD, this.winding)
    fun containsPoint(p: IPoint): Boolean = containsPoint(p.x, p.y, this.winding)
    fun containsPoint(x: Int, y: Int): Boolean = containsPoint(x.toDouble(), y.toDouble())
    fun containsPoint(x: Float, y: Float): Boolean = containsPoint(x.toDouble(), y.toDouble())

    private var _trapezoids: VectorPathTrapezoids? = null

    @KormaExperimental
    val trapezoids: VectorPathTrapezoids get() {
        if (_trapezoids == null || _trapezoids!!.version != this.version) {
            _trapezoids = VectorPathTrapezoids(this.version, this)
        }
        return _trapezoids!!
    }

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
    //fun containsPoint(x: Double, y: Double, winding: Winding): Boolean = trapezoids.containsPoint(x, y, winding) // @TODO: This is not working properly!
    fun containsPoint(x: Int, y: Int, winding: Winding): Boolean = containsPoint(x.toDouble(), y.toDouble(), winding)
    fun containsPoint(x: Float, y: Float, winding: Winding): Boolean = containsPoint(x.toDouble(), y.toDouble(), winding)

    fun intersectsWith(right: VectorPath): Boolean = intersectsWith(identityMatrix, right, identityMatrix)

    // @TODO: Use trapezoids instead
    fun intersectsWith(leftMatrix: MMatrix, right: VectorPath, rightMatrix: MMatrix, tempMatrix: MMatrix = MMatrix()): Boolean {
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

    fun getLineIntersection(line: MLine, out: LineIntersection = LineIntersection()): LineIntersection? {
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
        val moveTo: Int get() = stats[Command.MOVE_TO]
        val lineTo: Int get() = stats[Command.LINE_TO]
        val quadTo: Int get() = stats[Command.QUAD_TO]
        val cubicTo: Int get() = stats[Command.CUBIC_TO]
        val close: Int get() = stats[Command.CLOSE]
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

    fun write(path: VectorPath, transform: MMatrix = identityMatrix) {
        this.commands += path.commands
        if (transform.isIdentity()) {
            this.data += path.data
            lastPos = path.lastPos
        } else {
            @Suppress("ReplaceManualRangeWithIndicesCalls")
            for (n in 0 until path.data.size step 2) {
                val p = transform.transform(Point(path.data[n + 0], path.data[n + 1]))
                this.data.add(p.x, p.y)
            }
            lastPos = transform.transform(path.lastPos)
        }
        version++
    }

    //typealias Winding = com.soywiz.korma.geom.vector.Winding
    //typealias LineJoin = com.soywiz.korma.geom.vector.LineJoin
    //typealias LineCap = com.soywiz.korma.geom.vector.LineCap

    override fun toSvgString(): String = buildString {
        visitCmds(
            moveTo = { (x, y) -> append("M${x.niceStr},${y.niceStr} ") },
            lineTo = { (x, y) -> append("L${x.niceStr},${y.niceStr} ") },
            quadTo = { (x1, y1), (x2, y2) -> append("Q${x1.niceStr},${y1.niceStr},${x2.niceStr},${y2.niceStr} ") },
            cubicTo = { (x1, y1), (x2, y2), (x3, y3) -> append("C${x1.niceStr},${y1.niceStr},${x2.niceStr},${y2.niceStr},${x3.niceStr},${y3.niceStr} ") },
            close = { append("Z ") }
        )
    }.trimEnd()
    override fun toString(): String = "VectorPath(${toSvgString()})"

    inline fun transformPoints(transform: (p: Point) -> Point): VectorPath {
        for (n in 0 until data.size step 2) {
            val p = transform(Point(data[n + 0], data[n + 1]))
            data[n + 0] = p.x
            data[n + 1] = p.y
        }
        version++
        return this
    }

    fun scale(sx: Double, sy: Double = sx): VectorPath = transformPoints { Point(it.x * sx, it.y * sy) }

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

    fun roundDecimalPlaces(places: Int): VectorPath {
        for (n in 0 until data.size) data[n] = data[n].roundDecimalPlaces(places)
        version++
        return this
    }

    fun ceil(): VectorPath {
        for (n in 0 until data.size) data[n] = kotlin.math.ceil(data[n])
        version++
        return this
    }
}

class VectorPathTrapezoids(val version: Int, val path: VectorPath, val scale: Int = 100) {
    val segments = path.toSegments(scale)
    val trapezoidsEvenOdd by lazy { SegmentIntToTrapezoidIntList.convert(segments, Winding.EVEN_ODD) }
    val trapezoidsNonZero by lazy { SegmentIntToTrapezoidIntList.convert(segments, Winding.NON_ZERO) }
    fun trapezoids(winding: Winding = path.winding): FTrapezoidsInt = when (winding) {
        Winding.EVEN_ODD -> trapezoidsEvenOdd
        Winding.NON_ZERO -> trapezoidsNonZero
    }
    fun containsPoint(x: Double, y: Double, winding: Winding = path.winding): Boolean =
        trapezoids(winding).containsPoint((x * scale).toIntRound(), (y * scale).toIntRound())
}

fun VectorBuilder.path(path: VectorPath?) {
    if (path != null) write(path)
}

fun VectorBuilder.write(path: VectorPath) {
    path.visitCmds(
        moveTo = { moveTo(it) },
        lineTo = { lineTo(it) },
        quadTo = { p1, p2 -> quadTo(p1, p2) },
        cubicTo = { p1, p2, p3 -> cubicTo(p1, p2, p3) },
        close = { close() }
    )
}

fun VectorBuilder.moveTo(p: Point, m: MMatrix?) = if (m != null) moveTo(m.transform(p)) else moveTo(p)
fun VectorBuilder.lineTo(p: Point, m: MMatrix?) = if (m != null) lineTo(m.transform(p)) else lineTo(p)
fun VectorBuilder.quadTo(c: Point, a: Point, m: MMatrix?) =
    if (m != null) {
        quadTo(m.transform(c), m.transform(a))
    } else {
        quadTo(c, a)
    }
fun VectorBuilder.cubicTo(c1: Point, c2: Point, a: Point, m: MMatrix?) =
    if (m != null) {
        cubicTo(m.transform(c1), m.transform(c2), m.transform(a))
    } else {
        cubicTo(c1, c2, a)
    }


fun VectorBuilder.path(path: VectorPath, m: MMatrix?) {
    write(path, m)
}

fun VectorBuilder.write(path: VectorPath, m: MMatrix?) {
    path.visitCmds(
        moveTo = { moveTo(it, m) },
        lineTo = { lineTo(it, m) },
        quadTo = { p1, p2 -> quadTo(p1, p2, m) },
        cubicTo = { p1, p2, p3 -> cubicTo(p1, p2, p3, m) },
        close = { close() }
    )
}

fun BoundsBuilder.add(path: VectorPath, transform: MMatrix? = null) {
    val curvesList = path.getCurvesList()
    if (curvesList.isEmpty() && path.isNotEmpty()) {
        path.visit(object : VectorPath.Visitor {
            override fun moveTo(p: Point) { add(p) }
        })
    }
    curvesList.fastForEach { curves ->
        curves.beziers.fastForEach { bezier ->
            addEvenEmpty(bezier.getBounds(this.tempRect, transform))
        }
    }

    //println("BoundsBuilder.add.path: " + bb.getBounds())
}

fun VectorPath.applyTransform(m: MMatrix?): VectorPath = when {
    m != null -> transformPoints { m.transform(it) }
    else -> this
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
                line = { p1, p2 -> current += Bezier(p1, p2) },
                quad = { p1, p2, p3 -> current += Bezier(p1, p2, p3) },
                cubic = { p1, p2, p3, p4 -> current += Bezier(p1, p2, p3, p4) },
                move = { p -> flush() },
                close = {
                    currentClosed = true
                    flush()
                },
                optimizeClose = true
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

package com.soywiz.korma.geom.vector

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.korma.annotations.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.bezier.*
import com.soywiz.korma.geom.shape.*
import com.soywiz.korma.internal.niceStr
import kotlin.native.concurrent.ThreadLocal

// @TODO: ThreadLocal on JVM
@ThreadLocal
private val tempMatrix: Matrix = Matrix()
@ThreadLocal
private val identityMatrix: Matrix = Matrix()

open class VectorPath(
    val commands: IntArrayList = IntArrayList(),
    val data: DoubleArrayList = DoubleArrayList(),
    var winding: Winding = Winding.EVEN_ODD
) : VectorBuilder {
    var version: Int = 0

    open fun clone(): VectorPath = VectorPath(IntArrayList(commands), DoubleArrayList(data), winding)

    companion object {
        private val identityMatrix = Matrix()

        inline operator fun invoke(winding: Winding = Winding.EVEN_ODD, callback: VectorPath.() -> Unit): VectorPath = VectorPath(winding = winding).apply(callback)

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
                Command.MOVE_TO -> {
                    val x = data.getAt(n++)
                    val y = data.getAt(n++)
                    moveTo(x, y)
                }
                Command.LINE_TO -> {
                    val x = data.getAt(n++)
                    val y = data.getAt(n++)
                    lineTo(x, y)
                }
                Command.QUAD_TO -> {
                    val x1 = data.getAt(n++)
                    val y1 = data.getAt(n++)
                    val x2 = data.getAt(n++)
                    val y2 = data.getAt(n++)
                    quadTo(x1, y1, x2, y2)
                }
                Command.CUBIC_TO -> {
                    val x1 = data.getAt(n++)
                    val y1 = data.getAt(n++)
                    val x2 = data.getAt(n++)
                    val y2 = data.getAt(n++)
                    val x3 = data.getAt(n++)
                    val y3 = data.getAt(n++)
                    cubicTo(x1, y1, x2, y2, x3, y3)
                }
                Command.CLOSE -> {
                    close()
                }
            }
        }
    }

    inline fun visitEdges(
        line: (x0: Double, y0: Double, x1: Double, y1: Double) -> Unit,
        quad: (x0: Double, y0: Double, x1: Double, y1: Double, x2: Double, y2: Double) -> Unit,
        cubic: (x0: Double, y0: Double, x1: Double, y1: Double, x2: Double, y2: Double, x3: Double, y3: Double) -> Unit,
        close: () -> Unit
    ) {
        var mx = 0.0
        var my = 0.0
        var lx = 0.0
        var ly = 0.0
        visitCmds(
            moveTo = { x, y ->
                mx = x; my = y
                lx = x; ly = y
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
                if ((lx != mx) || (ly != my)) {
                    line(lx, ly, mx, my)
                }
                close()
            }
        )
    }

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
        lastX = 0.0
        lastY = 0.0
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
        this.lastX = other.lastX
        this.lastY = other.lastY
        version++
    }

    override var lastX = 0.0
    override var lastY = 0.0

    override fun moveTo(x: Double, y: Double) {
        commands += Command.MOVE_TO
        data += x
        data += y
        lastX = x
        lastY = y
        version++
    }

    override fun lineTo(x: Double, y: Double) {
        ensureMoveTo(x, y)
        commands += Command.LINE_TO
        data += x
        data += y
        lastX = x
        lastY = y
        version++
    }

    override fun quadTo(cx: Double, cy: Double, ax: Double, ay: Double) {
        ensureMoveTo(cx, cy)
        commands += Command.QUAD_TO
        data += cx
        data += cy
        data += ax
        data += ay
        lastX = ax
        lastY = ay
        version++
    }

    override fun cubicTo(cx1: Double, cy1: Double, cx2: Double, cy2: Double, ax: Double, ay: Double) {
        ensureMoveTo(cx1, cy1)
        commands += Command.CUBIC_TO
        data += cx1
        data += cy1
        data += cx2
        data += cy2
        data += ax
        data += ay
        lastX = ax
        lastY = ay
        version++
    }

    override fun close() {
        commands += Command.CLOSE
        version++
    }

    override val totalPoints: Int get() = data.size / 2

    private fun ensureMoveTo(x: Double, y: Double) {
        if (isEmpty()) moveTo(x, y)
    }

    @PublishedApi
    internal val bezierTemp = Bezier.Temp()

    fun getBounds(out: Rectangle = Rectangle(), bb: BoundsBuilder = BoundsBuilder()): Rectangle {
        bb.reset()
        bb.add(this)
        return bb.getBounds(out)
    }

    fun getPoints(): List<IPoint> {
        val points = arrayListOf<IPoint>()
        this.visitCmds(
            moveTo = { x, y -> points += IPoint(x, y) },
            lineTo = { x, y -> points += IPoint(x, y) },
            quadTo = { x1, y1, x2, y2 -> points += IPoint(x2, y2) },
            cubicTo = { x1, y1, x2, y2, x3, y3 -> points += IPoint(x3, y3) },
            close = { }
        )
        return points
    }

    // http://erich.realtimerendering.com/ptinpoly/
    // http://stackoverflow.com/questions/217578/how-can-i-determine-whether-a-2d-point-is-within-a-polygon/2922778#2922778
    // https://www.particleincell.com/2013/cubic-line-intersection/
    // I run a semi-infinite ray horizontally (increasing x, fixed y) out from the test point, and count how many edges it crosses.
    // At each crossing, the ray switches between inside and outside. This is called the Jordan curve theorem.
    fun containsPoint(x: Double, y: Double): Boolean = containsPoint(x, y, this.winding)
    fun containsPoint(x: Int, y: Int): Boolean = containsPoint(x.toDouble(), y.toDouble())
    fun containsPoint(x: Float, y: Float): Boolean = containsPoint(x.toDouble(), y.toDouble())

    @OptIn(KormaExperimental::class)
    private val scanline by lazy { PolygonScanline().also {
        it.add(this)
        it.version = this.version
    } }
    private fun ensureScanline() = scanline.also {
        if (it.version != this.version) {
            it.reset()
            it.add(this)
            it.version = this.version
        }
    }
    fun containsPoint(x: Double, y: Double, winding: Winding): Boolean = ensureScanline().containsPoint(x, y, winding)
    fun containsPoint(x: Int, y: Int, winding: Winding): Boolean = containsPoint(x.toDouble(), y.toDouble(), winding)
    fun containsPoint(x: Float, y: Float, winding: Winding): Boolean = containsPoint(x.toDouble(), y.toDouble(), winding)

    fun intersectsWith(right: VectorPath): Boolean = intersectsWith(identityMatrix, right, identityMatrix)

    fun intersectsWith(leftMatrix: Matrix, right: VectorPath, rightMatrix: Matrix): Boolean {
        val left = this
        val leftScanline = left.ensureScanline()
        val rightScanline = right.ensureScanline()

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
    }

    fun write(path: VectorPath, transform: Matrix = identityMatrix) {
        this.commands += path.commands
        if (transform.isIdentity()) {
            this.data += path.data
            this.lastX = path.lastX
            this.lastY = path.lastY
        } else {
            for (n in 0 until path.data.size step 2) {
                val x = path.data.getAt(n + 0)
                val y = path.data.getAt(n + 1)
                this.data += transform.transformX(x, y)
                this.data += transform.transformY(x, y)
            }
            this.lastX = transform.transformX(path.lastX, path.lastY)
            this.lastY = transform.transformY(path.lastX, path.lastY)
        }
        version++
    }

    //typealias Winding = com.soywiz.korma.geom.vector.Winding
    //typealias LineJoin = com.soywiz.korma.geom.vector.LineJoin
    //typealias LineCap = com.soywiz.korma.geom.vector.LineCap

    fun toSvgString(): String = buildString {
        visitCmds(
            moveTo = { x, y -> append("M${x.niceStr},${y.niceStr} ") },
            lineTo = { x, y -> append("L${x.niceStr},${y.niceStr} ") },
            quadTo = { x1, y1, x2, y2 -> append("Q${x1.niceStr},${y1.niceStr},${x2.niceStr},${y2.niceStr} ") },
            cubicTo = { x1, y1, x2, y2, x3, y3 -> append("C${x1.niceStr},${y1.niceStr},${x2.niceStr},${y2.niceStr},${x3.niceStr},${y3.niceStr} ") },
            close = { append("Z ") }
        )
    }.trimEnd()
    override fun toString(): String = "VectorPath(${toSvgString()})"
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

fun BoundsBuilder.add(path: VectorPath, transform: Matrix) {
    val bb = this
    var lx = 0.0
    var ly = 0.0

    path.visitCmds(
        moveTo = { x, y -> bb.add(x, y, transform).also { lx = x }.also { ly = y } },
        lineTo = { x, y -> bb.add(x, y, transform).also { lx = x }.also { ly = y } },
        quadTo = { cx, cy, ax, ay ->
            bb.add(Bezier.quadBounds(lx, ly, cx, cy, ax, ay, bb.tempRect), transform)
                .also { lx = ax }.also { ly = ay }
        },
        cubicTo = { cx1, cy1, cx2, cy2, ax, ay ->
            bb.add(Bezier.cubicBounds(lx, ly, cx1, cy1, cx2, cy2, ax, ay, bb.tempRect, path.bezierTemp), transform)
                .also { lx = ax }.also { ly = ay }
        },
        close = {}
    )
}

fun BoundsBuilder.add(path: VectorPath) {
    val bb = this
    var lx = 0.0
    var ly = 0.0

    path.visitCmds(
        moveTo = { x, y -> bb.add(x, y).also { lx = x }.also { ly = y } },
        lineTo = { x, y -> bb.add(x, y).also { lx = x }.also { ly = y } },
        quadTo = { cx, cy, ax, ay ->
            bb.add(Bezier.quadBounds(lx, ly, cx, cy, ax, ay, bb.tempRect))
                .also { lx = ax }.also { ly = ay }
        },
        cubicTo = { cx1, cy1, cx2, cy2, ax, ay ->
            bb.add(Bezier.cubicBounds(lx, ly, cx1, cy1, cx2, cy2, ax, ay, bb.tempRect, path.bezierTemp))
                .also { lx = ax }.also { ly = ay }
        },
        close = {}
    )
}

fun VectorPath.applyTransform(m: Matrix): VectorPath {
    for (n in 0 until data.size step 2) {
        val x = data.getAt(n + 0)
        val y = data.getAt(n + 1)
        data[n + 0] = m.transformX(x, y)
        data[n + 1] = m.transformY(x, y)
    }
    return this
}

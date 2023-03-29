package korlibs.math.geom.shape

import korlibs.datastructure.*
import korlibs.datastructure.iterators.*
import korlibs.math.geom.*
import korlibs.math.geom.bezier.*
import korlibs.math.geom.vector.*
import kotlin.math.*

interface WithHitShape2d {
    val hitShape2d: Shape2d
}

abstract class AbstractNShape2d : Shape2d {
    abstract protected val lazyVectorPath: VectorPath
    protected val lazyCurves: List<Curves> by lazy { lazyVectorPath.toCurvesList() }

    override val perimeter: Float get() {
        var sum: Double = 0.0
        lazyCurves.fastForEach { sum += it.length }
        return sum.toFloat()
    }

    override val area: Float get() = if (lazyVectorPath.isLastCommandClose) lazyVectorPath.area else 0f
    override fun toVectorPath(): VectorPath = lazyVectorPath
    override fun distance(p: Point): Float = (p - projectedPoint(p)).length * insideSign(p)
    override fun normalVectorAt(p: Point): Vector2 = -projectedPointExt(p, normal = true)
    override fun projectedPoint(p: Point): Point = projectedPointExt(p, normal = false)
    protected fun insideSign(p: Point): Float = if (containsPoint(p)) -1f else +1f
    protected fun projectedPointExt(p: Point, normal: Boolean): Point {
        var length = Double.POSITIVE_INFINITY
        var pp = Point()
        var n = Point()
        lazyCurves.fastForEach { it.beziers.fastForEach {
            val out = it.project(p)
            if (length > out.dSq) {
                length = out.dSq
                pp = out.p
                if (normal) n = out.normal
            } else if (length == out.dSq) {
                //println("EQUALS!")
                length = out.dSq
                pp = out.p
                if (normal) {
                    n += out.normal
                }
            }
        } }
        return if (normal) n.normalized else pp
    }
    override fun containsPoint(p: Point): Boolean = lazyVectorPath.containsPoint(p)
}

val VectorPath.cachedPoints: PointList by Extra.PropertyThis { this.getPoints2() }

inline fun buildVectorPath(out: VectorPath = VectorPath(), block: VectorPath.() -> Unit): VectorPath = out.apply(block)
inline fun buildVectorPath(out: VectorPath = VectorPath(), winding: Winding = Winding.DEFAULT, block: VectorPath.() -> Unit): VectorPath = out.also { it.winding = winding }.apply(block)

fun List<Shape2d>.toShape2d(): Shape2d = Shape2d(*this.toTypedArray())
fun Shape2d.toShape2d(): Shape2d = this

// RoundRectangle
interface Shape2d {
    val center: Point get() = TODO()
    val area: Float
    val perimeter: Float
    // @TODO: SDF
    // @TODO: NormalVector

    /** Compute the distance to the shortest point to the edge (SDF). Negative inside. Positive outside. */
    fun distance(p: Point): Float = TODO()
    /** Returns the normal vector to the shortest point to the edge */
    fun normalVectorAt(p: Point): Vector2 = (p - center).normalized
    /** Point projected to the closest edge */
    fun projectedPoint(p: Point): Point = p - normalVectorAt(p) * distance(p)

    fun toVectorPath(): VectorPath
    fun containsPoint(p: Point): Boolean = distance(p) <= 0f
    fun getBounds(): Rectangle = toVectorPath().getBounds()
    //fun containsPoint(p: Point, mat: Matrix) = containsPoint(mat.transform(p))

    fun intersectsWith(that: Shape2d) = Shape2d.intersects(this, Matrix.NIL, that, Matrix.NIL)
    fun intersectsWith(ml: Matrix, that: Shape2d, mr: Matrix) = Shape2d.intersects(this, ml, that, mr)

    companion object {
        operator fun invoke(vararg shapes: Shape2d): Shape2d {
            if (shapes.isEmpty()) return EmptyShape2d
            if (shapes.size == 1) return shapes[0]
            return CompoundShape2d(shapes.toList())
        }

        fun intersects(l: Shape2d, ml: Matrix, r: Shape2d, mr: Matrix): Boolean {
            //println("Shape2d.intersects:"); println(" - l=$l[$ml]"); println(" - r=$r[$mr]")

            if (ml.isNIL && mr.isNIL && l is Circle && r is Circle) return optimizedIntersect(l, ml, r, mr)

            return _intersectsStep0(l, ml, r, mr) || _intersectsStep0(r, mr, l, ml)
        }

        //private fun optimizedIntersect(l: Circle, r: Circle): Boolean =
        //    Point.distance(l.center, r.center) < (l.radius + r.radius)
        //private fun optimizedIntersect(l: Circle, ml: Matrix, r: Circle, mr: Matrix): Boolean {
        //    if (ml.isNIL && mr.isNIL) return optimizedIntersect(l, r)
        //    val radiusL = ml.dtx(l.radius, l.radius)
        //    val radiusR = mr.dtx(r.radius, r.radius)
        //    //println("radiusL=$radiusL, radiusR=$radiusR")
        //    return Point.distance(ml.transform(l.center), ml.transform(r.center)) < radiusL + radiusR
        //}

        private fun _intersectsStep0(l: Shape2d, ml: Matrix, r: Shape2d, mr: Matrix): Boolean {
            var tempMatrix = if (mr.isNotNIL) mr.inverted() else Matrix.IDENTITY
            if (ml.isNotNIL) tempMatrix = tempMatrix.premultiplied(ml)

            l.toVectorPath().cachedPoints.fastForEach {
                if (r.containsPoint(tempMatrix.transform(it))) return true
            }
            return false
        }

        fun intersects(l: Shape2d, r: Shape2d): Boolean = intersects(l, Matrix.NIL, r, Matrix.NIL)

    }
}

data class CompoundShape2d(val shapes: List<Shape2d>) : Shape2d {
    override val area: Float get() = shapes.sumOf { it.area.toDouble() }.toFloat()
    override val perimeter: Float get() = shapes.sumOf { it.perimeter.toDouble() }.toFloat()

    override fun containsPoint(p: Point): Boolean {
        shapes.fastForEach { if (it.containsPoint(p)) return true }
        return false
    }
    override fun toVectorPath(): VectorPath = buildVectorPath { shapes.fastForEach { write(it.toVectorPath()) } }
}

object EmptyShape2d : Shape2d {
    override val area: Float get() = 0f
    override val perimeter: Float get() = 0f
    override fun containsPoint(p: Point): Boolean = false
    override fun toVectorPath(): VectorPath = buildVectorPath { }
    override val center: Point get() = Point.ZERO
    override fun distance(p: Point): Float = Float.POSITIVE_INFINITY
    override fun normalVectorAt(p: Point): Vector2 = Vector2.NaN
    override fun projectedPoint(p: Point): Point = Vector2.NaN
}

inline fun VectorPath.emitEdges(
    crossinline edge: (a: Point, b: Point) -> Unit
) {
    var firstPos = Point()
    var lastPos = Point()

    emitPoints2(
        flush = { close ->
            if (close) {
                edge(lastPos, firstPos)
                lastPos = firstPos
            }
        },
        emit = { p, move ->
            if (move) {
                firstPos = p
            } else {
                edge(lastPos, p)
            }
            lastPos = p
        }
    )
}



fun PointList.toPolygon(out: VectorPath = VectorPath()): VectorPath = buildVectorPath(out) { polygon(this@toPolygon) }



fun PointList.toShape2d(closed: Boolean = true): Shape2d {
    if (closed && this.size == 4) {
        val x0 = this.getX(0)
        val y0 = this.getY(0)
        val x1 = this.getX(2)
        val y1 = this.getY(2)
        if (this.getX(1) == x1 && this.getY(1) == y0 && this.getX(3) == x0 && this.getY(3) == y1) {
            return Rectangle.fromBounds(x0, y0, x1, y1)
        }
    }
    return if (closed) Polygon(this) else Polyline(this)
}

//fun VectorPath.toShape2dNew(closed: Boolean = true): Shape2d = VectorPath(this, closed)
fun VectorPath.toShape2dNew(closed: Boolean = true): Shape2d = this

//fun VectorPath.toShape2d(closed: Boolean = true): Shape2d = toShape2dNew(closed)
fun VectorPath.toShape2d(closed: Boolean = true): Shape2d = toShape2dOld(closed)

fun VectorPath.toShape2dOld(closed: Boolean = true): Shape2d {
    val items = toPathPointList().map { it.toShape2d(closed) }
    return when (items.size) {
        0 -> EmptyShape2d
        1 -> items.first()
        else -> CompoundShape2d(items)
    }
}


//fun Shape2d.getAllPoints(out: PointArrayList = PointArrayList()): PointArrayList = out.apply { for (path in this@getAllPoints.paths) add(path) }
//fun Shape2d.toPolygon(): Shape2d.Polygon = if (this is Shape2d.Polygon) this else Shape2d.Polygon(this.getAllPoints())

fun List<MPoint>.containsPoint(x: Double, y: Double): Boolean {
    var intersections = 0
    for (n in 0 until this.size - 1) {
        val p1 = this[n + 0]
        val p2 = this[n + 1]
        intersections += intersectionsWithLine(x, y, p1.x, p1.y, p2.x, p2.y)
    }
    return (intersections % 2) != 0
}

private fun intersectionsWithLine(
    ax: Double, ay: Double,
    bx0: Double, by0: Double, bx1: Double, by1: Double
): Int {
    return if (((by1 > ay) != (by0 > ay)) && (ax < (bx0 - bx1) * (ay - by1) / (by0 - by1) + bx1)) 1 else 0
}

private fun Matrix.tx(x: Double, y: Double): Double = if (this.isNotNIL) this.transformX(x, y) else x
private fun Matrix.ty(x: Double, y: Double): Double = if (this.isNotNIL) this.transformY(x, y) else y
private fun Matrix.dtx(x: Double, y: Double): Double = if (this.isNotNIL) this.deltaTransform(Point(x, y)).x.toDouble() else x
private fun Matrix.dty(x: Double, y: Double): Double = if (this.isNotNIL) this.deltaTransform(Point(x, y)).y.toDouble() else y

private fun Matrix.tx(x: Float, y: Float): Float = if (this.isNotNIL) this.transformX(x, y) else x
private fun Matrix.ty(x: Float, y: Float): Float = if (this.isNotNIL) this.transformY(x, y) else y
private fun Matrix.dtx(x: Float, y: Float): Float = if (this.isNotNIL) this.deltaTransform(Point(x, y)).x else x
private fun Matrix.dty(x: Float, y: Float): Float = if (this.isNotNIL) this.deltaTransform(Point(x, y)).y else y

private fun optimizedIntersect(l: Circle, r: Circle): Boolean =
    Point.distance(l.center, r.center) < (l.radius + r.radius)

private fun optimizedIntersect(l: Circle, ml: Matrix, r: Circle, mr: Matrix): Boolean {
    if (ml.isNIL && mr.isNIL) return optimizedIntersect(l, r)
    val radiusL = ml.dtx(l.radius, l.radius)
    val radiusR = mr.dtx(r.radius, r.radius)
    //println("radiusL=$radiusL, radiusR=$radiusR")
    return Point.distance(ml.transform(l.center), mr.transform(r.center)) < radiusL + radiusR
}

private fun VectorPath.getPoints2(out: PointArrayList = PointArrayList()): PointArrayList {
    emitPoints2 { p, move -> out.add(p) }
    return out
}

inline fun VectorPath.emitPoints2(
    crossinline flush: (close: Boolean) -> Unit = {},
    crossinline joint: (close: Boolean) -> Unit = {},
    crossinline emit: (p: Point, move: Boolean) -> Unit
) {
    var i = Point()
    var l = Point()
    flush(false)
    this.visitCmds(
        moveTo = {
            i = it
            emit(it, true)
            l = it
        },
        lineTo = {
            emit(it, false)
            l = it
            joint(false)
        },
        quadTo = { c, a ->
            val sum = Point.distance(l, c) + Point.distance(c, a)
            approximateCurve(sum.toInt(), { ratio, get -> get(Bezier.quadCalc(l, c, a, ratio)) }, { emit(it, false) })
            l = a
            joint(false)
        },
        cubicTo = { c0, c1, a ->
            val sum = Point.distance(l, c0) + Point.distance(c0, c1) + Point.distance(c1, a)
            approximateCurve(sum.toInt(), { ratio, get -> get(Bezier.cubicCalc(l, c0, c1, a, ratio.toFloat())) }, { emit(it, false) })
            l = a
            joint(false)
        },
        close = {
            emit(i, false)
            joint(true)
            flush(true)
        }
    )
    flush(false)
}

@PublishedApi internal inline fun approximateCurve(
    curveSteps: Int,
    compute: (ratio: Double, get: (Point) -> Unit) -> Unit,
    crossinline emit: (Point) -> Unit,
    includeStart: Boolean = false,
    includeEnd: Boolean = true,
) {
    val rcurveSteps = max(curveSteps, 20)
    val dt = 1.0 / rcurveSteps
    var lastPos = Point()
    var prevPos = Point()
    var emittedCount = 0
    compute(0.0) { lastPos = it }
    val nStart = if (includeStart) 0 else 1
    val nEnd = if (includeEnd) rcurveSteps else rcurveSteps - 1
    for (n in nStart .. nEnd) {
        val ratio = n * dt
        //println("ratio: $ratio")
        compute(ratio) {
            //if (emittedCount == 0) {
            emit(it)
            emittedCount++
            lastPos = prevPos
            prevPos = it
        }
    }
    //println("curveSteps: $rcurveSteps, emittedCount=$emittedCount")
}

// @TODO: Instead of use curveSteps, let's determine the maximum distance between points for the curve, or the maximum angle (so we have a quality factor instead)
@PublishedApi internal inline fun VectorPath.emitPoints(flush: (close: Boolean) -> Unit, emit: (Point) -> Unit, curveSteps: Int = 20) {
    var l = Point()
    flush(false)
    this.visitCmds(
        moveTo = {
            flush(false)
            emit(it)
            l = it
        },
        lineTo = {
            emit(it)
            l = it
        },
        quadTo = { c, a ->
            val dt = 1.0 / curveSteps
            for (n in 1 .. curveSteps) emit(Bezier.quadCalc(l, c, a, n * dt))
            l = a
        },
        cubicTo = { c1,c2, a ->
            val dt = 1.0 / curveSteps
            for (n in 1 .. curveSteps) emit(Bezier.cubicCalc(l, c1, c2, a, (n * dt).toFloat()))
            l = a
        },
        close = { flush(true) }
    )
    flush(false)
}

fun VectorPath.toPathPointList(m: Matrix = Matrix.NIL, emitClosePoint: Boolean = false): List<PointList> {
    val paths = arrayListOf<PointArrayList>()
    var path = PointArrayList()
    var firstPos = Point()
    var first = true
    emitPoints({ close ->
        if (close) {
            if (emitClosePoint) {
                path.add(firstPos)
            }
            path.closed = true
        }
        if (path.isNotEmpty()) {
            //if (path.getX(0) == path.getX(path.size - 1) && path.getY(0) == path.getY(path.size - 1)) path.removeAt(path.size - 1)
            //println("POINTS:" + path.size)
            //for (p in path.toPoints()) println(" - $p")
            paths += path
            path = PointArrayList()
        }
        first = true
    }, {
        if (first) {
            first = false
            firstPos = it
        }
        path.add(it.transformed(m))
    })
    return paths
}

internal fun PointList.toRectangleOrNull(): Rectangle? {
    if (this.size != 4) return null
    //check there are only unique points
    val points = setOf(getX(0) to getY(0), getX(1) to getY(1), getX(2) to getY(2), getX(3) to getY(3))
    if (points.size != 4) return null
    //check there are exactly two unique x/y coordinates
    val xs = setOf(getX(0), getX(1), getX(2), getX(3))
    val ys = setOf(getY(0), getY(1), getY(2), getY(3))
    if (xs.size != 2 || ys.size != 2) return null
    //get coordinates
    val left = xs.minOrNull() ?: return null
    val right = xs.maxOrNull() ?: return null
    val top = ys.maxOrNull() ?: return null
    val bottom = ys.minOrNull() ?: return null
    return Rectangle.fromBounds(top, left, right, bottom)
}

fun VectorPath.getPoints2List(): List<PointArrayList> {
    val out = arrayListOf<PointArrayList>()
    var current = PointArrayList()

    fun flush() {
        if (!current.isNotEmpty()) return
        out.add(current)
        current = PointArrayList()
    }

    emitPoints2 { p, move ->
        if (move) flush()
        current.add(p)
    }
    flush()
    return out
}

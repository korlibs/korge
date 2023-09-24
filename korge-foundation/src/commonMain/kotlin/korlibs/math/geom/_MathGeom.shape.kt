@file:Suppress("PackageDirectoryMismatch")

package korlibs.math.geom.shape

import korlibs.datastructure.*
import korlibs.datastructure.iterators.*
import korlibs.math.geom.*
import korlibs.math.geom.bezier.*
import korlibs.math.geom.vector.*
import korlibs.math.interpolation.*
import kotlin.math.*

interface WithHitShape2D {
    val hitShape2d: Shape2D
}

abstract class AbstractShape2D : Shape2D {
    abstract protected val lazyVectorPath: VectorPath
    override fun toVectorPath(): VectorPath = lazyVectorPath

    override fun distance(p: Point): Double = (p - projectedPoint(p)).length * insideSign(p)
    override fun normalVectorAt(p: Point): Vector2D = -projectedPointExt(p, normal = true)
    override fun projectedPoint(p: Point): Point = projectedPointExt(p, normal = false)
    protected fun insideSign(p: Point): Double = if (containsPoint(p)) -1.0 else +1.0
    protected fun projectedPointExt(p: Point, normal: Boolean): Point {
        var length = Double.POSITIVE_INFINITY
        var pp = Point()
        var n = Point()
        toVectorPath().getCurvesList().fastForEach { it.beziers.fastForEach {
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
    override fun containsPoint(p: Point): Boolean = toVectorPath().containsPoint(p)
}

val VectorPath.cachedPoints: PointList by Extra.PropertyThis { this.getPoints2() }

inline fun buildVectorPath(out: VectorPath = VectorPath(), block: VectorPath.() -> Unit): VectorPath = out.apply(block)
inline fun buildVectorPath(out: VectorPath = VectorPath(), winding: Winding = Winding.DEFAULT, block: VectorPath.() -> Unit): VectorPath = out.also { it.winding = winding }.apply(block)

fun List<Shape2D>.toShape2D(): Shape2D = Shape2D(*this.toTypedArray())
fun Shape2D.toShape2D(): Shape2D = this

@Deprecated("", ReplaceWith("toShape2D()")) fun List<Shape2D>.toShape2d(): Shape2D = toShape2D()
@Deprecated("", ReplaceWith("toShape2D()")) fun Shape2D.toShape2d(): Shape2D = toShape2D()

// RoundRectangle
interface Shape2D {
    val closed: Boolean get() = toVectorPath().isLastCommandClose
    val center: Point get() = getBounds().center
    val area: Double get() {
        val lazyVectorPath = toVectorPath()
        return if (lazyVectorPath.isLastCommandClose) lazyVectorPath.area else 0.0
    }
    val perimeter: Double get() {
        var sum = 0.0
        toVectorPath().getCurvesList().fastForEach { sum += it.length }
        return sum
    }

    /** Compute the distance to the shortest point to the edge (SDF). Negative inside. Positive outside. */
    fun distance(p: Point): Double = (p - projectedPoint(p)).length
    /** Returns the normal vector to the shortest point to the edge */
    fun normalVectorAt(p: Point): Vector2D
    /** Point projected to the closest edge */
    fun projectedPoint(p: Point): Point

    fun toVectorPath(): VectorPath
    fun containsPoint(p: Point): Boolean = distance(p) <= 0f
    fun getBounds(): Rectangle = toVectorPath().getBounds()
    //fun containsPoint(p: Point, mat: Matrix) = containsPoint(mat.transform(p))

    fun intersectionsWith(that: Shape2D): PointList = intersectionsWith(Matrix.NIL, that, Matrix.NIL)
    //fun intersectionsWith(ray: Ray): PointList = intersectionsWith(Matrix.NIL, ray, Matrix.NIL)
    fun intersectsWith(that: Shape2D) = Shape2D.intersects(this, Matrix.NIL, that, Matrix.NIL)
    fun intersectsWith(ml: Matrix, that: Shape2D, mr: Matrix) = Shape2D.intersects(this, ml, that, mr)

    //fun intersectionsWith(ml: Matrix, ray: Ray, mr: Matrix): PointList {
    //    //val mat = mr * ml.inverted()
    //    //this.toVectorPath().getBVHBeziers().intersect(ray.transformed(mat)).fastForEach {
    //    //    TODO()
    //    //}
    //    TODO()
    //}

    /** [ml] transformation matrix of this [Shape2D], [mr] transformation matrix of the point [p] */
    @Deprecated("Untested yet")
    fun containsPoint(ml: Matrix, p: Point, mr: Matrix): Boolean {
        val mat = mr * ml.inverted()
        return containsPoint(p.transformed(mat))
    }

    // @TODO: Check
    /** [ml] transformation matrix of this [Shape2D], [mr] transformation matrix of the point [p] */
    @Deprecated("Untested yet")
    fun distance(ml: Matrix, p: Point, mr: Matrix): Double {
        return (p.transformed(mr) - projectedPoint(ml, p, mr)).length
    }

    // @TODO: Check
    /** [ml] transformation matrix of this [Shape2D], [mr] transformation matrix of the point [p] */
    @Deprecated("Untested yet")
    fun normalVectorAt(ml: Matrix, p: Point, mr: Matrix): Point {
        val mat = mr * ml.inverted()
        return normalVectorAt(p.transformed(mat)).deltaTransformed(ml)
    }

    // @TODO: Check
    /** [ml] transformation matrix of this [Shape2D], [mr] transformation matrix of the point [p] */
    @Deprecated("Untested yet")
    fun projectedPoint(ml: Matrix, p: Point, mr: Matrix): Point {
        val mat = mr * ml.inverted()
        return projectedPoint(p.transformed(mat)).transformed(ml)
    }

    /** [ml] transformation matrix of this [Shape2D], [mr] transformation matrix of the shape [that] */
    fun intersectionsWith(ml: Matrix, that: Shape2D, mr: Matrix): PointList {
        val mat = mr * ml.inverted()

        val out = PointArrayList()
        val thatPath = that.toVectorPath()
        thatPath.getCurvesList().fastForEachBezier { bezier1 ->
            val bezier1 = bezier1.transform(mat)
            //println("BASE: $bezier1")
            this.toVectorPath().getBVHBeziers().search(bezier1.getBounds()).fastForEach {
                //println("  OTHER: $it")
                it.value?.let { bezier0 ->
                    bezier0.intersections(bezier1).fastForEach {
                        val p1 = bezier0[it.first]
                        val p2 = bezier1[it.second]
                        val p = Point.middle(p1, p2).transformed(ml)
                        //println("    EMIT: $it : $p")
                        if (out.isNotEmpty()) {
                            val diff = (out.last - p).absoluteValue
                            //println("      DIFF=$diff")
                            // Repeated
                            if (diff.maxComponent() < 0.5f) {
                                return@fastForEach
                            }
                        }
                        out.add(p)
                    }
                }
            }
        }
        return out
    }

    companion object {
        operator fun invoke(vararg shapes: Shape2D): Shape2D {
            if (shapes.isEmpty()) return EmptyShape2D
            if (shapes.size == 1) return shapes[0]
            return CompoundShape2D(shapes.toList())
        }

        fun intersections(l: Shape2D, ml: Matrix, r: Shape2D, mr: Matrix): PointList = l.intersectionsWith(ml, r, mr)

        fun intersects(l: Shape2D, ml: Matrix, r: Shape2D, mr: Matrix): Boolean {
            //println("Shape2D.intersects:"); println(" - l=$l[$ml]"); println(" - r=$r[$mr]")

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

        private fun _intersectsStep0(l: Shape2D, ml: Matrix, r: Shape2D, mr: Matrix): Boolean {
            var tempMatrix = if (mr.isNotNIL) mr.inverted() else Matrix.IDENTITY
            if (ml.isNotNIL) tempMatrix = tempMatrix.premultiplied(ml)

            l.toVectorPath().cachedPoints.fastForEach {
                if (r.containsPoint(tempMatrix.transform(it))) return true
            }
            return false
        }

        fun intersects(l: Shape2D, r: Shape2D): Boolean = intersects(l, Matrix.NIL, r, Matrix.NIL)
    }
}

//@Deprecated("") typealias CompoundShape2d = CompoundShape2D

data class CompoundShape2D(val shapes: List<Shape2D>) : AbstractShape2D() {
    override val lazyVectorPath: VectorPath by lazy {
        buildVectorPath { shapes.fastForEach { shape -> path(shape.toVectorPath()) } }
    }

    override val area: Double get() = shapes.sumOfDouble { it.area }
    override val perimeter: Double get() = shapes.sumOfDouble { it.perimeter }

    override fun intersectionsWith(ml: Matrix, that: Shape2D, mr: Matrix): PointList {
        val out = PointArrayList()
        shapes.fastForEach { shape ->
            out += shape.intersectionsWith(ml, that, mr)
        }
        return out
    }

    fun findClosestShape(p: Point): Shape2D? {
        var minDistance = Double.POSITIVE_INFINITY
        var shape: Shape2D? = null
        shapes.fastForEach {
            val dist = it.distance(p)
            if (dist < minDistance) {
                minDistance = dist
                shape = it
            }
        }
        return shape
    }

    override fun projectedPoint(p: Point): Point = findClosestShape(p)?.projectedPoint(p) ?: Point.NaN
    override fun distance(p: Point): Double = findClosestShape(p)?.distance(p) ?: Double.POSITIVE_INFINITY
    override fun normalVectorAt(p: Point): Vector2D = findClosestShape(p)?.normalVectorAt(p) ?: Vector2D.NaN

    override fun containsPoint(p: Point): Boolean {
        shapes.fastForEach { if (it.containsPoint(p)) return true }
        return false
    }
    override fun toVectorPath(): VectorPath = buildVectorPath { shapes.fastForEach { write(it.toVectorPath()) } }
}

//@Deprecated("") typealias EmptyShape2d = EmptyShape2D

object EmptyShape2D : Shape2D {
    override val area: Double get() = 0.0
    override val perimeter: Double get() = 0.0
    override fun containsPoint(p: Point): Boolean = false
    override fun toVectorPath(): VectorPath = buildVectorPath { }
    override val center: Point get() = Point.ZERO
    override fun distance(p: Point): Double = Double.POSITIVE_INFINITY
    override fun normalVectorAt(p: Point): Vector2D = Vector2D.NaN
    override fun projectedPoint(p: Point): Point = Vector2D.NaN
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

@Deprecated("", ReplaceWith("toShape2D(closed)")) fun PointList.toShape2d(closed: Boolean = true): Shape2D = toShape2D(closed)
fun PointList.toShape2D(closed: Boolean = true): Shape2D {
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

//fun VectorPath.toShape2dNew(closed: Boolean = true): Shape2D = VectorPath(this, closed)
@Deprecated("", ReplaceWith("toShape2DNew()")) fun VectorPath.toShape2dNew(closed: Boolean = true): Shape2D = toShape2DNew()
fun VectorPath.toShape2DNew(closed: Boolean = true): Shape2D = this

//fun VectorPath.toShape2d(closed: Boolean = true): Shape2D = toShape2dNew(closed)
@Deprecated("", ReplaceWith("toShape2D(closed)")) fun VectorPath.toShape2d(closed: Boolean = true): Shape2D = toShape2D(closed)
fun VectorPath.toShape2D(closed: Boolean = true): Shape2D = toShape2DOld(closed)

@Deprecated("", ReplaceWith("toShape2DOld(closed)")) fun VectorPath.toShape2dOld(closed: Boolean = true): Shape2D = toShape2DOld(closed)
fun VectorPath.toShape2DOld(closed: Boolean = true): Shape2D {
    val items = toPathPointList().map { it.toShape2d(closed) }
    return when (items.size) {
        0 -> EmptyShape2D
        1 -> items.first()
        else -> CompoundShape2D(items)
    }
}


//fun Shape2D.getAllPoints(out: PointArrayList = PointArrayList()): PointArrayList = out.apply { for (path in this@getAllPoints.paths) add(path) }
//fun Shape2D.toPolygon(): Shape2D.Polygon = if (this is Shape2D.Polygon) this else Shape2D.Polygon(this.getAllPoints())

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
private fun Matrix.dtx(x: Double, y: Double): Double = if (this.isNotNIL) this.deltaTransform(Point(x, y)).x else x
private fun Matrix.dty(x: Double, y: Double): Double = if (this.isNotNIL) this.deltaTransform(Point(x, y)).y else y

private fun Matrix.tx(x: Float, y: Float): Float = tx(x.toDouble(), y.toDouble()).toFloat()
private fun Matrix.ty(x: Float, y: Float): Float = ty(x.toDouble(), y.toDouble()).toFloat()
private fun Matrix.dtx(x: Float, y: Float): Float = dtx(x.toDouble(), y.toDouble()).toFloat()
private fun Matrix.dty(x: Float, y: Float): Float = dty(x.toDouble(), y.toDouble()).toFloat()

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
            approximateCurve(sum.toInt(), { ratio, get -> get(Bezier.cubicCalc(l, c0, c1, a, ratio)) }, { emit(it, false) })
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
    compute: (ratio: Ratio, get: (Point) -> Unit) -> Unit,
    crossinline emit: (Point) -> Unit,
    includeStart: Boolean = false,
    includeEnd: Boolean = true,
) {
    val rcurveSteps = max(curveSteps, 20)
    val dt = 1f / rcurveSteps
    var lastPos = Point()
    var prevPos = Point()
    var emittedCount = 0
    compute(Ratio.ZERO) { lastPos = it }
    val nStart = if (includeStart) 0 else 1
    val nEnd = if (includeEnd) rcurveSteps else rcurveSteps - 1
    for (n in nStart .. nEnd) {
        val ratio = Ratio(n * dt)
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
            Ratio.forEachRatio(curveSteps, include0 = false) {
                emit(Bezier.quadCalc(l, c, a, it))
            }
            l = a
        },
        cubicTo = { c1,c2, a ->
            Ratio.forEachRatio(curveSteps, include0 = false) {
                emit(Bezier.cubicCalc(l, c1, c2, a, it))
            }
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

interface Shape3D {
    val center: Vector3F
    val volume: Float
}

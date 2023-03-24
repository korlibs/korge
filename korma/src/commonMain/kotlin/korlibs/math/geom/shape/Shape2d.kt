package korlibs.math.geom.shape

import korlibs.datastructure.*
import korlibs.datastructure.iterators.*
import korlibs.math.geom.*
import korlibs.math.geom.bezier.*
import korlibs.math.geom.vector.*
import korlibs.math.internal.*
import kotlin.math.*

private fun Matrix.tx(x: Double, y: Double): Double = if (this.isNotNIL) this.transformX(x, y) else x
private fun Matrix.ty(x: Double, y: Double): Double = if (this.isNotNIL) this.transformY(x, y) else y
private fun Matrix.dtx(x: Double, y: Double): Double = if (this.isNotNIL) this.deltaTransform(Point(x, y)).x.toDouble() else x
private fun Matrix.dty(x: Double, y: Double): Double = if (this.isNotNIL) this.deltaTransform(Point(x, y)).y.toDouble() else y

private fun optimizedIntersect(l: Shape2d.Circle, r: Shape2d.Circle): Boolean =
    MPoint.distance(l.x, l.y, r.x, r.y) < (l.radius + r.radius)

private fun optimizedIntersect(l: Shape2d.Circle, ml: Matrix, r: Shape2d.Circle, mr: Matrix): Boolean {
    if (ml.isNIL && mr.isNIL) return optimizedIntersect(l, r)
    val radiusL = ml.dtx(l.radius, l.radius)
    val radiusR = mr.dtx(r.radius, r.radius)
    //println("radiusL=$radiusL, radiusR=$radiusR")
    return MPoint.distance(
        ml.tx(l.x, l.y), ml.ty(l.x, l.y),
        mr.tx(r.x, r.y), mr.ty(r.x, r.y),
    ) < radiusL + radiusR
}

interface WithHitShape2d {
    val hitShape2d: Shape2d
}

abstract class Shape2d {
    abstract val type: Int
    abstract val paths: List<PointList>
    abstract val closed: Boolean
    abstract fun containsPoint(p: Point): Boolean
    fun containsPoint(p: Point, mat: Matrix) = containsPoint(mat.transform(p))
    open fun getBounds(out: MRectangle = MRectangle()): MRectangle {
        var minx = Double.POSITIVE_INFINITY
        var miny = Double.POSITIVE_INFINITY
        var maxx = Double.NEGATIVE_INFINITY
        var maxy = Double.NEGATIVE_INFINITY
        paths.fastForEach { path ->
            path.fastForEach { (x, y) ->
                minx = min(minx, x.toDouble())
                miny = min(miny, y.toDouble())
                maxx = max(maxx, x.toDouble())
                maxy = max(maxy, y.toDouble())
            }
        }
        return out.setBounds(minx, miny, maxx, maxy)
    }

    open val center: Point get() = getBounds().center

    companion object {
        fun intersects(l: Shape2d, ml: Matrix, r: Shape2d, mr: Matrix): Boolean {
            //println("Shape2d.intersects:"); println(" - l=$l[$ml]"); println(" - r=$r[$mr]")

            if (l.type == r.type) {
                when (l.type) {
                    BaseEllipse.TYPE -> {
                        if (l is Circle && r is Circle) {
                            return optimizedIntersect(l, ml, r, mr)
                        }
                    }
                }
            }

            return _intersectsStep0(l, ml, r, mr) || _intersectsStep0(r, mr, l, ml)
        }

        private fun _intersectsStep0(l: Shape2d, ml: Matrix, r: Shape2d, mr: Matrix): Boolean {
            if ((ml.isNotNIL || mr.isNotNIL)) {
                var tempMatrix = if (mr.isNotNIL) mr.inverted() else Matrix.IDENTITY
                if (ml.isNotNIL) tempMatrix = tempMatrix.premultiplied(ml)

                l.paths.fastForEach {
                    it.fastForEach { p ->
                        if (r.containsPoint(tempMatrix.transform(p))) return true
                    }
                }
            } else {
                l.paths.fastForEach {
                    it.fastForEach { p ->
                        if (r.containsPoint(p)) return true
                    }
                }
            }
            return false
        }

        fun intersects(l: Shape2d, r: Shape2d): Boolean = intersects(l, Matrix.NIL, r, Matrix.NIL)

        fun EllipseOrCircle(x: Double, y: Double, radiusX: Double, radiusY: Double, angle: Angle = Angle.ZERO, totalPoints: Int = 32): BaseEllipse =
            if (radiusX == radiusY) Circle(x, y, radiusX, totalPoints) else Ellipse(x, y, radiusX, radiusY, angle, totalPoints)
    }

    fun intersectsWith(that: Shape2d) = intersects(this, Matrix.NIL, that, Matrix.NIL)
    fun intersectsWith(ml: Matrix, that: Shape2d, mr: Matrix) = intersects(this, ml, that, mr)

    infix fun with(that: Shape2d): Shape2d {
        val left = this
        val right = that
        if (left is Empty) return right
        if (right is Empty) return left
        return Complex(buildFastList {
            if (left is Complex) addAll(left.items) else add(left)
            if (right is Complex) addAll(right.items) else add(right)
        })
    }

    interface WithArea {
        val area: Double
    }

    object Empty : Shape2d(), WithArea {
        const val TYPE = 0
        override val type: Int = TYPE
        override val paths = listOf(PointArrayList(0))
        override val closed = false
        override val area = 0.0
        override fun containsPoint(p: Point) = false
    }

    data class Line(val x0: Double, val y0: Double, val x1: Double, val y1: Double) : Shape2d(), WithArea {
        companion object {
            const val TYPE = 1
            operator fun invoke(x0: Float, y0: Float, x1: Float, y1: Float) = Line(x0.toDouble(), y0.toDouble(), x1.toDouble(), y1.toDouble())
            operator fun invoke(x0: Int, y0: Int, x1: Int, y1: Int) = Line(x0.toDouble(), y0.toDouble(), x1.toDouble(), y1.toDouble())
        }

        override val type: Int = TYPE
        override val paths = listOf(PointArrayList(2).apply { add(x0, y0).add(x1, y1) })
        override val closed = false
        override val area get() = 0.0
        override fun containsPoint(p: Point) = false
    }

    // @TODO: Ellipse
    // https://en.wikipedia.org/wiki/Matrix_representation_of_conic_sections
    // https://math.stackexchange.com/questions/425366/finding-intersection-of-an-ellipse-with-another-ellipse-when-both-are-rotated/425412#425412
    abstract class BaseEllipse(val ellipseX: Double, val ellipseY: Double, val ellipseRadiusX: Double, val ellipseRadiusY: Double = ellipseRadiusX, val ellipseAngle: Angle = Angle.ZERO, val ellipseTotalPoints: Int = 32) : Shape2d(), WithArea {
        companion object {
            const val TYPE = -2
        }
        override val type: Int get() = TYPE

        val isCircle get() = ellipseRadiusX == ellipseRadiusY
        val vectorPath by lazy {
            buildVectorPath(VectorPath()) {
                ellipse(Point(0.0, 0.0), Size(ellipseRadiusX, ellipseRadiusY))
            }.applyTransform(Matrix().pretranslated(ellipseX, ellipseY).prerotated(ellipseAngle))
        }

        override val paths: List<PointList> = when {
            isCircle -> listOf(PointArrayList(ellipseTotalPoints) {
                for (it in 0 until ellipseTotalPoints) {
                    add(
                        ellipseX + Angle.cos01(it.toDouble() / ellipseTotalPoints.toDouble()) * ellipseRadiusX,
                        ellipseY + Angle.sin01(it.toDouble() / ellipseTotalPoints.toDouble()) * ellipseRadiusY
                    )
                }
            })
            else -> listOf(vectorPath.getPoints2())
        }
        override val closed: Boolean get() = true
        override fun containsPoint(p: Point): Boolean {
            if (isCircle) {
                return hypot(this.ellipseX - p.xD, this.ellipseY - p.yD) < ellipseRadiusX
            }
            return vectorPath.containsPoint(p)
        }
        override val area: Double get() = PI.toDouble() * ellipseRadiusX * ellipseRadiusY
    }

    data class Ellipse(val x: Double, val y: Double, val radiusX: Double, val radiusY: Double, val angle: Angle = Angle.ZERO, val totalPoints: Int = 32) : BaseEllipse(x, y, radiusX, radiusY, angle, totalPoints) {
        companion object {
            operator fun invoke(x: Float, y: Float, radiusX: Float, radiusY: Float, angle: Angle = Angle.ZERO, totalPoints: Int = 32) = Ellipse(x.toDouble(), y.toDouble(), radiusX.toDouble(), radiusY.toDouble(), angle, totalPoints)
            operator fun invoke(x: Int, y: Int, radiusX: Int, radiusY: Int, angle: Angle = Angle.ZERO, totalPoints: Int = 32) = Ellipse(x.toDouble(), y.toDouble(), radiusX.toDouble(), radiusY.toDouble(), angle, totalPoints)
        }
    }
    data class Circle(val x: Double, val y: Double, override val radius: Double, val totalPoints: Int = 32) : BaseEllipse(x, y, radius, radius, Angle.ZERO, totalPoints), ICircle {
        override val center: Point = Point(x, y)
        companion object {
            operator fun invoke(x: Float, y: Float, radius: Float, totalPoints: Int = 32) = Circle(x.toDouble(), y.toDouble(), radius.toDouble(), totalPoints)
            operator fun invoke(x: Int, y: Int, radius: Int, totalPoints: Int = 32) = Circle(x.toDouble(), y.toDouble(), radius.toDouble(), totalPoints)
        }
    }

    data class Rectangle(val rect: MRectangle) : Shape2d(), WithArea {
        val x: Double by rect::x
        val y: Double by rect::y
        val width: Double by rect::width
        val height: Double by rect::height
        val left: Double by rect::left
        val right: Double by rect::right
        val top: Double by rect::top
        val bottom: Double by rect::bottom

        companion object {
            const val TYPE = 3
            inline operator fun invoke(x: Double, y: Double, width: Double, height: Double) = Rectangle(MRectangle(x, y, width, height))
            inline operator fun invoke(x: Float, y: Float, width: Float, height: Float) = Rectangle(MRectangle(x, y, width, height))
            inline operator fun invoke(x: Int, y: Int, width: Int, height: Int) = Rectangle(MRectangle(x, y, width, height))

            inline fun fromBounds(left: Double, top: Double, right: Double, down: Double) = Rectangle(MRectangle.fromBounds(left, top, right, down))
            inline fun fromBounds(left: Float, top: Float, right: Float, down: Float) = Rectangle(MRectangle.fromBounds(left, top, right, down))
            inline fun fromBounds(left: Int, top: Int, right: Int, down: Int) = Rectangle(MRectangle.fromBounds(left, top, right, down))
        }

        override val type: Int = TYPE
        override val paths = listOf(PointArrayList(4) { add(x, y).add(x + width, y).add(x + width, y + height).add(x, y + height) })
        override val closed: Boolean = true
        override val area: Double get() = width * height

        override fun containsPoint(p: Point) = (p.xD in this.left..this.right) && (p.yD in this.top..this.bottom)
        override fun toString(): String =
            "Rectangle(x=${x.niceStr}, y=${y.niceStr}, width=${width.niceStr}, height=${height.niceStr})"
    }

    data class Path(val vectorPath: VectorPath, override val closed: Boolean = true) : Shape2d() {
        companion object {
            const val TYPE = 4
        }
        override val type: Int = TYPE
        override val paths = listOf(vectorPath.getPoints2())
        override fun containsPoint(p: Point): Boolean = if (closed) vectorPath.containsPoint(p) else false
    }

    data class Polygon(val points: PointList) : Shape2d() {
        companion object {
            const val TYPE = 5
        }
        override val type: Int = TYPE
        override val paths = listOf(points)
        override val closed: Boolean = true
        val vectorPath by lazy {
            buildVectorPath(VectorPath()) {
                polygon(points)
            }
        }
        override fun containsPoint(p: Point): Boolean = vectorPath.containsPoint(p)
    }

    data class Polyline(val points: PointList) : Shape2d(), WithArea {
        companion object {
            const val TYPE = 6
        }
        override val type: Int = TYPE
        override val paths = listOf(points)
        override val closed: Boolean = false
        override val area: Double get() = 0.0
        override fun containsPoint(p: Point) = false
    }

    data class Complex(val items: List<Shape2d>) : Shape2d() {
        companion object {
            const val TYPE = 7
        }
        override val type: Int = TYPE
        override val paths by lazy { items.flatMap { it.paths } }
        override val closed: Boolean = false
        override fun containsPoint(p: Point): Boolean {
            items.fastForEach { if (it.containsPoint(p)) return true }
            return false
        }
    }
}

fun Iterable<VectorPath>.toShape2d(closed: Boolean = true) = Shape2d.Complex(this.map { it.toShape2d(closed) })

val List<PointList>.totalVertices get() = this.map { it.size }.sum()

fun BoundsBuilder.add(shape: Shape2d) {
    for (path in shape.paths) add(path)
}

val Shape2d.bounds: MRectangle get() = BoundsBuilder().apply { add(this@bounds) }.getBounds()

fun MRectangle.toShape() = Shape2d.Rectangle(x, y, width, height)

// @TODO: Instead of use curveSteps, let's determine the maximum distance between points for the curve, or the maximum angle (so we have a quality factor instead)
inline fun VectorPath.emitPoints(flush: (close: Boolean) -> Unit, emit: (Point) -> Unit, curveSteps: Int = 20) {
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

fun VectorPath.getPoints2(out: PointArrayList = PointArrayList()): PointArrayList {
    emitPoints2 { p, move -> out.add(p) }
    return out
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

inline fun buildVectorPath(out: VectorPath = VectorPath(), block: VectorPath.() -> Unit): VectorPath = out.apply(block)
inline fun buildVectorPath(out: VectorPath = VectorPath(), winding: Winding = Winding.DEFAULT, block: VectorPath.() -> Unit): VectorPath = out.also { it.winding = winding }.apply(block)

fun PointList.toPolygon(out: VectorPath = VectorPath()): VectorPath = buildVectorPath(out) { polygon(this@toPolygon) }

inline fun approximateCurve(
    curveSteps: Int,
    crossinline compute: (ratio: Double, get: (Point) -> Unit) -> Unit,
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

fun PointList.toRectangleOrNull(): Shape2d.Rectangle? {
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
    return Shape2d.Rectangle(MRectangle.fromBounds(top, left, right, bottom))
}

fun PointList.toShape2d(closed: Boolean = true): Shape2d {
    if (closed && this.size == 4) {
        val x0 = this.getX(0)
        val y0 = this.getY(0)
        val x1 = this.getX(2)
        val y1 = this.getY(2)
        if (this.getX(1) == x1 && this.getY(1) == y0 && this.getX(3) == x0 && this.getY(3) == y1) {
            return Shape2d.Rectangle(MRectangle.fromBounds(x0, y0, x1, y1))
        }
    }
    return if (closed) Shape2d.Polygon(this) else Shape2d.Polyline(this)
}

fun VectorPath.toShape2dNew(closed: Boolean = true): Shape2d = Shape2d.Path(this, closed)

//fun VectorPath.toShape2d(closed: Boolean = true): Shape2d = toShape2dNew(closed)
fun VectorPath.toShape2d(closed: Boolean = true): Shape2d = toShape2dOld(closed)

fun VectorPath.toShape2dOld(closed: Boolean = true): Shape2d {
    val items = toPathPointList().map { it.toShape2d(closed) }
    return when (items.size) {
        0 -> Shape2d.Empty
        1 -> items.first()
        else -> Shape2d.Complex(items)
    }
}

fun VectorPath.toPathPointList(m: MMatrix? = null, emitClosePoint: Boolean = false): List<PointList> {
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

fun Shape2d.getAllPoints(out: PointArrayList = PointArrayList()): PointArrayList = out.apply { for (path in this@getAllPoints.paths) add(path) }
fun Shape2d.toPolygon(): Shape2d.Polygon = if (this is Shape2d.Polygon) this else Shape2d.Polygon(this.getAllPoints())

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
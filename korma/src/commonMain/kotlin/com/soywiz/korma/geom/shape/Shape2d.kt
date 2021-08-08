package com.soywiz.korma.geom.shape

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.bezier.*
import com.soywiz.korma.geom.vector.*
import com.soywiz.korma.internal.*
import kotlin.math.*

private fun Matrix?.tx(x: Double, y: Double) = this?.transformX(x, y) ?: x
private fun Matrix?.ty(x: Double, y: Double) = this?.transformY(x, y) ?: y
private fun Matrix?.dtx(x: Double, y: Double) = this?.deltaTransformX(x, y) ?: x
private fun Matrix?.dty(x: Double, y: Double) = this?.deltaTransformY(x, y) ?: y

private fun optimizedIntersect(l: Shape2d.Circle, r: Shape2d.Circle): Boolean {
    return Point.distance(l.x, l.y, r.x, r.y) < (l.radius + r.radius)
}

private fun optimizedIntersect(l: Shape2d.Circle, ml: Matrix?, r: Shape2d.Circle, mr: Matrix?): Boolean {
    if (ml == null && mr == null) return optimizedIntersect(l, r)
    val radiusL = ml.dtx(l.radius, l.radius)
    val radiusR = mr.dtx(r.radius, r.radius)
    //println("radiusL=$radiusL, radiusR=$radiusR")
    return Point.distance(
        ml.tx(l.x, l.y), ml.ty(l.x, l.y),
        mr.tx(r.x, r.y), mr.ty(r.x, r.y),
    ) < radiusL + radiusR
}

interface WithHitShape2d {
    val hitShape2d: Shape2d
}

abstract class Shape2d {
    abstract val type: Int
    abstract val paths: List<IPointArrayList>
    abstract val closed: Boolean
    abstract fun containsPoint(x: Double, y: Double): Boolean
    fun containsPoint(x: Double, y: Double, mat: Matrix) = containsPoint(mat.transformX(x, y), mat.transformY(x, y))
    open fun getBounds(out: com.soywiz.korma.geom.Rectangle = com.soywiz.korma.geom.Rectangle()): com.soywiz.korma.geom.Rectangle {
        var minx = Double.POSITIVE_INFINITY
        var miny = Double.POSITIVE_INFINITY
        var maxx = Double.NEGATIVE_INFINITY
        var maxy = Double.NEGATIVE_INFINITY
        paths.fastForEach { path ->
            path.fastForEach { x, y ->
                minx = min(minx, x)
                miny = min(miny, y)
                maxx = max(maxx, x)
                maxy = max(maxy, y)
            }
        }
        return out.setBounds(minx, miny, maxx, maxy)
    }
    open fun getCenter(): com.soywiz.korma.geom.Point {
        return getBounds().center
    }
    companion object {
        fun intersects(l: Shape2d, ml: Matrix?, r: Shape2d, mr: Matrix?, tempMatrix: Matrix? = Matrix()): Boolean {
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

            return _intersectsStep0(l, ml, r, mr, tempMatrix) || _intersectsStep0(r, mr, l, ml, tempMatrix)
        }

        private fun _intersectsStep0(l: Shape2d, ml: Matrix?, r: Shape2d, mr: Matrix?, tempMatrix: Matrix? = Matrix()): Boolean {
            if (tempMatrix != null && (ml != null || mr != null)) {
                if (mr != null) tempMatrix.invert(mr) else tempMatrix.identity()
                if (ml != null) tempMatrix.premultiply(ml)

                l.paths.fastForEach {
                    it.fastForEach { x, y ->
                        val tx = tempMatrix.transformX(x, y)
                        val ty = tempMatrix.transformY(x, y)
                        if (r.containsPoint(tx, ty)) return true
                    }
                }
            } else {
                l.paths.fastForEach {
                    it.fastForEach { x, y ->
                        if (r.containsPoint(x, y)) return true
                    }
                }
            }
            return false
        }

        fun intersects(l: Shape2d, r: Shape2d): Boolean = intersects(l, null, r, null, null)

        fun EllipseOrCircle(x: Double, y: Double, radiusX: Double, radiusY: Double, angle: Angle = Angle.ZERO, totalPoints: Int = 32): BaseEllipse =
            if (radiusX == radiusY) Circle(x, y, radiusX, totalPoints) else Ellipse(x, y, radiusX, radiusY, angle, totalPoints)
    }

    fun intersectsWith(that: Shape2d) = intersects(this, null, that, null, null)
    fun intersectsWith(ml: Matrix?, that: Shape2d, mr: Matrix?) = intersects(this, ml, that, mr)

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
        override fun containsPoint(x: Double, y: Double) = false
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
        override fun containsPoint(x: Double, y: Double) = false
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
        val vectorPath by lazy { buildPath { ellipse(0.0, 0.0, ellipseRadiusX, ellipseRadiusY) }.applyTransform(Matrix().pretranslate(ellipseX, ellipseY).prerotate(ellipseAngle)) }

        override val paths: List<IPointArrayList> = when {
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
        override fun containsPoint(x: Double, y: Double): Boolean {
            if (isCircle) {
                return hypot(this.ellipseX - x, this.ellipseY - y) < ellipseRadiusX
            }
            return vectorPath.containsPoint(x, y)
        }
        override val area: Double get() = PI.toDouble() * ellipseRadiusX * ellipseRadiusY
    }

    data class Ellipse(val x: Double, val y: Double, val radiusX: Double, val radiusY: Double, val angle: Angle = Angle.ZERO, val totalPoints: Int = 32) : BaseEllipse(x, y, radiusX, radiusY, angle, totalPoints) {
        companion object {
            operator fun invoke(x: Float, y: Float, radiusX: Float, radiusY: Float, angle: Angle = Angle.ZERO, totalPoints: Int = 32) = Ellipse(x.toDouble(), y.toDouble(), radiusX.toDouble(), radiusY.toDouble(), angle, totalPoints)
            operator fun invoke(x: Int, y: Int, radiusX: Int, radiusY: Int, angle: Angle = Angle.ZERO, totalPoints: Int = 32) = Ellipse(x.toDouble(), y.toDouble(), radiusX.toDouble(), radiusY.toDouble(), angle, totalPoints)
        }
    }
    data class Circle(val x: Double, val y: Double, val radius: Double, val totalPoints: Int = 32) : BaseEllipse(x, y, radius, radius, Angle.ZERO, totalPoints) {
        companion object {
            operator fun invoke(x: Float, y: Float, radius: Float, totalPoints: Int = 32) = Circle(x.toDouble(), y.toDouble(), radius.toDouble(), totalPoints)
            operator fun invoke(x: Int, y: Int, radius: Int, totalPoints: Int = 32) = Circle(x.toDouble(), y.toDouble(), radius.toDouble(), totalPoints)
        }
    }

    data class Rectangle(val rect: com.soywiz.korma.geom.Rectangle) : Shape2d(), WithArea, IRectangle by rect {
        companion object {
            const val TYPE = 3
            inline operator fun invoke(x: Double, y: Double, width: Double, height: Double) = Rectangle(com.soywiz.korma.geom.Rectangle(x, y, width, height))
            inline operator fun invoke(x: Float, y: Float, width: Float, height: Float) = Rectangle(com.soywiz.korma.geom.Rectangle(x, y, width, height))
            inline operator fun invoke(x: Int, y: Int, width: Int, height: Int) = Rectangle(com.soywiz.korma.geom.Rectangle(x, y, width, height))

            inline fun fromBounds(left: Double, top: Double, right: Double, down: Double) = Rectangle(com.soywiz.korma.geom.Rectangle.fromBounds(left, top, right, down))
            inline fun fromBounds(left: Float, top: Float, right: Float, down: Float) = Rectangle(com.soywiz.korma.geom.Rectangle.fromBounds(left, top, right, down))
            inline fun fromBounds(left: Int, top: Int, right: Int, down: Int) = Rectangle(com.soywiz.korma.geom.Rectangle.fromBounds(left, top, right, down))
        }

        override val type: Int = TYPE
        override val paths = listOf(PointArrayList(4) { add(x, y).add(x + width, y).add(x + width, y + height).add(x, y + height) })
        override val closed: Boolean = true
        override val area: Double get() = width * height
        override fun containsPoint(x: Double, y: Double) = (x in this.left..this.right) && (y in this.top..this.bottom)
        override fun toString(): String =
            "Rectangle(x=${x.niceStr}, y=${y.niceStr}, width=${width.niceStr}, height=${height.niceStr})"
    }

    data class Path(val vectorPath: VectorPath, override val closed: Boolean = true) : Shape2d() {
        companion object {
            const val TYPE = 4
        }
        override val type: Int = TYPE
        override val paths = listOf(vectorPath.getPoints2())
        override fun containsPoint(x: Double, y: Double): Boolean = if (closed) vectorPath.containsPoint(x, y) else false
    }

    data class Polygon(val points: IPointArrayList) : Shape2d() {
        companion object {
            const val TYPE = 5
        }
        override val type: Int = TYPE
        override val paths = listOf(points)
        override val closed: Boolean = true
        val vectorPath by lazy { buildPath { polygon(points) } }
        override fun containsPoint(x: Double, y: Double): Boolean = vectorPath.containsPoint(x, y)
    }

    data class Polyline(val points: IPointArrayList) : Shape2d(), WithArea {
        companion object {
            const val TYPE = 6
        }
        override val type: Int = TYPE
        override val paths = listOf(points)
        override val closed: Boolean = false
        override val area: Double get() = 0.0
        override fun containsPoint(x: Double, y: Double) = false
    }

    data class Complex(val items: List<Shape2d>) : Shape2d() {
        companion object {
            const val TYPE = 7
        }
        override val type: Int = TYPE
        override val paths by lazy { items.flatMap { it.paths } }
        override val closed: Boolean = false
        override fun containsPoint(x: Double, y: Double): Boolean {
            items.fastForEach { if (it.containsPoint(x, y)) return true }
            return false
        }
    }
}

fun Iterable<VectorPath>.toShape2d(closed: Boolean = true) = Shape2d.Complex(this.map { it.toShape2d(closed) })

val List<IPointArrayList>.totalVertices get() = this.map { it.size }.sum()

fun BoundsBuilder.add(shape: Shape2d) {
    for (path in shape.paths) add(path)
}

val Shape2d.bounds: Rectangle get() = BoundsBuilder().apply { add(this@bounds) }.getBounds()

fun IRectangle.toShape() = Shape2d.Rectangle(x, y, width, height)

// @TODO: Instead of use curveSteps, let's determine the maximum distance between points for the curve, or the maximum angle (so we have a quality factor instead)
inline fun VectorPath.emitPoints(flush: (close: Boolean) -> Unit, emit: (x: Double, y: Double) -> Unit, curveSteps: Int = 20) {
    var lx = 0.0
    var ly = 0.0
    flush(false)
    this.visitCmds(
        moveTo = { x, y -> emit(x, y).also { lx = x }.also { ly = y } },
        lineTo = { x, y -> emit(x, y).also { lx = x }.also { ly = y } },
        quadTo = { x0, y0, x1, y1 ->
            val dt = 1.0 / curveSteps
            for (n in 1 until curveSteps) Bezier.quadCalc(lx, ly, x0, y0, x1, y1, n * dt, emit)
            run { lx = x1 }.also { ly = y1 }
        },
        cubicTo = { x0, y0, x1, y1, x2, y2 ->
            val dt = 1.0 / curveSteps
            for (n in 1 until curveSteps) Bezier.cubicCalc(lx, ly, x0, y0, x1, y1, x2, y2, n * dt, emit)
            run { lx = x2 }.also { ly = y2 }

        },
        close = { flush(true) }
    )
    flush(false)
}

inline fun VectorPath.emitEdges(
    crossinline edge: (x0: Double, y0: Double, x1: Double, y1: Double) -> Unit
) {
    var firstX = 0.0
    var firstY = 0.0
    var lastX = 0.0
    var lastY = 0.0

    emitPoints2(
        flush = { close ->
            if (close) {
                edge(lastX, lastY, firstX, firstY)
                lastX = firstX
                lastY = firstY
            }
        },
        emit = { x, y, move ->
            if (move) {
                firstX = x
                firstY = y
            } else {
                edge(lastX, lastY, x, y)
            }
            lastX = x
            lastY = y
        }
    )
}

inline fun VectorPath.emitPoints2(
    crossinline flush: (close: Boolean) -> Unit = {},
    crossinline joint: (close: Boolean) -> Unit = {},
    crossinline emit: (x: Double, y: Double, move: Boolean) -> Unit
) {
    var ix = 0.0
    var iy = 0.0
    var lx = 0.0
    var ly = 0.0
    flush(false)
    this.visitCmds(
        moveTo = { x, y ->
            ix = x
            iy = y
            emit(x, y, true).also { lx = x }.also { ly = y }
        },
        lineTo = { x, y ->
            emit(x, y, false).also { lx = x }.also { ly = y }
            joint(false)
        },
        quadTo = { x0, y0, x1, y1 ->
            val sum = Point.distance(lx, ly, x0, y0) + Point.distance(x0, y0, x1, y1)
            approximateCurve(sum.toInt(), { ratio, get -> Bezier.quadCalc(lx, ly, x0, y0, x1, y1, ratio) { x, y -> get(x, y) } }) { x, y -> emit(x, y, false) }
            run { lx = x1 }.also { ly = y1 }
            joint(false)
        },
        cubicTo = { x0, y0, x1, y1, x2, y2 ->
            val sum = Point.distance(lx, ly, x0, y0) + Point.distance(x0, y0, x1, y1) + Point.distance(x1, y1, x2, y2)
            approximateCurve(sum.toInt(), { ratio, get -> Bezier.cubicCalc(lx, ly, x0, y0, x1, y1, x2, y2, ratio) { x, y -> get(x, y) }}) { x, y -> emit(x, y, false) }
            run { lx = x2 }.also { ly = y2 }
            joint(false)
        },
        close = {
            emit(ix, iy, false)
            joint(true)
            flush(true)
        }
    )
    flush(false)
}

fun VectorPath.getPoints2(out: PointArrayList = PointArrayList()): PointArrayList {
    emitPoints2 { x, y, move -> out.add(x, y) }
    return out
}

inline fun buildPath(out: VectorPath = VectorPath(), block: VectorPath.() -> Unit): VectorPath = out.apply(block)
inline fun buildPath(out: VectorPath = VectorPath(), winding: Winding = Winding.EVEN_ODD, block: VectorPath.() -> Unit): VectorPath = out.also { it.winding = winding }.apply(block)

inline fun approximateCurve(
    curveSteps: Int,
    crossinline compute: (ratio: Double, get: (x: Double, y: Double) -> Unit) -> Unit,
    crossinline emit: (x: Double, y: Double) -> Unit
) {
    val rcurveSteps = max(curveSteps, 20)
    val dt = 1.0 / rcurveSteps
    var lastX = 0.0
    var lastY = 0.0
    var prevX = 0.0
    var prevY = 0.0
    var emittedCount = 0
    compute(0.0) { x, y ->
        lastX = x
        lastY = y
    }
    for (n in 1 until rcurveSteps) {
        val ratio = n * dt
        //println("ratio: $ratio")
        compute(ratio) { x, y ->
            //if (emittedCount == 0) {
            run {
                emit(x, y)
                emittedCount++
                lastX = prevX
                lastY = prevY
            }

            prevX = x
            prevY = y
        }
    }
    //println("curveSteps: $rcurveSteps, emittedCount=$emittedCount")
}

fun IPointArrayList.toRectangleOrNull(): Shape2d.Rectangle? {
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
    return Shape2d.Rectangle(Rectangle.fromBounds(top, left, right, bottom))
}

fun IPointArrayList.toShape2d(closed: Boolean = true): Shape2d {
    if (closed && this.size == 4) {
        val x0 = this.getX(0)
        val y0 = this.getY(0)
        val x1 = this.getX(2)
        val y1 = this.getY(2)
        if (this.getX(1) == x1 && this.getY(1) == y0 && this.getX(3) == x0 && this.getY(3) == y1) {
            return Shape2d.Rectangle(Rectangle.fromBounds(x0, y0, x1, y1))
        }
    }
    return if (closed) Shape2d.Polygon(this) else Shape2d.Polyline(this)
}

fun VectorPath.toShape2dNew(closed: Boolean = true): Shape2d = Shape2d.Path(this, closed)

//fun VectorPath.toShape2d(closed: Boolean = true): Shape2d = toShape2dNew(closed)
fun VectorPath.toShape2d(closed: Boolean = true): Shape2d = toShape2dOld(closed)

fun VectorPath.toShape2dOld(closed: Boolean = true): Shape2d {
    val items = toPathList().map { it.toShape2d(closed) }
    return when (items.size) {
        0 -> Shape2d.Empty
        1 -> items.first()
        else -> Shape2d.Complex(items)
    }
}

fun VectorPath.toPathList(): List<IPointArrayList> {
    val paths = arrayListOf<IPointArrayList>()
    var path = PointArrayList()
    emitPoints({
        if (path.isNotEmpty()) {
            //if (path.getX(0) == path.getX(path.size - 1) && path.getY(0) == path.getY(path.size - 1)) path.removeAt(path.size - 1)
            //println("POINTS:" + path.size)
            //for (p in path.toPoints()) println(" - $p")
            paths += path
            path = PointArrayList()
        }
    }, { x, y ->
        path.add(x, y)
    })
    return paths
}

fun Shape2d.getAllPoints(out: PointArrayList = PointArrayList()): PointArrayList = out.apply { for (path in this@getAllPoints.paths) add(path) }
fun Shape2d.toPolygon(): Shape2d.Polygon = if (this is Shape2d.Polygon) this else Shape2d.Polygon(this.getAllPoints())

fun List<IPoint>.containsPoint(x: Double, y: Double): Boolean {
    var intersections = 0
    for (n in 0 until this.size - 1) {
        val p1 = this[n + 0]
        val p2 = this[n + 1]
        intersections += HorizontalLine.intersectionsWithLine(x, y, p1.x, p1.y, p2.x, p2.y)
    }
    return (intersections % 2) != 0
}

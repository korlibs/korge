@file:Suppress("PackageDirectoryMismatch")

package korlibs.math.geom.collider

import korlibs.datastructure.iterators.*
import korlibs.math.geom.*
import korlibs.math.geom.vector.*

fun interface HitTestable {
    fun hitTestAny(p: Point, direction: HitTestDirection): Boolean
}

inline class HitTestDirectionFlags(val value: Int) {
    operator fun plus(that: HitTestDirectionFlags): HitTestDirectionFlags = HitTestDirectionFlags(this.value or that.value)

    constructor(up: Boolean, right: Boolean, down: Boolean, left: Boolean) : this(
        0.insert(up, 0).insert(right, 1).insert(down, 2).insert(left, 3)
    )

    companion object {
        val ALL = HitTestDirectionFlags(true, true, true, true)
        val NONE = HitTestDirectionFlags(false, false, false, false)

        fun fromString(kind: String?, default: HitTestDirectionFlags = ALL): HitTestDirectionFlags {
            if (kind == null || kind == "") return default
            if (!kind.startsWith("collision")) return NONE
            if (kind == "collision") return ALL
            return HitTestDirectionFlags(kind.contains("_up"), kind.contains("_right"), kind.contains("_down"), kind.contains("_left"))
        }
    }

    val any: Boolean get() = value != 0
    val all: Boolean get() = up && right && down && left
    val up: Boolean get() = value.extract(0)
    val right: Boolean get() = value.extract(1)
    val down: Boolean get() = value.extract(2)
    val left: Boolean get() = value.extract(3)

    fun matches(direction: HitTestDirection) = when (direction) {
        HitTestDirection.ANY -> any
        HitTestDirection.UP -> up
        HitTestDirection.RIGHT -> right
        HitTestDirection.DOWN -> down
        HitTestDirection.LEFT -> left
    }

    override fun toString(): String = "HitTestDirectionFlags(up=$up, right=$right, down=$down, left=$left)"
}

enum class HitTestDirection {
    ANY, UP, RIGHT, DOWN, LEFT;

    val up get() = this == ANY || this == UP
    val right get() = this == ANY || this == RIGHT
    val down get() = this == ANY || this == DOWN
    val left get() = this == ANY || this == LEFT

    companion object {
        fun fromPoint(point: Point): HitTestDirection {
            if (point.x == 0.0 && point.y == 0.0) return ANY
            return fromAngle(Point.ZERO.angleTo(point))
        }
        fun fromAngle(angle: Angle): HitTestDirection {
            val quadrant = ((angle + 45.degrees) / 90.degrees).toInt()
            return when (quadrant) {
                0 -> HitTestDirection.RIGHT
                1 -> HitTestDirection.DOWN
                2 -> HitTestDirection.LEFT
                3 -> HitTestDirection.UP
                else -> HitTestDirection.RIGHT
            }
        }
    }
}

fun VectorPath.toHitTestable(testDirections: HitTestDirectionFlags): HitTestable = HitTestable { p, direction ->
    testDirections.matches(direction) && containsPoint(p)
}

fun List<HitTestable>.toHitTestable(): HitTestable {
    val list = this
    return object : HitTestable {
        override fun hitTestAny(p: Point, direction: HitTestDirection): Boolean {
            list.fastForEach { if (it.hitTestAny(p, direction)) return true }
            return false
        }
    }
}

private fun Int.extract(offset: Int): Boolean = ((this ushr offset) and 1) != 0
private fun Int.insert(value: Boolean, offset: Int): Int {
    val bits = (1 shl offset)
    return if (value) this or bits else this and bits.inv()
}

/*
import korlibs.datastructure.*
import korlibs.datastructure.iterators.*
import korlibs.math.geom.*
import korlibs.math.geom.shape.*
import korlibs.math.geom.vector.*
import korlibs.math.math.*

abstract class MovementCollider {
    val tempLine = Line()
    val tempOut = Point()
    val tempLi = LineIntersection()

    abstract fun tryMove(line: Line, out: Point)
}

open class PathsCollider(val paths: List<VectorPath>) : MovementCollider() {
    //val scale = 100.0
    val scale = 1.0
    //val scale = 20.0
    //val scaledPaths = paths.map { it.clone().scale(scale).round() }
    val scaledPaths = paths.map { it.clone().scale(scale) }
    //val lines = scaledPaths.flatMap { it.getAllLines() }
    val lines = scaledPaths.flatMap {
        val edges = fastArrayListOf<Line>()
        it.emitEdges { x0, y0, x1, y1 ->
            edges.add(Line(x0, y0, x1, y1))
        }
        edges
    }
    private val tempPoint = Point()

    //fun containsPoint(x: Double, y: Double) = scaledPaths.any { it.containsPoint(x, y) }
    fun containsPoint(x: Double, y: Double) = paths.any { it.containsPoint(x / scale, y / scale) }
    fun containsPoint(p: MPoint) = containsPoint(p.x, p.y)

    var id = 0

    companion object {
        val checkDistances = doubleArrayOf(0.25, 0.5, 1.0, 2.0)
        val checkDeltas = doubleArrayOf(0.0, -1.0, +1.0)
    }

    override fun tryMove(mov: Line, out: Point) {
        val movA = Point().setToMul(mov.a, scale)//.round()
        val movB = Point().setToMul(mov.b, scale)//.round()
        val movement = Line(movA, movB)

        out.setToDiv(movA, scale)

        if (!containsPoint(movB)) {
            //println("NOT BLOCKED movA=$movA, movB=$movB, scaledPaths=$scaledPaths, lines=$lines")
            out.setToDiv(movB, scale)
            return
        }

        id++

        var nearestLine: Line? = null
        var minDistance = Double.POSITIVE_INFINITY

        lines.fastForEach { pathLine ->
            val dist = pathLine.getMinimumDistance(movB)
            if (dist < minDistance) {
                minDistance = dist
                nearestLine = pathLine
            }
            val intersection = pathLine.getIntersectionPoint(Line(movA, movB), tempPoint)
            if (intersection != null) {
                minDistance = Double.NEGATIVE_INFINITY
                nearestLine = pathLine
            }
        }

        val pathLine = nearestLine
        if (pathLine != null) {

            val vd = pathLine.directionVector().also { it.normalize() }
            val vecp = vd.x * movement.dx + vd.y * movement.dy
            val nd = Point(vd.x * vecp, vd.y * vecp)
            val tempPoint = movement.a + nd
            val tempPoint2 = Point()

            //tempPoint.round()

            //val normalAngle = pathLine.angle //+ 90.degrees
            //tempPoint.setToPolar(movement.a, normalAngle, movement.length)

            tempPoint2.copyFrom(tempPoint)
            if (!containsPoint(tempPoint2)) {
                //println("moved $id")
                out.setToDiv(tempPoint2, scale)
                return
            }

            for (dist in checkDistances) {
                for (dy in checkDeltas) {
                    for (dx in checkDeltas) {
                        tempPoint2.copyFrom(tempPoint)
                        tempPoint2.add(dx * dist, dy * dist)
                        if (!containsPoint(tempPoint2)) {
                            //println("moved $id")
                            out.setToDiv(tempPoint2, scale)
                            return
                        }
                    }
                }
            }

            println("forbidden movement $id : pathLine=$pathLine, movement=$movement, triedPoint=$tempPoint2")

            //println("pathLine=$pathLine, intersection=$intersection, normalVector=$normalVector")
            return
        }
        //println(nearestLine)

        /*
        val intersection = pathLine.getIntersectionPoint(movement, tempPoint)
        if (intersection != null) {

            val vd = pathLine.directionVector().also { it.normalize() }
            val vecp = vd.x * movement.dx + vd.y * movement.dy
            val nd = Point(vd.x * vecp, vd.y * vecp)
            val tempPoint = movement.a + nd

            //val normalAngle = pathLine.angle //+ 90.degrees
            //tempPoint.setToPolar(movement.a, normalAngle, movement.length)
            if (!containsPoint(tempPoint)) {
                println("moved $id")
                out.copyFrom(tempPoint)
            } else {
                println("forbidden movement $id")
            }

            //println("pathLine=$pathLine, intersection=$intersection, normalVector=$normalVector")
            return
        }
         */


        //println("no lines found $id")
    }
}

fun VectorPath.toCollider() = PathsCollider(listOf(this))
fun List<VectorPath>.toCollider() = PathsCollider(this)
fun Iterable<VectorPath>.toCollider() = PathsCollider(this.toList())

fun XY.moveWithCollider(dx: Double, dy: Double, collider: MovementCollider) {
    val tempOut = collider.tempOut
    val tempLine = collider.tempLine
    tempOut.copyFrom(this)
    tempLine.a.setTo(this.x, this.y)
    tempLine.b.setTo(this.x + dx, this.y + dy)
    collider.tryMove(tempLine, tempOut)
    if (tempOut.x.isNanOrInfinite() || tempOut.y.isNanOrInfinite()) {
        println("WARNING! NaN: $tempOut, $this -> $dx,$dy")
    }
    if (this.x != tempOut.x && !tempOut.x.isNanOrInfinite()) this.x = tempOut.x
    if (this.y != tempOut.y && !tempOut.y.isNanOrInfinite()) this.y = tempOut.y
}

fun XY.moveWithCollider(delta: MPoint, collider: MovementCollider) = moveWithCollider(delta.x, delta.y, collider)
*/

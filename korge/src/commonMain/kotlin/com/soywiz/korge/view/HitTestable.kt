package com.soywiz.korge.view

import com.soywiz.kds.iterators.*
import com.soywiz.kmem.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.shape.*
import com.soywiz.korma.geom.vector.*

fun interface HitTestable {
    fun hitTestAny(x: Double, y: Double, direction: HitTestDirection): Boolean
}

fun HitTestable.hitTestAny(x: Int, y: Int, direction: HitTestDirection): Boolean =
    this.hitTestAny(x.toDouble(), y.toDouble(), direction)

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
            return fromAngle(Point.Zero.angleTo(point))
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

fun HitTestable.hitTestAny(x: Double, y: Double) = hitTestAny(x, y, HitTestDirection.ANY)

fun VectorPath.toHitTestable(testDirections: HitTestDirectionFlags): HitTestable {
    return HitTestable { x, y, direction ->
        testDirections.matches(direction) && containsPoint(x, y)
    }
}

fun List<HitTestable>.toHitTestable(): HitTestable {
    val list = this
    return object : HitTestable {
        override fun hitTestAny(x: Double, y: Double, direction: HitTestDirection): Boolean {
            list.fastForEach { if (it.hitTestAny(x, y, direction)) return true }
            return false
        }
    }
}

private val MOVE_ANGLES = arrayOf(0.degrees, 5.degrees, 10.degrees, 15.degrees, 20.degrees, 30.degrees, 45.degrees, 60.degrees, 80.degrees, 85.degrees)
private val MOVE_SCALES = arrayOf(+1.0, -1.0)

// @TODO: if dx & dy are big, we should check intermediary positions to ensure we are not jumping to the other side of the object
fun View.moveWithHitTestable(collision: HitTestable, dx: Double, dy: Double, hitTestDirection: HitTestDirection? = null) {
    val char = this
    val deltaXY = Point(dx, dy)
    val angle = Angle.between(0.0, 0.0, deltaXY.x, deltaXY.y)
    val length = deltaXY.length
    val oldX = char.x
    val oldY = char.y
    MOVE_ANGLES.fastForEach { dangle ->
        MOVE_SCALES.fastForEach { dscale ->
            val rangle = angle + dangle * dscale
            val lengthScale = dangle.cosine
            val dpoint = Point.fromPolar(rangle, length * lengthScale)
            char.x = oldX + dpoint.x
            char.y = oldY + dpoint.y
            if (!collision.hitTestAny(
                char.globalX, char.globalY,
                hitTestDirection ?: HitTestDirection.fromAngle(angle))
            ) {
                return // Accept movement
            }
        }
    }
    char.x = oldX
    char.y = oldY
}

fun View.moveWithCollisions(collision: List<View>, dx: Double, dy: Double, kind: CollisionKind = CollisionKind.SHAPE) {
    val char = this
    val deltaXY = Point(dx, dy)
    val angle = Angle.between(0.0, 0.0, deltaXY.x, deltaXY.y)
    val length = deltaXY.length
    val oldX = char.x
    val oldY = char.y
    MOVE_ANGLES.fastForEach { dangle ->
        MOVE_SCALES.fastForEach { dscale ->
            val rangle = angle + dangle * dscale
            val lengthScale = dangle.cosine
            val dpoint = Point.fromPolar(rangle, length * lengthScale)
            char.x = oldX + dpoint.x
            char.y = oldY + dpoint.y
            //char.hitTestView(collision, kind)
            //if (!char.collidesWith(collision, kind)) {
            if (collision.all { it.hitTestView(char) == null }) {
            //if (char.hitTestView(collision) == null) {
                return // Accept movement
            }
        }
    }
    char.x = oldX
    char.y = oldY
}

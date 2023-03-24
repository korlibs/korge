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
        fun fromPoint(point: MPoint): HitTestDirection {
            if (point.x == 0.0 && point.y == 0.0) return ANY
            return fromAngle(MPoint.Zero.angleTo(point))
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

private inline fun Int.extract(offset: Int): Boolean = ((this ushr offset) and 0b1) != 0
private inline fun Int.insert(value: Boolean, offset: Int): Int = if (value) this or (1 shl offset) else this
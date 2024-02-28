package korlibs.math

import kotlin.math.roundToInt

////////////////////
////////////////////

/** Divides [this] into [that] rounding to the floor */
public infix fun Int.divFloor(that: Int): Int = this / that
/** Divides [this] into [that] rounding to the ceil */
public infix fun Int.divCeil(that: Int): Int = if (this % that != 0) (this / that) + 1 else (this / that)
/** Divides [this] into [that] rounding to the round */
public infix fun Int.divRound(that: Int): Int = (this.toDouble() / that.toDouble()).roundToInt()

public infix fun Long.divCeil(other: Long): Long {
    val res = this / other
    if (this % other != 0L) return res + 1
    return res
}

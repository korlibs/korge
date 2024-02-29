package korlibs.math

import kotlin.math.absoluteValue


////////////////////
////////////////////

/** Returns the next value of [this] that is multiple of [align]. If [this] is already multiple, returns itself. */
public fun Int.nextAlignedTo(align: Int): Int = if (this.isAlignedTo(align)) this else (((this / align) + 1) * align)
/** Returns the next value of [this] that is multiple of [align]. If [this] is already multiple, returns itself. */
public fun Long.nextAlignedTo(align: Long): Long = if (this.isAlignedTo(align)) this else (((this / align) + 1) * align)
/** Returns the next value of [this] that is multiple of [align]. If [this] is already multiple, returns itself. */
public fun Float.nextAlignedTo(align: Float): Float = if (this.isAlignedTo(align)) this else (((this / align).toInt() + 1) * align)
/** Returns the next value of [this] that is multiple of [align]. If [this] is already multiple, returns itself. */
public fun Double.nextAlignedTo(align: Double): Double = if (this.isAlignedTo(align)) this else (((this / align).toInt() + 1) * align)

/** Returns the previous value of [this] that is multiple of [align]. If [this] is already multiple, returns itself. */
public fun Int.prevAlignedTo(align: Int): Int = if (this.isAlignedTo(align)) this else nextAlignedTo(align) - align
/** Returns the previous value of [this] that is multiple of [align]. If [this] is already multiple, returns itself. */
public fun Long.prevAlignedTo(align: Long): Long = if (this.isAlignedTo(align)) this else nextAlignedTo(align) - align
/** Returns the previous value of [this] that is multiple of [align]. If [this] is already multiple, returns itself. */
public fun Float.prevAlignedTo(align: Float): Float = if (this.isAlignedTo(align)) this else nextAlignedTo(align) - align
/** Returns the previous value of [this] that is multiple of [align]. If [this] is already multiple, returns itself. */
public fun Double.prevAlignedTo(align: Double): Double = if (this.isAlignedTo(align)) this else nextAlignedTo(align) - align

/** Returns whether [this] is multiple of [alignment] */
public fun Int.isAlignedTo(alignment: Int): Boolean = alignment == 0 || (this % alignment) == 0
/** Returns whether [this] is multiple of [alignment] */
public fun Long.isAlignedTo(alignment: Long): Boolean = alignment == 0L || (this % alignment) == 0L
/** Returns whether [this] is multiple of [alignment] */
public fun Float.isAlignedTo(alignment: Float): Boolean = alignment == 0f || (this % alignment) == 0f
/** Returns whether [this] is multiple of [alignment] */
public fun Double.isAlignedTo(alignment: Double): Boolean = alignment == 0.0 || (this % alignment) == 0.0

/** Returns the previous or next value of [this] that is multiple of [align]. If [this] is already multiple, returns itself. */
public fun Float.nearestAlignedTo(align: Float): Float {
    val prev = this.prevAlignedTo(align)
    val next = this.nextAlignedTo(align)
    return if ((this - prev).absoluteValue < (this - next).absoluteValue) prev else next
}
/** Returns the previous or next value of [this] that is multiple of [align]. If [this] is already multiple, returns itself. */
public fun Double.nearestAlignedTo(align: Double): Double {
    val prev = this.prevAlignedTo(align)
    val next = this.nextAlignedTo(align)
    return if ((this - prev).absoluteValue < (this - next).absoluteValue) prev else next
}


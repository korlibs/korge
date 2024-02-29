package korlibs.math

/** Clamps [this] value into the range [min] and [max] */
fun Int.clamp(min: Int, max: Int): Int = if (this < min) min else if (this > max) max else this
/** Clamps [this] value into the range [min] and [max] */
fun Long.clamp(min: Long, max: Long): Long = if (this < min) min else if (this > max) max else this
/** Clamps [this] value into the range [min] and [max] */
fun Double.clamp(min: Double, max: Double): Double = if (this < min) min else if (this > max) max else this
/** Clamps [this] value into the range [min] and [max] */
fun Float.clamp(min: Float, max: Float): Float = if ((this < min)) min else if ((this > max)) max else this

/** Clamps [this] value into the range 0 and 1 */
fun Double.clamp01(): Double = clamp(0.0, 1.0)
/** Clamps [this] value into the range 0 and 1 */
fun Float.clamp01(): Float = clamp(0f, 1f)

/** Clamps [this] [Long] value into the range [min] and [max] converting it into [Int]. The default parameters will cover the whole range of values. */
fun Long.toIntClamp(min: Int = Int.MIN_VALUE, max: Int = Int.MAX_VALUE): Int {
    if (this < min) return min
    if (this > max) return max
    return this.toInt()
}

/** Clamps [this] [Long] value into the range [min] and [max] converting it into [Int] (where [min] must be zero or positive). The default parameters will cover the whole range of positive and zero values. */
fun Long.toUintClamp(min: Int = 0, max: Int = Int.MAX_VALUE): Int = this.toIntClamp(min, max)

/** Clamps the integer value in the 0..255 range */
fun Int.clampUByte(): Int {
    val n = this and -(if (this >= 0) 1 else 0)
    return (n or (0xFF - n shr 31)) and 0xFF
}
fun Int.clampUShort(): Int {
    val n = this and -(if (this >= 0) 1 else 0)
    return (n or (0xFFFF - n shr 31)) and 0xFFFF
}

fun Int.toShortClamped(): Short = this.clamp(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
fun Int.toByteClamped(): Byte = this.clamp(Byte.MIN_VALUE.toInt(), Byte.MAX_VALUE.toInt()).toByte()

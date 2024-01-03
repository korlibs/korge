@file:OptIn(ExperimentalStdlibApi::class)
@file:Suppress("NOTHING_TO_INLINE")

package korlibs.datastructure.internal.memory

/** Returns the bits in memory of [this] float */
internal inline fun Float.reinterpretAsInt(): Int = this.toBits()
/** Returns the bits in memory of [this] float */
internal inline fun Double.reinterpretAsLong(): Long = this.toBits()

/** Returns the float representation of [this] memory bits */
internal inline fun Int.reinterpretAsFloat(): Float = Float.fromBits(this)
/** Returns the float representation of [this] memory bits */
internal inline fun Long.reinterpretAsDouble(): Double = Double.fromBits(this)
/** Reverses the bytes of [this] [Short]: AABB -> BBAA */
internal fun Short.reverseBytes(): Short {
    val low = ((this.toInt() ushr 0) and 0xFF)
    val high = ((this.toInt() ushr 8) and 0xFF)
    return ((high and 0xFF) or (low shl 8)).toShort()
}

/** Reverses the bytes of [this] [Int]: AABBCCDD -> DDCCBBAA */
internal fun Int.reverseBytes(): Int {
    val v0 = ((this ushr 0) and 0xFF)
    val v1 = ((this ushr 8) and 0xFF)
    val v2 = ((this ushr 16) and 0xFF)
    val v3 = ((this ushr 24) and 0xFF)
    return (v0 shl 24) or (v1 shl 16) or (v2 shl 8) or (v3 shl 0)
}

/** Reverses the bytes of [this] [Long]: AABBCCDDEEFFGGHH -> HHGGFFEEDDCCBBAA */
internal fun Long.reverseBytes(): Long {
    val v0 = (this ushr 0).toInt().reverseBytes().toLong() and 0xFFFFFFFFL
    val v1 = (this ushr 32).toInt().reverseBytes().toLong() and 0xFFFFFFFFL
    return (v0 shl 32) or (v1 shl 0)
}
/** Creates an [Int] with [this] bits set to 1 */
internal fun Int.mask(): Int = (1 shl this) - 1
/** Creates a [Long] with [this] bits set to 1 */
internal fun Long.mask(): Long = (1L shl this.toInt()) - 1L

/** Extracts [count] bits at [offset] from [this] [Int] */
internal fun Int.extract(offset: Int, count: Int): Int = (this ushr offset) and count.mask()
/** Extracts a bits at [offset] from [this] [Int] (returning a [Boolean]) */
internal inline fun Int.extract(offset: Int): Boolean = extract1(offset) != 0
/** Extracts 1 bit at [offset] from [this] [Int] */
internal inline fun Int.extract1(offset: Int): Int = (this ushr offset) and 0b1
/** Replaces [this] bits from [offset] to [offset]+[count] with [value] and returns the result of doing such replacement */
internal fun Int.insert(value: Int, offset: Int, count: Int): Int {
    val mask = count.mask()
    val clearValue = this and (mask shl offset).inv()
    return clearValue or ((value and mask) shl offset)
}
/** Replaces 1 bit at [offset] with [value] and returns the result of doing such replacement */
internal fun Int.insert(value: Boolean, offset: Int): Int {
    val ivalue = if (value) 1 else 0
    return (this and (1 shl offset).inv()) or (ivalue shl offset)
}
/** Creates an integer with only bit [bit] set */
internal fun bit(bit: Int): Int = 1 shl bit
internal fun Int.without(bits: Int): Int = this and bits.inv()
internal fun Int.with(bits: Int): Int = this or bits

internal fun Long.without(bits: Long): Long = this and bits.inv()
internal fun Long.with(bits: Long): Long = this or bits

/** Get high 32-bits of this Long */
val Long.high: Int get() = (this ushr 32).toInt()
/** Get low 32-bits of this Long */
val Long.low: Int get() = this.toInt()


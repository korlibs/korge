@file:OptIn(ExperimentalStdlibApi::class)
@file:Suppress("NOTHING_TO_INLINE")

package korlibs.memory

import korlibs.math.clamp01
import korlibs.math.toInt
import kotlin.rotateLeft as rotateLeftKotlin
import kotlin.rotateRight as rotateRightKotlin

/** Returns the bits in memory of [this] float */
public inline fun Float.reinterpretAsInt(): Int = this.toBits()
/** Returns the bits in memory of [this] float */
public inline fun Double.reinterpretAsLong(): Long = this.toBits()

/** Returns the float representation of [this] memory bits */
public inline fun Int.reinterpretAsFloat(): Float = Float.fromBits(this)
/** Returns the float representation of [this] memory bits */
public inline fun Long.reinterpretAsDouble(): Double = Double.fromBits(this)

/** Rotates [this]  [bits] bits to the left */
public fun UInt.rotateLeft(bits: Int): UInt = this.rotateLeftKotlin(bits)
/** Rotates [this]  [bits] bits to the left */
public fun Int.rotateLeft(bits: Int): Int = this.rotateLeftKotlin(bits)
/** Rotates [this]  [bits] bits to the left */
public fun Long.rotateLeft(bits: Int): Long = this.rotateLeftKotlin(bits)

/** Rotates [this]  [bits] bits to the right */
public fun UInt.rotateRight(bits: Int): UInt = this.rotateRightKotlin(bits)
/** Rotates [this]  [bits] bits to the right */
public fun Int.rotateRight(bits: Int): Int = this.rotateRightKotlin(bits)
/** Rotates [this]  [bits] bits to the right */
public fun Long.rotateRight(bits: Int): Long = this.rotateRightKotlin(bits)

/** Reverses the bytes of [this] [Short]: AABB -> BBAA */
public fun Short.reverseBytes(): Short {
    val low = ((this.toInt() ushr 0) and 0xFF)
    val high = ((this.toInt() ushr 8) and 0xFF)
    return ((high and 0xFF) or (low shl 8)).toShort()
}

/** Reverses the bytes of [this] [Char]: AABB -> BBAA */
public fun Char.reverseBytes(): Char = this.code.toShort().reverseBytes().toInt().toChar()

/** Reverses the bytes of [this] [Int]: AABBCCDD -> DDCCBBAA */
public fun Int.reverseBytes(): Int {
    val v0 = ((this ushr 0) and 0xFF)
    val v1 = ((this ushr 8) and 0xFF)
    val v2 = ((this ushr 16) and 0xFF)
    val v3 = ((this ushr 24) and 0xFF)
    return (v0 shl 24) or (v1 shl 16) or (v2 shl 8) or (v3 shl 0)
}

/** Reverses the bytes of [this] [Long]: AABBCCDDEEFFGGHH -> HHGGFFEEDDCCBBAA */
public fun Long.reverseBytes(): Long {
    val v0 = (this ushr 0).toInt().reverseBytes().toLong() and 0xFFFFFFFFL
    val v1 = (this ushr 32).toInt().reverseBytes().toLong() and 0xFFFFFFFFL
    return (v0 shl 32) or (v1 shl 0)
}

/** Reverse the bits of [this] Int: abcdef...z -> z...fedcba */
public fun Int.reverseBits(): Int {
    var v = this
    v = ((v ushr 1) and 0x55555555) or ((v and 0x55555555) shl 1) // swap odd and even bits
    v = ((v ushr 2) and 0x33333333) or ((v and 0x33333333) shl 2) // swap consecutive pairs
    v = ((v ushr 4) and 0x0F0F0F0F) or ((v and 0x0F0F0F0F) shl 4) // swap nibbles ...
    v = ((v ushr 8) and 0x00FF00FF) or ((v and 0x00FF00FF) shl 8) // swap bytes
    v = ((v ushr 16) and 0x0000FFFF) or ((v and 0x0000FFFF) shl 16) // swap 2-byte long pairs
    return v
}

/** Returns the number of leading zeros of the bits of [this] integer */
public inline fun Int.countLeadingZeros(): Int = this.countLeadingZeroBits()

/** Returns the number of trailing zeros of the bits of [this] integer */
public fun Int.countTrailingZeros(): Int = this.countTrailingZeroBits()

/** Returns the number of leading ones of the bits of [this] integer */
public fun Int.countLeadingOnes(): Int = this.inv().countLeadingZeros()

/** Returns the number of trailing ones of the bits of [this] integer */
public fun Int.countTrailingOnes(): Int = this.inv().countTrailingZeros()

/** Takes n[bits] of [this] [Int], and extends the last bit, creating a plain [Int] in one's complement */
public fun Int.signExtend(bits: Int): Int = (this shl (32 - bits)) shr (32 - bits) // Int.SIZE_BITS
/** Takes n[bits] of [this] [Long], and extends the last bit, creating a plain [Long] in one's complement */
public fun Long.signExtend(bits: Int): Long = (this shl (64 - bits)) shr (64 - bits) // Long.SIZE_BITS

/** Creates an [Int] with [this] bits set to 1 */
public fun Int.mask(): Int = (1 shl this) - 1
/** Creates a [Long] with [this] bits set to 1 */
public fun Long.mask(): Long = (1L shl this.toInt()) - 1L

//fun Int.getBit(offset: Int): Boolean = ((this ushr offset) and 1) != 0
//fun Int.getBits(offset: Int, count: Int): Int = (this ushr offset) and count.mask()

/** Extracts [count] bits at [offset] from [this] [Int] */
public fun Int.extract(offset: Int, count: Int): Int = (this ushr offset) and count.mask()
/** Extracts a bits at [offset] from [this] [Int] (returning a [Boolean]) */
inline fun Int.extract(offset: Int): Boolean = extract1(offset) != 0
/** Extracts a bits at [offset] from [this] [Int] (returning a [Boolean]) */
inline fun Int.extractBool(offset: Int): Boolean = extract1(offset) != 0
/** Extracts 1 bit at [offset] from [this] [Int] */
inline fun Int.extract1(offset: Int): Int = (this ushr offset) and 0b1
/** Extracts 2 bits at [offset] from [this] [Int] */
inline fun Int.extract2(offset: Int): Int = (this ushr offset) and 0b11
/** Extracts 3 bits at [offset] from [this] [Int] */
inline fun Int.extract3(offset: Int): Int = (this ushr offset) and 0b111
/** Extracts 4 bits at [offset] from [this] [Int] */
inline fun Int.extract4(offset: Int): Int = (this ushr offset) and 0b1111
/** Extracts 5 bits at [offset] from [this] [Int] */
inline fun Int.extract5(offset: Int): Int = (this ushr offset) and 0b11111
/** Extracts 6 bits at [offset] from [this] [Int] */
inline fun Int.extract6(offset: Int): Int = (this ushr offset) and 0b111111
/** Extracts 7 bits at [offset] from [this] [Int] */
inline fun Int.extract7(offset: Int): Int = (this ushr offset) and 0b1111111
/** Extracts 8 bits at [offset] from [this] [Int] */
inline fun Int.extract8(offset: Int): Int = (this ushr offset) and 0b11111111
/** Extracts 9 bits at [offset] from [this] [Int] */
inline fun Int.extract9(offset: Int): Int = (this ushr offset) and 0b111111111
/** Extracts 10 bits at [offset] from [this] [Int] */
inline fun Int.extract10(offset: Int): Int = (this ushr offset) and 0b1111111111
/** Extracts 11 bits at [offset] from [this] [Int] */
inline fun Int.extract11(offset: Int): Int = (this ushr offset) and 0b11111111111
/** Extracts 12 bits at [offset] from [this] [Int] */
inline fun Int.extract12(offset: Int): Int = (this ushr offset) and 0b111111111111
/** Extracts 13 bits at [offset] from [this] [Int] */
inline fun Int.extract13(offset: Int): Int = (this ushr offset) and 0b1111111111111
/** Extracts 14 bits at [offset] from [this] [Int] */
inline fun Int.extract14(offset: Int): Int = (this ushr offset) and 0b11111111111111
/** Extracts 15 bits at [offset] from [this] [Int] */
inline fun Int.extract15(offset: Int): Int = (this ushr offset) and 0b111111111111111
/** Extracts 16 bits at [offset] from [this] [Int] */
inline fun Int.extract16(offset: Int): Int = (this ushr offset) and 0b1111111111111111
/** Extracts 24 bits at [offset] from [this] [Int] */
inline fun Int.extract24(offset: Int): Int = (this ushr offset) and 0xFFFFFF

/** Extracts [count] bits at [offset] from [this] [Int] sign-extending its result */
public fun Int.extractSigned(offset: Int, count: Int): Int = ((this ushr offset) and count.mask()).signExtend(count)
/** Extracts 8 bits at [offset] from [this] [Int] sign-extending its result */
public fun Int.extract8Signed(offset: Int): Int = (this ushr offset).toByte().toInt()
/** Extracts 16 bits at [offset] from [this] [Int] sign-extending its result */
public fun Int.extract16Signed(offset: Int): Int = (this ushr offset).toShort().toInt()

/** Extracts 8 bits at [offset] from [this] [Int] as [Byte] */
public fun Int.extractByte(offset: Int): Byte = (this ushr offset).toByte()
/** Extracts 16 bits at [offset] from [this] [Int] as [Short] */
public fun Int.extractShort(offset: Int): Short = (this ushr offset).toShort()

/** Extracts [count] at [offset] from [this] [Int] and convert the possible values into the range 0x00..[scale] */
public fun Int.extractScaled(offset: Int, count: Int, scale: Int): Int = (extract(offset, count) * scale) / count.mask()
/** Extracts [count] at [offset] from [this] [Int] and convert the possible values into the range 0.0..1.0 */
public fun Int.extractScaledf01(offset: Int, count: Int): Float = extract(offset, count).toFloat() / count.mask().toFloat()

/** Extracts [count] at [offset] from [this] [Int] and convert the possible values into the range 0x00..0xFF */
public fun Int.extractScaledFF(offset: Int, count: Int): Int = extractScaled(offset, count, 0xFF)
/** Extracts [count] at [offset] from [this] [Int] and convert the possible values into the range 0x00..0xFF (if there are 0 bits, returns [default]) */
public fun Int.extractScaledFFDefault(offset: Int, count: Int, default: Int): Int =
    if (count == 0) default else extractScaled(offset, count, 0xFF)

/** Replaces [this] bits from [offset] to [offset]+[count] with [value] and returns the result of doing such replacement */
public fun Int.insert(value: Int, offset: Int, count: Int): Int {
    val mask = count.mask()
    val clearValue = this and (mask shl offset).inv()
    return clearValue or ((value and mask) shl offset)
}

public fun Int.insert24(value: Int, offset: Int): Int = insertMask(value, offset, 0xFFFFFF)
public fun Int.insert16(value: Int, offset: Int): Int = insertMask(value, offset, 0xFFFF)
public fun Int.insert15(value: Int, offset: Int): Int = insertMask(value, offset, 0b111111111111111)
public fun Int.insert14(value: Int, offset: Int): Int = insertMask(value, offset, 0b11111111111111)
public fun Int.insert13(value: Int, offset: Int): Int = insertMask(value, offset, 0b1111111111111)
public fun Int.insert12(value: Int, offset: Int): Int = insertMask(value, offset, 0b111111111111)
public fun Int.insert11(value: Int, offset: Int): Int = insertMask(value, offset, 0b11111111111)
public fun Int.insert10(value: Int, offset: Int): Int = insertMask(value, offset, 0b1111111111)
public fun Int.insert9(value: Int, offset: Int): Int = insertMask(value, offset, 0b111111111)
public fun Int.insert8(value: Int, offset: Int): Int = insertMask(value, offset, 0b11111111)
public fun Int.insert7(value: Int, offset: Int): Int = insertMask(value, offset, 0b1111111)
public fun Int.insert6(value: Int, offset: Int): Int = insertMask(value, offset, 0b111111)
public fun Int.insert5(value: Int, offset: Int): Int = insertMask(value, offset, 0b11111)
public fun Int.insert4(value: Int, offset: Int): Int = insertMask(value, offset, 0b1111)
public fun Int.insert3(value: Int, offset: Int): Int = insertMask(value, offset, 0b111)
public fun Int.insert2(value: Int, offset: Int): Int = insertMask(value, offset, 0b11)
public fun Int.insert1(value: Int, offset: Int): Int = insertMask(value, offset, 0b1)

/** Fast Insert: do not clear bits, assume affecting bits are 0 */
public fun Int.finsert(value: Int, offset: Int): Int = this or (value shl offset)
public fun Int.finsert24(value: Int, offset: Int): Int = this or ((value and 0xFFFFFF) shl offset)
public fun Int.finsert16(value: Int, offset: Int): Int = this or ((value and 0xFFFF) shl offset)
public fun Int.finsert12(value: Int, offset: Int): Int = this or ((value and 0xFFF) shl offset)
public fun Int.finsert8(value: Int, offset: Int): Int = this or ((value and 0xFF) shl offset)
public fun Int.finsert7(value: Int, offset: Int): Int = this or ((value and 0b1111111) shl offset)
public fun Int.finsert6(value: Int, offset: Int): Int = this or ((value and 0b111111) shl offset)
public fun Int.finsert5(value: Int, offset: Int): Int = this or ((value and 0b11111) shl offset)
public fun Int.finsert4(value: Int, offset: Int): Int = this or ((value and 0b1111) shl offset)
public fun Int.finsert3(value: Int, offset: Int): Int = this or ((value and 0b111) shl offset)
public fun Int.finsert2(value: Int, offset: Int): Int = this or ((value and 0b11) shl offset)
public fun Int.finsert1(value: Int, offset: Int): Int = this or ((value and 0b1) shl offset)
public fun Int.finsert(value: Boolean, offset: Int): Int = finsert(value.toInt(), offset)

inline fun Int.insertMask(value: Int, offset: Int, mask: Int): Int {
    return (this and (mask shl offset).inv()) or ((value and mask) shl offset)
}
/** Replaces 1 bit at [offset] with [value] and returns the result of doing such replacement */
public fun Int.insert(value: Boolean, offset: Int): Int {
    val ivalue = if (value) 1 else 0
    return (this and (1 shl offset).inv()) or (ivalue shl offset)
}

public fun Int.insertScaled(value: Int, offset: Int, count: Int, scale: Int): Int = insert((value * count.mask()) / scale, offset, count)
public fun Int.insertScaledFF(value: Int, offset: Int, count: Int): Int = if (count == 0) this else this.insertScaled(value, offset, count, 0xFF)
/** Extracts [count] at [offset] from [this] [Int] and convert the possible values into the range 0.0..1.0 */
public fun Int.insertScaledf01(value: Float, offset: Int, count: Int): Int = this.insert((value.clamp01() * offset.mask()).toInt(), offset, count)


/** Check if [this] has all the bits set in [bits] set */
public infix fun Int.hasFlags(bits: Int): Boolean = (this and bits) == bits
public infix fun Int.hasBits(bits: Int): Boolean = (this and bits) == bits

/** Check if a specific bit at [index] is set */
public infix fun Int.hasBitSet(index: Int): Boolean = ((this ushr index) and 1) != 0

public infix fun Long.hasFlags(bits: Long): Boolean = (this and bits) == bits
public infix fun Long.hasBits(bits: Long): Boolean = (this and bits) == bits

/** Creates an integer with only bit [bit] set */
public fun bit(bit: Int): Int = 1 shl bit

/** Returns the integer [this] without the [bits] set */
public fun Int.unsetBits(bits: Int): Int = this and bits.inv()

/** Returns the integer [this] with the [bits] set */
public fun Int.setBits(bits: Int): Int = this or bits

/** Returns the integer [this] with the [bits] set or unset depending on the [set] parameter */
public fun Int.setBits(bits: Int, set: Boolean): Int = if (set) setBits(bits) else unsetBits(bits)

public fun Int.without(bits: Int): Int = this and bits.inv()
public fun Int.with(bits: Int): Int = this or bits

public fun Long.without(bits: Long): Long = this and bits.inv()
public fun Long.with(bits: Long): Long = this or bits

/** Get high 32-bits of this Long */
val Long.high: Int get() = (this ushr 32).toInt()
/** Get low 32-bits of this Long */
val Long.low: Int get() = this.toInt()

/** Get high 32-bits of this Long */
val Long._high: Int get() = (this ushr 32).toInt()
/** Get low 32-bits of this Long */
val Long._low: Int get() = this.toInt()

fun Long.Companion.fromLowHigh(low: Int, high: Int): Long = (low.toLong() and 0xFFFFFFFFL) or (high.toLong() shl 32)

inline fun Int.fastForEachOneBits(block: (Int) -> Unit) {
    var value = this
    var index = 0
    while (value != 0) {
        val shift = value.countTrailingZeroBits()
        index += shift
        if (index < 32) block(index)
        value = value ushr (shift + 1)
        index++
    }
}

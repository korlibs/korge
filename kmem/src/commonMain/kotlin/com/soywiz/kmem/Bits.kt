@file:UseExperimental(ExperimentalStdlibApi::class)
@file:Suppress("NOTHING_TO_INLINE")

package com.soywiz.kmem

import kotlin.rotateLeft as rotateLeftKotlin
import kotlin.rotateRight as rotateRightKotlin

/** Returns the bits in memory of [this] float */
inline fun Float.reinterpretAsInt() = this.toBits()
/** Returns the bits in memory of [this] float */
inline fun Double.reinterpretAsLong() = this.toBits()

/** Returns the float representation of [this] memory bits */
inline fun Int.reinterpretAsFloat() = Float.fromBits(this)
/** Returns the float representation of [this] memory bits */
inline fun Long.reinterpretAsDouble() = Double.fromBits(this)

/** Rotates [this]  [bits] bits to the left */
fun UInt.rotateLeft(bits: Int): UInt = this.rotateLeftKotlin(bits)
/** Rotates [this]  [bits] bits to the left */
fun Int.rotateLeft(bits: Int): Int = this.rotateLeftKotlin(bits)
/** Rotates [this]  [bits] bits to the left */
fun Long.rotateLeft(bits: Int): Long = this.rotateLeftKotlin(bits)

/** Rotates [this]  [bits] bits to the right */
fun UInt.rotateRight(bits: Int): UInt = this.rotateRightKotlin(bits)
/** Rotates [this]  [bits] bits to the right */
fun Int.rotateRight(bits: Int): Int = this.rotateRightKotlin(bits)
/** Rotates [this]  [bits] bits to the right */
fun Long.rotateRight(bits: Int): Long = this.rotateRightKotlin(bits)

/** Reverses the bytes of [this] [Short]: AABB -> BBAA */
fun Short.reverseBytes(): Short {
    val low = ((this.toInt() ushr 0) and 0xFF)
    val high = ((this.toInt() ushr 8) and 0xFF)
    return ((high and 0xFF) or (low shl 8)).toShort()
}

/** Reverses the bytes of [this] [Char]: AABB -> BBAA */
fun Char.reverseBytes(): Char = this.toShort().reverseBytes().toChar()

/** Reverses the bytes of [this] [Int]: AABBCCDD -> DDCCBBAA */
fun Int.reverseBytes(): Int {
    val v0 = ((this ushr 0) and 0xFF)
    val v1 = ((this ushr 8) and 0xFF)
    val v2 = ((this ushr 16) and 0xFF)
    val v3 = ((this ushr 24) and 0xFF)
    return (v0 shl 24) or (v1 shl 16) or (v2 shl 8) or (v3 shl 0)
}

/** Reverses the bytes of [this] [Long]: AABBCCDDEEFFGGHH -> HHGGFFEEDDCCBBAA */
fun Long.reverseBytes(): Long {
    val v0 = (this ushr 0).toInt().reverseBytes().toLong() and 0xFFFFFFFFL
    val v1 = (this ushr 32).toInt().reverseBytes().toLong() and 0xFFFFFFFFL
    return (v0 shl 32) or (v1 shl 0)
}

/** Reverse the bits of [this] Int: abcdef...z -> z...fedcba */
fun Int.reverseBits(): Int {
    var v = this
    v = ((v ushr 1) and 0x55555555) or ((v and 0x55555555) shl 1) // swap odd and even bits
    v = ((v ushr 2) and 0x33333333) or ((v and 0x33333333) shl 2) // swap consecutive pairs
    v = ((v ushr 4) and 0x0F0F0F0F) or ((v and 0x0F0F0F0F) shl 4) // swap nibbles ...
    v = ((v ushr 8) and 0x00FF00FF) or ((v and 0x00FF00FF) shl 8) // swap bytes
    v = ((v ushr 16) and 0x0000FFFF) or ((v and 0x0000FFFF) shl 16) // swap 2-byte long pairs
    return v
}

/** Returns the number of leading zeros of the bits of [this] integer */
inline fun Int.countLeadingZeros(): Int = this.countLeadingZeroBits()

/** Returns the number of trailing zeros of the bits of [this] integer */
fun Int.countTrailingZeros(): Int = this.countTrailingZeroBits()

/** Returns the number of leading ones of the bits of [this] integer */
fun Int.countLeadingOnes(): Int = this.inv().countLeadingZeros()

/** Returns the number of trailing ones of the bits of [this] integer */
fun Int.countTrailingOnes(): Int = this.inv().countTrailingZeros()

/** Takes n[bits] of [this] [Int], and extends the last bit, creating a plain [Int] in one's complement */
fun Int.signExtend(bits: Int): Int = (this shl (32 - bits)) shr (32 - bits) // Int.SIZE_BITS
/** Takes n[bits] of [this] [Long], and extends the last bit, creating a plain [Long] in one's complement */
fun Long.signExtend(bits: Int): Long = (this shl (64 - bits)) shr (64 - bits) // Long.SIZE_BITS

/** Creates an [Int] with [this] bits set to 1 */
fun Int.mask(): Int = (1 shl this) - 1
/** Creates a [Long] with [this] bits set to 1 */
fun Long.mask(): Long = (1L shl this.toInt()) - 1L

//fun Int.getBit(offset: Int): Boolean = ((this ushr offset) and 1) != 0
//fun Int.getBits(offset: Int, count: Int): Int = (this ushr offset) and count.mask()

/** Extracts [count] bits at [offset] from [this] [Int] */
fun Int.extract(offset: Int, count: Int): Int = (this ushr offset) and count.mask()
/** Extracts a bits at [offset] from [this] [Int] (returning a [Boolean]) */
fun Int.extract(offset: Int): Boolean = ((this ushr offset) and 1) != 0
/** Extracts a bits at [offset] from [this] [Int] (returning a [Boolean]) */
fun Int.extractBool(offset: Int) = this.extract(offset)
/** Extracts 4 bits at [offset] from [this] [Int] */
fun Int.extract4(offset: Int): Int = (this ushr offset) and 0xF
/** Extracts 8 bits at [offset] from [this] [Int] */
fun Int.extract8(offset: Int): Int = (this ushr offset) and 0xFF
/** Extracts 16 bits at [offset] from [this] [Int] */
fun Int.extract16(offset: Int): Int = (this ushr offset) and 0xFFFF

/** Extracts [count] bits at [offset] from [this] [Int] sign-extending its result */
fun Int.extractSigned(offset: Int, count: Int): Int = ((this ushr offset) and count.mask()).signExtend(count)
/** Extracts 8 bits at [offset] from [this] [Int] sign-extending its result */
fun Int.extract8Signed(offset: Int): Int = (this ushr offset).toByte().toInt()
/** Extracts 16 bits at [offset] from [this] [Int] sign-extending its result */
fun Int.extract16Signed(offset: Int): Int = (this ushr offset).toShort().toInt()

/** Extracts 8 bits at [offset] from [this] [Int] as [Byte] */
fun Int.extractByte(offset: Int): Byte = (this ushr offset).toByte()
/** Extracts 16 bits at [offset] from [this] [Int] as [Short] */
fun Int.extractShort(offset: Int): Short = (this ushr offset).toShort()

/** Extracts [count] at [offset] from [this] [Int] and convert the possible values into the range 0x00..[scale] */
fun Int.extractScaled(offset: Int, count: Int, scale: Int): Int = (extract(offset, count) * scale) / count.mask()
/** Extracts [count] at [offset] from [this] [Int] and convert the possible values into the range 0.0..1.0 */
fun Int.extractScaledf01(offset: Int, count: Int): Double = extract(offset, count).toDouble() / count.mask().toDouble()

/** Extracts [count] at [offset] from [this] [Int] and convert the possible values into the range 0x00..0xFF */
fun Int.extractScaledFF(offset: Int, count: Int): Int = extractScaled(offset, count, 0xFF)
/** Extracts [count] at [offset] from [this] [Int] and convert the possible values into the range 0x00..0xFF (if there are 0 bits, returns [default]) */
fun Int.extractScaledFFDefault(offset: Int, count: Int, default: Int): Int =
    if (count == 0) default else extractScaled(offset, count, 0xFF)

/** Replaces [this] bits from [offset] to [offset]+[count] with [value] and returns the result of doing such replacement */
fun Int.insert(value: Int, offset: Int, count: Int): Int {
    val mask = count.mask()
    val clearValue = this and (mask shl offset).inv()
    return clearValue or ((value and mask) shl offset)
}

/** Replaces [this] bits from [offset] to [offset]+8 with [value] and returns the result of doing such replacement */
fun Int.insert8(value: Int, offset: Int): Int = insert(value, offset, 8)
/** Replaces 1 bit at [offset] with [value] and returns the result of doing such replacement */
fun Int.insert(value: Boolean, offset: Int): Int = this.insert(if (value) 1 else 0, offset, 1)

fun Int.insertScaled(value: Int, offset: Int, count: Int, scale: Int): Int = insert((value * count.mask()) / scale, offset, count)
fun Int.insertScaledFF(value: Int, offset: Int, count: Int): Int = if (count == 0) this else this.insertScaled(value, offset, count, 0xFF)


/** Check if [this] has all the bits set in [bits] set */
infix fun Int.hasFlags(bits: Int) = (this and bits) == bits
infix fun Int.hasBits(bits: Int) = (this and bits) == bits

infix fun Long.hasFlags(bits: Long) = (this and bits) == bits
infix fun Long.hasBits(bits: Long) = (this and bits) == bits

/** Creates an integer with only bit [bit] set */
fun bit(bit: Int) = 1 shl bit

/** Returns the integer [this] without the [bits] set */
fun Int.unsetBits(bits: Int) = this and bits.inv()

/** Returns the integer [this] with the [bits] set */
fun Int.setBits(bits: Int) = this or bits

/** Returns the integer [this] with the [bits] set or unset depending on the [set] parameter */
fun Int.setBits(bits: Int, set: Boolean): Int = if (set) setBits(bits) else unsetBits(bits)

fun Int.without(bits: Int) = this and bits.inv()
fun Int.with(bits: Int) = this or bits

fun Long.without(bits: Long) = this and bits.inv()
fun Long.with(bits: Long) = this or bits

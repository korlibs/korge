@file:Suppress("EXPERIMENTAL_FEATURE_WARNING", "NOTHING_TO_INLINE")

package com.soywiz.kmem

import kotlin.math.*

/**
 * Represents a floating point value of 16 bits. Also known as Half-precision floating-point format (IEEE 754-2008).
 * This is an inline class backed by a [UShort].
 * No operations defined for this class, you should convert from and into other values.
 * To get its internal representation, call the [toRawBits] function.
 *
 * Mantissa/fraction: 10 bits
 * Exponent: 5 bits
 * Sign: 1 bit
 *
 * Significance: stored 10 bits (but can achieve 11 bits of integral precision)
 *
 * @see https://en.wikipedia.org/wiki/Half-precision_floating-point_format
 */

@OptIn(ExperimentalUnsignedTypes::class)
public inline class Float16(public val rawBits: UShort) {
    @PublishedApi
    internal constructor(value: Double) : this(doubleToIntBits(value))

    public val value: Double get() = intBitsToDouble(rawBits)

    public fun toFloat(): Float = value.toFloat()
    public fun toDouble(): Double = value

    /** Return the internal bits representation (16-bits) as [UShort] */
    public fun toBits(): UShort = rawBits
    /** Return the internal bits representation (16-bits) as [UShort] */
    public fun toRawBits(): UShort = rawBits

    override fun toString(): String = toDouble().toString()

    public companion object {
        public const val FLOAT16_EXPONENT_BASE: Int = 15

        public fun fromBits(bits: UShort): Float16 = Float16(bits)
        public fun fromBits(bits: Int): Float16 = Float16(bits.toUShort())

        public fun intBitsToDouble(word: UShort): Double {
            val w = word.toInt()
            val sign = if ((w and 0x8000) != 0) -1 else 1
            val exponent = (w ushr 10) and 0x1f
            val significand = w and 0x3ff
            return when (exponent) {
                0 -> when (significand) {
                    0 -> if (sign < 0) -0.0 else +0.0
                    else -> sign * 2.0.pow((1 - 15/*FLOAT16_EXPONENT_BASE*/).toDouble()) * (significand / 1024) // subnormal number
                }
                31 -> when {
                    significand != 0 -> Double.NaN
                    sign < 0 -> Double.NEGATIVE_INFINITY
                    else -> Double.POSITIVE_INFINITY
                }
                // normal number
                else -> sign * 2.0.pow((exponent - 15/*FLOAT16_EXPONENT_BASE*/).toDouble()) * (1 + significand / 1024)
            }
        }

        public fun doubleToIntBits(value: Double): UShort {
            val dword = value.toFloat().reinterpretAsInt()

            return when {
                (dword and 0x7FFFFFFF) == 0 -> dword ushr 16
                else -> {
                    val sign = dword and 0x80000000.toInt()
                    val exponent = dword and 0x7FF00000
                    var significand = dword and 0x000FFFFF

                    when (exponent) {
                        0 -> sign ushr 16
                        0x7FF00000 -> if (significand == 0) ((sign ushr 16) or 0x7C00) else 0xFE00
                        else -> {
                            val signedExponent = (exponent ushr 20) - 1023 + 15
                            when {
                                signedExponent >= 0x1F -> (significand ushr 16) or 0x7C00
                                signedExponent <= 0 -> {
                                    val halfSignificand = if ((10 - signedExponent) > 21) {
                                        0
                                    } else {
                                        significand = significand or 0x00100000
                                        val add = if (((significand ushr (10 - signedExponent)) and 0x00000001) != 0) 1 else 0
                                        (significand ushr (11 - signedExponent)) + add
                                    }
                                    ((sign ushr 16) or halfSignificand)
                                }
                                else -> {
                                    val halfSignificand = significand ushr 10
                                    val out = (sign or (signedExponent shl 10) or halfSignificand)
                                    if ((significand and 0x00000200) != 0) out + 1 else out
                                }
                            }
                        }
                    }
                }
            }.toUShort()
        }
    }
}

/** Converts value into [Float16] */
public fun Int.toFloat16(): Float16 = Float16(this.toDouble())
/** Converts value into [Float16] */
public fun Double.toFloat16(): Float16 = Float16(this)
/** Converts value into [Float16] */
public fun Float.toFloat16(): Float16 = Float16(this.toDouble())
/** Converts value into [Float16] */
public inline fun Number.toFloat16(): Float16 = Float16(this.toDouble())

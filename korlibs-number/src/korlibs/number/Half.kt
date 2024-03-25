@file:Suppress("EXPERIMENTAL_FEATURE_WARNING", "NOTHING_TO_INLINE")

package korlibs.number

import korlibs.number.internal.*

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
 * Formula: (2**(exp)) * (1 + mantissa/1024)
 *
 * Significance: stored 10 bits (but can achieve 11 bits of integral precision)
 *
 * @see <https://en.wikipedia.org/wiki/Half-precision_floating-point_format>
 */

@OptIn(ExperimentalUnsignedTypes::class)
public inline class Half(public val rawBits: UShort) {
    @PublishedApi internal constructor(value: Float) : this(floatToIntBits(value))
    @PublishedApi internal constructor(value: Double) : this(value.toFloat())

    //public val value: Double get() = intBitsToDouble(rawBits)
    public val value: Float get() = intBitsToFloat(rawBits)

    //public fun toFloat(): Float = value.toFloat()
    public fun toFloat(): Float = intBitsToFloat(rawBits)
    public fun toDouble(): Double = value.toDouble()

    /** Return the internal bits representation (16-bits) as [UShort] */
    public fun toBits(): UShort = rawBits
    /** Return the internal bits representation (16-bits) as [UShort] */
    public fun toRawBits(): UShort = rawBits

    override fun toString(): String = toDouble().toString()

    operator fun unaryMinus(): Half = Half(-this.value)
    operator fun unaryPlus(): Half = this
    operator fun plus(that: Half): Half = Half(this.value + that.value)
    operator fun minus(that: Half): Half = Half(this.value - that.value)
    operator fun times(that: Half): Half = Half(this.value * that.value)
    operator fun div(that: Half): Half = Half(this.value / that.value)
    operator fun rem(that: Half): Half = Half(this.value % that.value)

    public companion object {
        public const val FLOAT16_EXPONENT_BASE: Int = 15

        public fun fromBits(bits: Short): Half = Half(bits.toUShort())
        public fun fromBits(bits: UShort): Half = Half(bits)
        public fun fromBits(bits: Int): Half = Half(bits.toUShort())

        fun getHalfExp(bits: UShort): Int = ((bits.toInt() ushr 10) and 0b11111) - 0b01111
        fun getHalfMantissa(bits: UShort): Int = bits.toInt() and 0b1111111111
        fun getHalfSign(bits: UShort): Boolean = (bits.toInt() ushr 15) != 0

        fun getFloatExp(bits: Int): Int = ((bits ushr 23) and 0b11111111) - 0x7f
        fun getFloatMantissa(bits: Int): Int = bits and 0b11111111111111111111111
        fun getFloatSign(bits: Int): Boolean = (bits ushr 31) != 0

        fun packHalf(exp: Int, mantissa: Int, sign: Boolean): UShort {
            val sign = if (sign) 1 else 0
            val e = (exp + 0b01111) and 0b11111
            val m = mantissa and 0b1111111111
            return ((sign shl 15) or (e shl 10) or (m)).toUShort()
        }
        fun packFloat(exp: Int, mantissa: Int, sign: Boolean): Int {
            val sign = if (sign) 1 else 0
            val e = (exp + 0b01111111) and 0b11111111
            val m = mantissa and 0b11111111111111111111111
            return ((sign shl 31) or (e shl 23) or (m))
        }

        public fun intBitsToFloat(word: UShort): Float {
            //return intBitsToDouble(word).toFloat()
            //val exp = getHalfExp(word)
            //val mantissa = getHalfMantissa(word) shl 13
            //val sign = getHalfSign(word)
            //if (exp == 0 && mantissa == 0) return if (sign) -0f else 0f
            //return packFloat(exp, mantissa, sign).reinterpretAsFloat()

            // https://gist.github.com/neshume/0edc6ae1c5ad332bb4c62026be68a2fb
            val word = word.toInt()
            val t2: Int = word and 0x8000   // Sign bit
            var t1: Int = word and 0x7fff   // Non-sign bits
            val t3: Int = word and 0x7c00   // Exponent
            if (t3 == 0) return if (t2 == 0) 0f else -0f
            if (t3 == 0x7c00) {
                if ((t1 and 0b1111111111) != 0) return Float.NaN
                return when (t2) {
                    0 -> Float.POSITIVE_INFINITY
                    else -> Float.NEGATIVE_INFINITY
                }
            }
            t1 = t1 shl 13                  // Align mantissa on MSB
            t1 += 0x38000000                // Adjust bias
            t1 = if (t3 == 0) 0 else t1     // Denormals-as-zero
            t1 = t1 or (t2 shl 16)          // Re-insert sign bit
            return t1.reinterpretAsFloat()
        }

        fun floatToIntBits(value: Float): UShort {
            val word = value.toRawBits()
            val exp = getFloatExp(word)
            val mantissa = getFloatMantissa(word) ushr 13
            val sign = getFloatSign(word)
            val eexp = exp.coerceIn(-15, 16)
            return packHalf(eexp, mantissa, sign)
        }
    }
}

/** Converts value into [Half] */
fun Int.toHalf(): Half = Half(this.toFloat())
/** Converts value into [Half] */
fun Double.toHalf(): Half = Half(this)
/** Converts value into [Half] */
fun Float.toHalf(): Half = Half(this)
/** Converts value into [Half] */
@Deprecated("", ReplaceWith("this")) fun Half.toHalf(): Half = this
/** Converts value into [Half] */
inline fun Number.toHalf(): Half = Half(this.toFloat())

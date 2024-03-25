package korlibs.number

import korlibs.number.internal.*
import kotlin.math.*

/**
 * Fixed point class, to handle decimal values with a fixed precision.
 *
 * Floating points have more precision in the range [0, 1] and losses decimal precision outside that range.
 * This fixed point won't lose precision in any integer range.
 *
 * [Fixed] in decimal it has precision of:
 * +-9999999.99
 *
 * 7 integer digits, 2 decimal digits.
 *
 * 1 bit for sign
 * 23~24 bits of integer
 * 6~7 bits of decimal
 */
inline class Fixed private constructor(val raw: Int) : Comparable<Fixed> {
    companion object {
        const val SCALE_DIGITS = 2
        const val SCALE = 100
        const val HANDLE_DENORMALS = false
        //const val HANDLE_DENORMALS = true

        val NEGATIVE_INFINITY: Fixed get() = Fixed(Int.MIN_VALUE)
        val NaN: Fixed get() = Fixed(Int.MIN_VALUE + 1)
        val POSITIVE_INFINITY: Fixed get() = Fixed(Int.MAX_VALUE)

        fun fromRaw(raw: Int): Fixed = Fixed(raw)
        operator fun invoke(value: Int, unit: Unit = Unit): Fixed = Fixed((value * SCALE))
        operator fun invoke(value: Double): Fixed {
            if (HANDLE_DENORMALS) {
                if (value.isNaN()) return NaN
                when (value) {
                    Double.NEGATIVE_INFINITY -> return NEGATIVE_INFINITY
                    Double.POSITIVE_INFINITY -> return POSITIVE_INFINITY
                }
            }
            return Fixed((value * SCALE).toIntRound())
        }
        operator fun invoke(value: String): Fixed {
            return Fixed(value.toDouble())
            //val int = value.substringBefore('.')
            //val dec = value.substringAfter('.', "0")
            //return Fixed(int.toInt() * SCALE + (dec.toInt() % SCALE))
        }
    }

    val valueInt: Int get() = raw / SCALE
    val valueDec: Int get() = raw.absoluteValue % SCALE

    operator fun unaryPlus(): Fixed = this
    operator fun unaryMinus(): Fixed = Fixed(-raw)
    operator fun plus(other: Fixed): Fixed = Fixed(raw + other.raw)
    operator fun minus(other: Fixed): Fixed = Fixed(raw - other.raw)
    operator fun times(other: Fixed): Fixed = Fixed(((raw * other.raw.toDouble()) / SCALE).toIntRound())
    operator fun div(other: Fixed): Fixed = Fixed(((raw.toDouble() * SCALE) / other.raw.toDouble()).toIntRound())
    // @TODO: Do this properly
    operator fun rem(other: Fixed): Fixed = Fixed(this.toDouble() % other.toDouble())
    override fun compareTo(other: Fixed): Int = this.raw.compareTo(other.raw)

    fun toDouble(): Double {
        if (HANDLE_DENORMALS) {
            when (this) {
                NEGATIVE_INFINITY -> return Double.NEGATIVE_INFINITY
                NaN -> return Double.NaN
                POSITIVE_INFINITY -> return Double.POSITIVE_INFINITY
            }
        }
        return raw.toDouble() / SCALE
    }
    fun toFloat(): Float = toDouble().toFloat()
    fun toLong(): Long = toInt().toLong()
    fun toInt(): Int = raw / SCALE

    override fun toString(): String {
        if (HANDLE_DENORMALS) {
            when (this) {
                NEGATIVE_INFINITY -> return "-Infinity"
                NaN -> return "NaN"
                POSITIVE_INFINITY -> return "Infinity"
            }
        }
        val str = "$raw"
        return when {
            str.length <= SCALE_DIGITS -> "0.$str"
            else -> str.substring(0, str.length - SCALE_DIGITS) + "." + str.substring(str.length - SCALE_DIGITS)
        }
    }
}

val String.fixed: Fixed get() = Fixed(this)
val Long.fixed: Fixed get() = Fixed(this.toInt())
val Int.fixed: Fixed get() = Fixed(this)
val Double.fixed: Fixed get() = Fixed(this)
val Float.fixed: Fixed get() = Fixed(this.toDouble())
inline val Number.fixed: Fixed get() = Fixed(this.toDouble())

fun String.toFixed(): Fixed = Fixed(this)
fun Long.toFixed(): Fixed = Fixed(this.toInt())
fun Int.toFixed(): Fixed = Fixed(this)
fun Double.toFixed(): Fixed = Fixed(this)
fun Float.toFixed(): Fixed = Fixed(this.toDouble())
inline fun Number.toFixed(): Fixed = Fixed(this.toDouble())

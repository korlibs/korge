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
 * +-99999999999999.9999
 *
 * 14 integer digits, 4 decimal digits.
 *
 * 1 bit for sign
 * 47~48 bits of integer
 * 14~15 bits of decimal
 */
inline class FixedLong private constructor(val raw: Long) : Comparable<FixedLong> {
    companion object {
        const val SCALE_DIGITS = 4
        const val SCALE = 10000L
        const val HANDLE_DENORMALS = false
        //const val HANDLE_DENORMALS = true

        val NEGATIVE_INFINITY: FixedLong get() = FixedLong(Long.MIN_VALUE)
        val NaN: FixedLong get() = FixedLong(Long.MIN_VALUE + 1)
        val POSITIVE_INFINITY: FixedLong get() = FixedLong(Long.MAX_VALUE)

        fun fromRaw(raw: Long): FixedLong = FixedLong(raw)
        operator fun invoke(value: Long, unit: Unit = Unit): FixedLong = FixedLong((value * SCALE))
        operator fun invoke(value: Double): FixedLong {
            if (HANDLE_DENORMALS) {
                if (value.isNaN()) return NaN
                when (value) {
                    Double.NEGATIVE_INFINITY -> return NEGATIVE_INFINITY
                    Double.POSITIVE_INFINITY -> return POSITIVE_INFINITY
                }
            }
            return FixedLong((value * SCALE).toLongRound())
        }
        operator fun invoke(value: String): FixedLong {
            val integral = value.substringBeforeLast('.').toLong()
            val decimal = (value.substringAfterLast('.', "0") + "0000").substring(0, SCALE_DIGITS).toLong()
            val intAbs = integral.absoluteValue
            val sign = integral.sign
            //println("sign=$sign, intAbs=$intAbs, decimal=$decimal")
            return FixedLong(sign.toLong() * ((intAbs * SCALE) + decimal))
            //val int = value.substringBefore('.')
            //val dec = value.substringAfter('.', "0")
            //return Fixed(int.toInt() * SCALE + (dec.toInt() % SCALE))
        }
    }

    val valueInt: Long get() = raw / SCALE
    val valueDec: Int get() = (raw.absoluteValue % SCALE).toInt()

    operator fun unaryPlus(): FixedLong = this
    operator fun unaryMinus(): FixedLong = FixedLong(-raw)
    operator fun plus(other: FixedLong): FixedLong = FixedLong(raw + other.raw)
    operator fun minus(other: FixedLong): FixedLong = FixedLong(raw - other.raw)
    operator fun times(other: FixedLong): FixedLong = FixedLong(((raw * other.raw.toDouble()) / SCALE).toLongRound())
    operator fun div(other: FixedLong): FixedLong = FixedLong(((raw.toDouble() * SCALE) / other.raw.toDouble()).toLongRound())
    // @TODO: Do this properly
    operator fun rem(other: FixedLong): FixedLong = FixedLong(this.toDouble() % other.toDouble())
    override fun compareTo(other: FixedLong): Int = this.raw.compareTo(other.raw)

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
    fun toLong(): Long = raw / SCALE
    fun toInt(): Int = toLong().toInt()

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

val String.fixedLong: FixedLong get() = FixedLong(this)
val Long.fixedLong: FixedLong get() = FixedLong(this)
val Int.fixedLong: FixedLong get() = FixedLong(this.toLong())
val Double.fixedLong: FixedLong get() = FixedLong(this)
val Float.fixedLong: FixedLong get() = FixedLong(this.toDouble())
inline val Number.fixedLong: FixedLong get() = FixedLong(this.toDouble())

fun String.toFixedLong(): FixedLong = FixedLong(this)
fun Long.toFixedLong(): FixedLong = FixedLong(this)
fun Int.toFixedLong(): FixedLong = FixedLong(this.toLong())
fun Double.toFixedLong(): FixedLong = FixedLong(this)
fun Float.toFixedLong(): FixedLong = FixedLong(this.toDouble())
inline fun Number.toFixedLong(): FixedLong = FixedLong(this.toDouble())

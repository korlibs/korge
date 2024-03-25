package korlibs.number

import korlibs.number.internal.*
import kotlin.math.*

/**
 * FixedShort point class using a 16-bit short as backup, to handle decimal values with a fixed precision.
 *
 * Floating points have more precision in the range [0, 1] and losses decimal precision outside that range.
 * This fixed point won't lose precision in any integer range.
 *
 * [FixedShort] in decimal it has precision of:
 * +-3275.9
 *
 * 3.3 integer digits, 1 decimal digit.
 *
 * 1 bit for sign
 * 11~12 bits of integer
 * 3~5 bits of decimal
 */
inline class FixedShort private constructor(val raw: Short) : Comparable<FixedShort> {
    companion object {
        const val SCALE_DIGITS = 1
        const val SCALE = 10
        const val HANDLE_DENORMALS = false
        //const val HANDLE_DENORMALS = true

        val NEGATIVE_INFINITY: FixedShort get() = FixedShort(Short.MIN_VALUE)
        val NaN: FixedShort get() = FixedShort((Short.MIN_VALUE + 1).toShort())
        val POSITIVE_INFINITY: FixedShort get() = FixedShort(Short.MAX_VALUE)

        fun fromRaw(raw: Short): FixedShort = FixedShort(raw)
        operator fun invoke(value: Int, unit: Unit = Unit): FixedShort = FixedShort((value * SCALE).toShort())
        operator fun invoke(value: Double): FixedShort {
            if (HANDLE_DENORMALS) {
                if (value.isNaN()) return NaN
                when (value) {
                    Double.NEGATIVE_INFINITY -> return NEGATIVE_INFINITY
                    Double.POSITIVE_INFINITY -> return POSITIVE_INFINITY
                }
            }
            return FixedShort((value * SCALE).toIntRound().toShort())
        }
        operator fun invoke(value: String): FixedShort {
            return FixedShort(value.toDouble())
            //val int = value.substringBefore('.')
            //val dec = value.substringAfter('.', "0")
            //return Fixed(int.toInt() * SCALE + (dec.toInt() % SCALE))
        }
    }

    val valueInt: Int get() = raw.toInt() / SCALE
    val valueDec: Int get() = raw.toInt().absoluteValue % SCALE

    operator fun unaryPlus(): FixedShort = this
    operator fun unaryMinus(): FixedShort = FixedShort((-raw.toInt()).toShort())
    operator fun plus(other: FixedShort): FixedShort = FixedShort((raw + other.raw).toShort())
    operator fun minus(other: FixedShort): FixedShort = FixedShort((raw - other.raw).toShort())
    operator fun times(other: FixedShort): FixedShort = FixedShort(((raw * other.raw.toDouble()) / SCALE).toIntRound().toShort())
    operator fun div(other: FixedShort): FixedShort = FixedShort(((raw.toDouble() * SCALE) / other.raw.toDouble()).toIntRound().toShort())
    // @TODO: Do this properly
    operator fun rem(other: FixedShort): FixedShort = FixedShort(this.toDouble() % other.toDouble())
    override fun compareTo(other: FixedShort): Int = this.raw.compareTo(other.raw)

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

fun String.toFixedShort(): FixedShort = FixedShort(this)
fun Long.toFixedShort(): FixedShort = FixedShort(this.toInt())
fun Int.toFixedShort(): FixedShort = FixedShort(this.toInt())
fun Short.toFixedShort(): FixedShort = FixedShort(this.toInt())
fun Double.toFixedShort(): FixedShort = FixedShort(this)
fun Float.toFixedShort(): FixedShort = FixedShort(this.toDouble())
inline fun Number.toFixedShort(): FixedShort = FixedShort(this.toDouble())

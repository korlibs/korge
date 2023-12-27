package korlibs.bignumber

import korlibs.bignumber.ranges.*
import kotlin.math.*

// Big Number
/** Converts this into a [BigNum] */
val Double.bn: BigNum get() = BigNum("$this")
/** Converts this into a [BigNum] */
val Long.bn: BigNum get() = BigNum(this.bi, 0)
/** Converts this into a [BigNum] */
val Int.bn: BigNum get() = BigNum(this.bi, 0)
/** Converts this into a [BigNum] */
val String.bn: BigNum get() = BigNum(this)

/**
 * Represents a [BigNum],
 * a numeric value with decimal places that is exact.
 *
 * There are no precision issues like happens with [Float] or [Double] floating point types.
 */
class BigNum(val int: BigInt, val scale: Int) : Comparable<BigNum> {
    init {
        //println("BigNum($int, $scale) == $this")
    }

    companion object {
        /** Represents 0.0 as a [BigNum] */
        val ZERO = BigNum(BigInt(0), 0)
        /** Represents 1.0 as a [BigNum] */
        val ONE = BigNum(BigInt(1), 0)
        /** Represents 2.0 as a [BigNum] */
        val TWO = BigNum(BigInt(2), 0)

        /** Creates a [BigNum] from a string [str] */
        operator fun invoke(str: String): BigNum {
            val str = str.lowercase()
            //val ss = if (str.contains('.')) str.trimEnd('0') else str
            val exponentPartStr = str.substringAfter('e', "").takeIf { it.isNotEmpty() }?.trimStart('+')
            val ss = str.substringBefore('e')
            val point = ss.indexOf('.')
            val strBase = ss.replace(".", "")
            val exponent = exponentPartStr?.toInt() ?: 0
            val int = BigInt(strBase)
            return if (point < 0) {
                BigNum(int, -exponent)
            } else {
                BigNum(int, ss.length - point - 1 - exponent)
            }
        }
    }

    /**
     * Converts the internal scale of this BigNum to [otherScale] while keeping the same value.
     *
     * For example:
     * ```kotlin
     * assertEquals("0.0010", "0.001".bn.convertToScale(4).toString())
     * ```
     */
    fun convertToScale(otherScale: Int): BigNum = when {
        this.scale == otherScale -> this
        otherScale > this.scale -> BigNum(int * (10.bi pow (otherScale - this.scale)), otherScale)
        else -> BigNum(int / (10.bi pow (this.scale - otherScale)), otherScale)
    }

    /** Performs this + [other] returning a [BigNum], if the scale is different for both numbers, it finds a common one */
    operator fun plus(other: BigNum): BigNum = binary(other, BigInt::plus)
    /** Performs this - [other] returning a [BigNum], if the scale is different for both numbers, it finds a common one */
    operator fun minus(other: BigNum): BigNum = binary(other, BigInt::minus)
    /** Performs this * [other] returning a [BigNum], the scale ends being the sum of both scales */
    operator fun times(other: BigNum): BigNum =
        BigNum(this.int * other.int, this.scale + other.scale)

    /** Performs this / [other] returning a [BigNum] */
    //operator fun div(other: BigNum): BigNum = div(other, other.int.significantBits / 2)
    operator fun div(other: BigNum): BigNum = div(other, 0)

    /** Performs this / [other] returning a [BigNum] using a specific [precision] */
    fun div(other: BigNum, precision: Int): BigNum {
        val scale = (10.bi pow (other.scale + precision))
        val li = this.int * scale
        val ri = other.int
        val res = li / ri
        return BigNum(res, this.scale) * BigNum(1.bi, precision)
    }

    /** Performs this ** [exponent] */
    infix fun pow(exponent: Int) = pow(exponent, 32)

    /** Performs this ** [exponent] with a specific [precision] */
    fun pow(exponent: Int, precision: Int): BigNum {
        //if (exponent < 0) return ONE.div(this.pow(-exponent, precision), precision)
        if (exponent < 0) return ONE.div(this.pow(-exponent, precision), 0)
        var result = ONE
        var base = this
        var exp = exponent
        while (exp != 0) {
            if ((exp and 1) != 0) result *= base
            exp = exp shr 1
            base *= base
        }
        return result
    }

    override operator fun compareTo(other: BigNum): Int {
        val commonScale = this.commonScale(other)
        return this.convertToScale(commonScale).int.compareTo(other.convertToScale(commonScale).int)
    }

    /** Creates a [ClosedBigNumRange] between this and [that] */
    operator fun rangeTo(that: BigNum): ClosedBigNumRange = ClosedBigNumRange(
        start = this,
        endInclusive = that
    )

    override fun hashCode(): Int = int.hashCode() + 3 * scale.hashCode()
    override fun equals(other: Any?): Boolean = (other is BigNum) && this.compareTo(other) == 0

    private fun commonScale(other: BigNum) = max(this.scale, other.scale)

    private inline fun binary(other: BigNum, callback: (l: BigInt, r: BigInt) -> BigInt): BigNum {
        val commonScale = this.commonScale(other)
        val l = this.convertToScale(commonScale)
        val r = other.convertToScale(commonScale)
        val li = l.int
        val ri = r.int
        return BigNum(callback(li, ri), commonScale)
    }

    override fun toString(): String {
        val isNegative = int.isNegative
        val out = "${int.abs()}"
        val pos = out.length - scale
        val negativePart = if (isNegative) "-" else ""
        return negativePart + when {
            pos <= 0 -> "0." + "0".repeat(-pos) + out
            pos >= out.length -> out + "0".repeat(pos - out.length)
            else -> (out.substring(0, pos) + "." + out.substring(pos)).trimEnd('.')
        }
    }

    /** Converts this [BigInt] effectively losing the decimal places */
    fun toBigInt(): BigInt = convertToScale(0).int
    /** Converts this [BigInt] doing flooring when there are decimals */
    fun toBigIntFloor(): BigInt = toBigInt()
    /** Converts this [BigInt] doing ceiling when there are decimals */
    fun toBigIntCeil(): BigInt {
        val it = this.toBigInt()
        val decimal = decimalPart
        return if (decimal.isZero) it else (it + 1.bi)
    }
    /** Converts this [BigInt] doing rounding when there are decimals */
    fun toBigIntRound(): BigInt {
        val firstDigit = decimalPart / 10.bi.pow(scale - 1)
        return if (firstDigit.toInt() >= 5) toBigIntCeil() else toBigIntFloor()
    }

    /** Returns the decimal part as a [BigInt] of this BigNum so for `1.9123.bn` it will return `9123.bi` */
    val decimalPart: BigInt
        get() = int % 10.bi.pow(scale)
}

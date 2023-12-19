package korlibs.bignumber

import korlibs.bignumber.ranges.*

// Big Integer
/** Converts this into a [BigInt] */
val Long.bi: BigInt get() = BigInt(this)
/** Converts this into a [BigInt] */
val Int.bi: BigInt get() = BigInt(this)
/** Converts this into a [BigInt] */
val String.bi: BigInt get() = BigInt(this)
/** Converts this into a [BigInt] using a specific [radix], that is the base to use. radix=10 for decimal, radix=16 for hexadecimal */
fun String.bi(radix: Int): BigInt = BigInt(this, radix)

/**
 * Represents an arbitrary-sized Big Integer.
 */
interface BigInt : Comparable<BigInt>, BigIntConstructor {
    companion object {
        val usesNativeImplementation get() = BigInt(0) !is CommonBigInt

        val ZERO = BigInt("0")
        val MINUS_ONE = BigInt("-1")
        val ONE = BigInt("1")
        val TWO = BigInt("2")
        val TEN = BigInt("10")
        val SMALL = BigInt(UINT16_MASK)
    }

    // Checks
    /** Returns -1, 0 or +1 depending on the sign of this [BigInt] */
    val signum: Int
    /** Determines if this [BigInt] is 0 */
    val isZero get() = signum == 0
    /** Determines if this [BigInt] is not 0 */
    val isNotZero get() = signum != 0
    /** Determines if this [BigInt] is negative */
    val isNegative get() = signum < 0
    /** Determines if this [BigInt] is positive */
    val isPositive get() = signum > 0
    /** Determines if this [BigInt] is either negative or zero (non-positive) */
    val isNegativeOrZero get() = signum <= 0
    /** Determines if this [BigInt] is either positive or zero (non-negative) */
    val isPositiveOrZero get() = signum >= 0

    // Unary
    /** Returns this [BigInt]. A convenience method to make explicit the sign and to have symmetry with [unaryMinus]. */
    operator fun unaryPlus(): BigInt = this
    /** Returns a new [BigInt] with its sign changed. In the case of 0, it returns itself. */
    operator fun unaryMinus(): BigInt
    /** Returns a new [BigInt] with its bits flipped. 0 converts into 1, and 1 into 0. Equivalent to `-(this + 1)` */
    fun inv(): BigInt

    // Binary
    /** Returns this [BigInt] raised to [exponent] : `this ** exponent` */
    infix fun pow(exponent: BigInt): BigInt
    /** Returns this [BigInt] raised to [exponent] : `this ** exponent` */
    infix fun pow(exponent: Int): BigInt

    /** Returns a new BigInt with bits combining [this], [other] doing a bitwise `&`/`and` operation. Forces sign to positive. */
    infix fun and(other: BigInt): BigInt
    /** Returns a new BigInt with bits combining [this], [other] doing a bitwise `|`/`or` operation. Forces sign to positive. */
    infix fun or(other: BigInt): BigInt
    /** Returns a new BigInt with bits combining [this], [other] doing a bitwise `^`/`xor` operation. Forces sign to positive. */
    infix fun xor(other: BigInt): BigInt

    /** Returns a new BigInt with bits from [this] shifted to the left [count]. Equivalent to multiply by 2**[count]. Keeps the sign of [this] */
    infix fun shl(count: Int): BigInt
    /** Returns a new BigInt with bits from [this] shifted to the left [count]. Equivalent to divide by 2**[count]. Keeps the sign of [this] */
    infix fun shr(count: Int): BigInt

    /** Returns a new BigInt this [this] + [other] */
    operator fun plus(other: BigInt): BigInt
    /** Returns a new BigInt this [this] - [other] */
    operator fun minus(other: BigInt): BigInt
    /** Returns a new BigInt this [this] * [other] */
    operator fun times(other: BigInt): BigInt
    /** Returns a new BigInt this [this] / [other] */
    operator fun div(other: BigInt): BigInt
    /** Returns a new BigInt this [this] % [other] */
    operator fun rem(other: BigInt): BigInt

    // Conversion
    /** Converts this number to a [Int]. Throws a [BigIntOverflowException] in the case the number is too big to be stored in an [Int] */
    fun toInt(): Int
    /** Converts this number to a [String] using the specified [radix]. The radix is the base to use. 10 for decimal. 16 for hexadecimal. */
    fun toString(radix: Int): String

    // Extra
    /** Square of this number. Equivalent [this] * [this], but might be faster in some implementations. */
    fun square(): BigInt = this * this
    /** Returns this [BigInt] as positive or zero. Equivalent to `if (isNegative) -this else this` */
    fun abs(): BigInt = if (isNegative) -this else this
    /** Returns this + [other] */
    operator fun plus(other: Int): BigInt = this + create(other)
    /** Returns this - [other] */
    operator fun minus(other: Int): BigInt = this - create(other)
    /** Returns this * [other] */
    operator fun times(other: Int): BigInt = this * create(other)
    /** Returns this * [other] */
    operator fun times(other: Long): BigInt = this * create(other)
    /** Returns this / [other] */
    operator fun div(other: Int): BigInt = this / create(other)
    /** Returns this % [other] */
    operator fun rem(other: Int): BigInt = this % create(other)

    /** Creates a new inclusive [BigIntRange] ranging from [this] to [that] */
    operator fun rangeTo(that: BigInt): BigIntRange = BigIntRange(start = this, endInclusive = that)
}

interface BigIntCompanion : BigIntConstructor {
    operator fun invoke(value: Int) = create(value)
    operator fun invoke(value: Long) = create(value)
    operator fun invoke(value: String) = create(value)
    operator fun invoke(value: String, radix: Int) = create(value, radix)
}

interface BigIntConstructor {
    fun create(value: Int): BigInt
    //fun create(value: Long): BigInt = create("$value", 10) // @TODO: Kotlin.JS BUG
    fun create(value: Long): BigInt {
        if (value.toInt().toLong() == value) return create(value.toInt())
        return create(value.toString(10), 10)
    }
    // General
    fun create(value: String, radix: Int): BigInt {
        if (value.isEmpty()) throw BigIntInvalidFormatException("Zero length BigInteger")
        if (value.startsWith('-')) return -create(value.substring(1), radix)
        if (value == "0") return create(0)
        //val wordsRequired = ceil((value.length * log2(radix.toDouble())) / 16.0).toInt()
        //val out = UInt16ArrayZeroPad(IntArray(wordsRequired))
        var out = create(0)

        //for (c in value) {
        //    out *= radix
        //    out += digit(c, radix)
        //}

        var sum = 0
        var mul = 1

        for (n in 0 until value.length) {
            val last = n == value.length - 1
            val c = value[n]
            val d = digit(c, radix)

            //UnsignedBigInt.inplaceSmallMulAdd(out, radix, d)
            sum *= radix
            sum += d
            mul *= radix
            //if (last || mul * radix > 0x7FFF) {
            //if (last || mul * radix >= 0x1FFFFFFF) {
            if (last || mul * radix >= 0x1FFFFFF) {
                out *= mul
                out += sum
                sum = 0
                mul = 1
            }
        }
        return out
    }

    fun create(value: String): BigInt {
        if (value.startsWith("-")) return -create(value.substring(1))
        return parseWithNumberPrefix(value) { sub, radix -> create(sub, radix) }
    }
}

expect val BigIntNativeFactory: BigIntConstructor

fun BigInt(value: Int): BigInt = BigIntNativeFactory.create(value)
fun BigInt(value: Long): BigInt = BigIntNativeFactory.create(value)
fun BigInt(value: String, radix: Int): BigInt = BigIntNativeFactory.create(value, radix)
fun BigInt(value: String): BigInt = BigIntNativeFactory.create(value)

internal fun <T> parseWithNumberPrefix(str: String, gen: (sub: String, radix: Int) -> T): T = when {
    str.startsWith("0x") -> gen(str.substring(2), 16)
    str.startsWith("0o") -> gen(str.substring(2), 8)
    str.startsWith("0b") -> gen(str.substring(2), 2)
    else -> gen(str, 10)
}

/** A generic [BigInt] exception */
open class BigIntException(message: String) : Throwable(message)
/** A [BigInt] exception thrown when an invalid String value is provided while parsing */
open class BigIntInvalidFormatException(message: String) : BigIntException(message)
/** A [BigInt] exception thrown when trying to divide by zero */
open class BigIntDivisionByZeroException() : BigIntException("Division by zero")
/** A [BigInt] exception thrown when an overflow operation occurs, like for example when trying to convert a too big [BigInt] into an [Int] */
open class BigIntOverflowException(message: String) : BigIntException(message)
/** A [BigInt] exception thrown when doing a `pow` operation with a negative exponent */
open class BigIntNegativeExponentException() : BigIntOverflowException("Negative exponent")

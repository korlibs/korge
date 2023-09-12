package korlibs.bignumber

import java.math.*

/** Converts a [BigInteger] into a [BigInt] ([JvmBigInt]) */
val BigInteger.bi: JvmBigInt get() = JvmBigInt(this)

/** Converts a [BigInt] into a [BigInteger] */
fun BigInt.toBigInteger(): BigInteger = when (this) {
    is JvmBigInt -> this.value
    else -> BigInteger(this.toString())
}

actual val BigIntNativeFactory: BigIntConstructor = JvmBigInt

class JvmBigInt(val value: BigInteger) : BigInt, BigIntConstructor by JvmBigInt {
    constructor(value: Int) : this(BigInteger.valueOf(value.toLong()))
    constructor(value: Long) : this(BigInteger.valueOf(value))
    constructor(value: String, radix: Int) : this(try {
        BigInteger(value, radix)
    } catch (e: NumberFormatException) {
        throw BigIntInvalidFormatException(e.message ?: "")
    })
    companion object : BigIntConstructor {
        override fun create(value: Int): BigInt = JvmBigInt(value)
        override fun create(value: String, radix: Int): BigInt = JvmBigInt(value, radix)
    }

    val BigInt.jvm get() = (this as JvmBigInt).value
    val Int.jvm get() = BigInteger.valueOf(this.toLong())
    val Long.jvm get() = BigInteger.valueOf(this)

    override val signum: Int
        get() = value.signum()

    override fun unaryPlus(): BigInt = this
    override fun unaryMinus(): BigInt = (-value).bi
    override fun inv(): BigInt = value.inv().bi

    override fun abs(): BigInt = value.abs().bi
    override fun square(): BigInt = (value * value).bi
    override fun pow(exponent: BigInt): BigInt {
        return pow(exponent.toInt())
    }
    override fun pow(exponent: Int): BigInt {
        if (exponent < 0) throw BigIntNegativeExponentException()
        return (value.pow(exponent)).bi
    }

    override fun and(other: BigInt): BigInt = value.and(other.jvm).bi
    override fun or(other: BigInt): BigInt = value.or(other.jvm).bi
    override fun xor(other: BigInt): BigInt = value.xor(other.jvm).bi
    override fun shl(count: Int): BigInt = (value shl count).bi
    override fun shr(count: Int): BigInt = (value shr count).bi

    override fun plus(other: BigInt): BigInt = (value + other.jvm).bi
    override fun minus(other: BigInt): BigInt = (value - other.jvm).bi
    override fun times(other: BigInt): BigInt = (value * other.jvm).bi
    override fun div(other: BigInt): BigInt {
        if (other.isZero) throw BigIntDivisionByZeroException()
        return (value / other.jvm).bi
    }
    override fun rem(other: BigInt): BigInt = (value % other.jvm).bi

    override fun toInt(): Int = value.toInt()
    override fun toString(radix: Int): String = value.toString(radix)
    override fun toString(): String = toString(10)

    override fun hashCode(): Int = value.hashCode()
    override fun equals(other: Any?): Boolean = if (other is JvmBigInt) value == other.value else false
    override fun compareTo(other: BigInt): Int = value.compareTo(other.jvm)
}

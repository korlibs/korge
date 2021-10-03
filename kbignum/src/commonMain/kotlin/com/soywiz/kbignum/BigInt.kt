package com.soywiz.kbignum

interface BigInt : Comparable<BigInt>, BigIntConstructor {
    companion object {
        val usesNativeImplementation get() = BigInt(0) !is CommonBigInt
    }

    // Checks
    val signum: Int
    val isZero get() = signum == 0
    val isNotZero get() = signum != 0
    val isNegative get() = signum < 0
    val isPositive get() = signum > 0
    val isNegativeOrZero get() = signum <= 0
    val isPositiveOrZero get() = signum >= 0

    // Unary
    operator fun unaryMinus(): BigInt
    fun inv(): BigInt

    // Binary
    infix fun pow(exponent: BigInt): BigInt
    infix fun pow(exponent: Int): BigInt

    infix fun and(other: BigInt): BigInt
    infix fun or(other: BigInt): BigInt
    infix fun xor(other: BigInt): BigInt

    infix fun shl(count: Int): BigInt
    infix fun shr(count: Int): BigInt

    operator fun plus(other: BigInt): BigInt
    operator fun minus(other: BigInt): BigInt
    operator fun times(other: BigInt): BigInt
    operator fun div(other: BigInt): BigInt
    operator fun rem(other: BigInt): BigInt

    // Conversion
    fun toInt(): Int
    fun toString(radix: Int): String

    // Extra
    fun square(): BigInt = this * this
    operator fun unaryPlus(): BigInt = this
    fun abs(): BigInt = if (isNegative) -this else this
    operator fun plus(other: Int): BigInt = this + create(other)
    operator fun minus(other: Int): BigInt = this - create(other)
    operator fun times(other: Int): BigInt = this * create(other)
    operator fun times(other: Long): BigInt = this * create(other)
    operator fun div(other: Int): BigInt = this / create(other)
    operator fun rem(other: Int): BigInt = this % create(other)
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
            if (last || mul * radix > 0x1FFFFFFF) {
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

internal fun validateRadix(value: String, radix: Int) {
    for (c in value) digit(c, radix)
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


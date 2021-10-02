package com.soywiz.kbignum

import com.soywiz.kbignum.internal.*
import kotlin.math.*
import kotlin.time.*

/**
 * @TODO: Use JVM BigInteger and JS BigInt
 */
class CommonBigInt private constructor(val data: UInt16ArrayZeroPad, override val signum: Int, var dummy: Boolean) : BigInt, BigIntConstructor by CommonBigInt {
	val isSmall get() = data.size <= 1
	val maxBits get() = data.size * CHUNK_BITS
	val significantBits get() = maxBits - leadingZeros()

	companion object : BigIntCompanion {
        internal const val CHUNK_BITS = Short.SIZE_BITS // UInt16ArrayZeroPad

		val ZERO = CommonBigInt(uint16ArrayZeroPadOf(), 0, true)
		val MINUS_ONE = CommonBigInt(uint16ArrayZeroPadOf(1), -1, true)
		val ONE = CommonBigInt(uint16ArrayZeroPadOf(1), 1, true)
		val TWO = CommonBigInt(uint16ArrayZeroPadOf(2), 1, true)
		val TEN = CommonBigInt(uint16ArrayZeroPadOf(10), 1, true)
		val SMALL = CommonBigInt(uint16ArrayZeroPadOf(UINT16_MASK), 1, true)

		operator fun invoke(data: UInt16ArrayZeroPad, signum: Int): CommonBigInt {
			// Trim leading zeros
			var maxN = 0
			for (n in data.size - 1 downTo 0) {
				if (data[n] != 0) {
					maxN = n + 1
					break
				}
			}

			if (maxN == 0) return ZERO
			return CommonBigInt(data.copyOf(maxN), signum, false)
		}

		override fun create(value: Int): CommonBigInt = when (value) {
            -1 -> MINUS_ONE
            0 -> ZERO
            1 -> ONE
            2 -> TWO
            else -> {
                val magnitude = value.toLong().absoluteValue
                CommonBigInt(
                    uint16ArrayZeroPadOf(
                        (magnitude ushr 0).toInt(),
                        (magnitude ushr 16).toInt()
                    ), value.sign
                )
            }
        }
        //if (value == 0) return CommonBigInt(uint16ArrayZeroPadOf(), 0, true)

        override operator fun invoke(value: Int): CommonBigInt = create(value)
        override operator fun invoke(value: Long): CommonBigInt = create(value) as CommonBigInt
        override operator fun invoke(value: String): CommonBigInt = create(value) as CommonBigInt
        override operator fun invoke(value: String, radix: Int): CommonBigInt = create(value, radix) as CommonBigInt
    }

	fun countBits(): Int {
		var count = 0
		for (n in 0 until data.size) count += data[n].bitCount()
		return count
	}

    /** Number of leadingZeros with the size of [maxBits] */
	fun leadingZeros(): Int {
        if (isZero) return maxBits
        for (n in 0 until data.size) {
            val dataN = data[data.size - n - 1]
            if (dataN != 0) {
                //println("dataN: $dataN : trailingZeros=${dataN.trailingZeros()}")
                return (16 * n) + (dataN.leadingZeros() - 16)
            }
        }
        return maxBits
	}

    /** Number of trailingZeros with the size of [maxBits] */
	fun trailingZeros(): Int {
		if (isZero) return maxBits
        for (n in 0 until data.size) {
            val dataN = data[n]
            if (dataN != 0) {
                return 16 * n + dataN.trailingZeros()
            }
        }
		return maxBits
	}

	override operator fun plus(other: BigInt): CommonBigInt {
        other as CommonBigInt
		val l = this
		val r = other
		return when {
			l.isZero -> r
			r.isZero -> l
			l.isNegative && r.isPositive -> r - l.absoluteValue
			l.isPositive && r.isNegative -> l - r.absoluteValue
			l.isNegative && r.isNegative -> -(l.absoluteValue + r.absoluteValue)
			else -> CommonBigInt(UnsignedBigInt.add(this.data, other.data), signum)
		}
	}

	override operator fun minus(other: BigInt): CommonBigInt {
        other as CommonBigInt
		val l = this
		val r = other
		return when {
			r.isZero -> l
			l.isZero -> -r
			l.isNegative && r.isNegative -> r.abs() - l.abs() // (-l) - (-r) == (-l) + (r) == (r - l)
			l.isNegative && r.isPositive -> -(l.absoluteValue + r) // -l - r == -(l + r)
			l.isPositive && r.isNegative -> l + r.absoluteValue // l - (-r) == l + r
			l.isPositive && r.isPositive && l < r -> -(r - l)
			else -> CommonBigInt(UnsignedBigInt.sub(l.data, r.data), 1)
		}
	}

	@OptIn(ExperimentalTime::class)
    override infix fun pow(exponent: BigInt): CommonBigInt {
        exponent as CommonBigInt
		if (exponent.isNegative) throw BigIntOverflowException("Negative exponent")
        if (exponent.isZero) return ONE
        if (exponent == ONE) return this
        var base = this
        var expBit = 0
        val expMaxBits = exponent.significantBits
        //println("$exponent -> maxBits=${exponent.maxBits}, leadingZeros=${exponent.leadingZeros()}, trailingZeros=${exponent.trailingZeros()}, expMaxBits=$expMaxBits")
        if (expMaxBits < 32) return pow(exponent.toInt())
		var result = ONE
        while (expBit < expMaxBits) {
            if (exponent.getBit(expBit)) result *= base
            base *= base
            expBit++
        }
		return result
	}

    override infix fun pow(exponent: Int): CommonBigInt = powWithStats(exponent, null)

    override fun square(): CommonBigInt {
        return this * this
    }

	fun powWithStats(exponent: Int, stats: OpStats?): CommonBigInt {
        //return this pow exponent.bi
        if (exponent < 0) throw BigIntOverflowException("Negative exponent")
        if (exponent == 0) return ONE
        if (exponent == 1) return this
        var result = ONE
        var base = this
        var exp = exponent
        var iterCount = 0
        var multCount = 0
        while (exp != 0) {
            if ((exp and 1) != 0) {
                result *= base
                multCount++
            }
            base = base.square()
            multCount++
            iterCount++
            exp /= 2
        }
        stats?.set(iterations = iterCount, bigMultiplications = multCount)
        return result
    }


    data class OpStats(var iterations: Int = 0, var bigMultiplications: Int = 0) {
        fun set(iterations: Int = 0, bigMultiplications: Int = 0) {
            this.iterations = iterations
            this.bigMultiplications = bigMultiplications
        }
    }

    fun mulWithStats(other: CommonBigInt, stats: OpStats?): CommonBigInt {
        stats?.iterations = 0
        return when {
            this.isZero || other.isZero -> ZERO
            this == ONE -> other
            other == ONE -> this
            this == TWO -> other shl 1
            other == TWO -> this shl 1
            other.countBits() == 1 -> CommonBigInt(
                (this shl other.trailingZeros()).data,
                if (this.signum == other.signum) +1 else -1
            )
            else -> CommonBigInt(
                UnsignedBigInt.mul(this.data, other.data, stats),
                if (this.signum == other.signum) +1 else -1
            )
        }
    }

    override operator fun times(other: BigInt): CommonBigInt = mulWithStats(other as CommonBigInt, null)
    override operator fun div(other: BigInt): CommonBigInt = divRem(other as CommonBigInt).div
	override operator fun rem(other: BigInt): CommonBigInt = divRem(other as CommonBigInt).rem

    fun withBit(bit: Int, set: Boolean = true): CommonBigInt {
        // return if (set) this or (ONE shl bit) else this and (ONE shl bit).inv()
        val bitShift = (bit % 16)
        val bitMask = 1 shl bitShift
        val wordPos = bit / 16
        val out = CommonBigInt(data.copyOf(max(data.size, wordPos + 1)), if (signum == 0) 1 else signum, dummy)
        val outData = out.data
        outData[wordPos] = if (set) outData[wordPos] or bitMask else outData[wordPos] and bitMask.inv()
        return out
    }

	// Assumes positive non-zero values this > 0 && other > 0
	data class DivRem(val div: CommonBigInt, val rem: CommonBigInt)

	fun divRem(other: CommonBigInt): DivRem {
		return when {
			this.isZero -> DivRem(
				ZERO,
				ZERO
			)
			other.isZero -> throw BigIntDivisionByZeroException("Division by zero")
			this.isNegative && other.isNegative -> this.absoluteValue.divRem(other.absoluteValue).let {
				DivRem(it.div, -it.rem)
			}
			this.isNegative && other.isPositive -> this.absoluteValue.divRem(other.absoluteValue).let {
				DivRem(-it.div, -it.rem)
			}
			this.isPositive && other.isNegative -> this.absoluteValue.divRem(other.absoluteValue).let {
				DivRem(-it.div, it.rem)
			}
			other == ONE -> DivRem(this, ZERO)
			other == TWO -> DivRem(this shr 1, CommonBigInt(this.getBitInt(0)) as CommonBigInt)
			other <= SMALL -> UnsignedBigInt.divRemSmall(this.data, other.toInt()).let {
				DivRem(
					CommonBigInt(it.div, signum),
					CommonBigInt(it.rem) as CommonBigInt
				)
			}
			other.countBits() == 1 -> {
				val bits = other.trailingZeros()
				DivRem(this shr bits, this and ((ONE shl bits) - ONE))
			}
			else -> this.divRemBig(other)
		}
	}

	// Simple euclidean division
	private fun divRemBig(other: CommonBigInt): DivRem {
		if (this.isZero) return DivRem(ZERO, ZERO)
		if (other.isZero) throw BigIntDivisionByZeroException("division by zero")
		if (this.isNegative || other.isNegative) throw BigIntInvalidOperationException("Non positive numbers")
		val lbits = this.significantBits
		val rbits = other.significantBits
		var rem = this
		var divisor = other
		var divisorShift = 0
		var res = ZERO
		val initialShiftBits = lbits - rbits + 1
		divisorShift += initialShiftBits
		divisor = divisor shl initialShiftBits

		while (divisorShift >= 0) {
			if (divisor.isZero) throw BigIntDivisionByZeroException("divisor is zero!")

			if (divisor <= rem) {
                res = res.withBit(divisorShift)
				rem -= divisor
			}
			divisorShift--
			divisor = divisor shr 1
		}

		return DivRem(res, rem)
	}

    fun getBitInt(n: Int): Int = ((data[n / 16] ushr (n % 16)) and 1)
	fun getBit(n: Int): Boolean = getBitInt(n) != 0

	override infix fun shl(count: Int): CommonBigInt {
		if (count < 0) return this shr (-count)
		val blockShift = count / 16
		val smallShift = count % 16
		val out = UInt16ArrayZeroPad(data.size + blockShift + 1)
		var carry = 0
		val count_rcp = 16 - smallShift
		for (n in 0 until data.size + 1) {
			val v = data[n]
			out[n + blockShift] = ((carry) or (v shl smallShift))
			carry = v ushr count_rcp
		}
		if (carry != 0) throw BigIntException("ERROR!")
		return CommonBigInt(out, signum)
	}

    override infix fun shr(count: Int): CommonBigInt {
		//if (this.isNegative) return -(this.absoluteValue shr count) - 1
		if (count < 0) return this shl (-count)
		val blockShift = count / 16
		val smallShift = count % 16
		val out = UInt16ArrayZeroPad(data.size - blockShift)
		var carry = 0
		val count_rcp = 16 - smallShift
		val LOW_MASK = (1 shl smallShift) - 1
		for (n in data.size - 1 downTo blockShift) {
			val v = data[n]
			out[n - blockShift] = ((carry shl count_rcp) or (v ushr smallShift))
			carry = v and LOW_MASK
		}
		return CommonBigInt(out, signum)
	}

	override operator fun compareTo(that: BigInt): Int {
        that as CommonBigInt
		if (this.isNegative && that.isPositiveOrZero) return -1
		if (this.isPositiveOrZero && that.isNegative) return +1
		val resUnsigned = UnsignedBigInt.compare(this.data, that.data)
		return if (this.isNegative && that.isNegative) -resUnsigned else resUnsigned
	}

	override fun hashCode(): Int = this.data.hashCode() * this.signum

    override fun equals(other: Any?): Boolean = (other is CommonBigInt) && this.signum == other.signum && this.data.contentEquals(other.data)

	val absoluteValue get() = abs()
    override fun abs() = if (this.isZero) ZERO else if (this.isPositive) this else CommonBigInt(this.data, 1)
	override operator fun unaryPlus(): CommonBigInt = this
    override operator fun unaryMinus(): CommonBigInt = CommonBigInt(this.data, -signum, false)

    fun mulAddSmall(mul: Int, add: Int): CommonBigInt {
        if ((mul and 0xFFFF) == mul && (add and 0xFFFF) == add) {
            val temp = UInt16ArrayZeroPad(data.data.copyOf(data.size + 1))
            UnsignedBigInt.inplaceSmallMulAdd(temp, mul, add)
            return CommonBigInt(temp, if (temp.isAllZero) 0 else if (signum == 0) 1 else signum)
        }
        val out = this * CommonBigInt(mul)
        return if (add == 0) out else out + CommonBigInt(add)
    }

	override operator fun plus(other: Int): CommonBigInt = plus(CommonBigInt(other))
    override operator fun minus(other: Int): CommonBigInt = minus(CommonBigInt(other))
    override operator fun times(other: Int): CommonBigInt = mulAddSmall(other, 0)
    override operator fun times(other: Long): CommonBigInt = times(CommonBigInt(other))
    override operator fun div(other: Int): CommonBigInt = div(CommonBigInt(other))
    override operator fun rem(other: Int): CommonBigInt = rem(CommonBigInt(other))

    override infix fun and(other: BigInt): CommonBigInt = bitwise(other as CommonBigInt, Int::and)
    override infix fun or(other: BigInt): CommonBigInt = bitwise(other as CommonBigInt, Int::or)
	override infix fun xor(other: BigInt): CommonBigInt = bitwise(other as CommonBigInt, Int::xor)

    override fun inv(): CommonBigInt = CommonBigInt(
        UInt16ArrayZeroPad(this.data.size).also {
            for (n in 0 until it.size) it[n] = this.data[n].inv()
        }, 1
    )

	private inline fun bitwise(other: CommonBigInt, op: (a: Int, b: Int) -> Int): CommonBigInt {
		return CommonBigInt(
            UInt16ArrayZeroPad(max(this.data.size, other.data.size)).also {
				for (n in 0 until it.size) it[n] = op(this.data[n], other.data[n])
			}, 1
		)
	}

	override fun toString() = toString(10)

    override fun toString(radix: Int): String {
        // @TODO: Estimate digits
        val sb = StringBuilder()
        toString(sb, radix)
        return sb.toString()
    }

    fun toString(sb: StringBuilder, radix: Int) {
        if (this.isZero) {
            sb.append(0)
            return
        }
        if (this.isNegative) {
            sb.append('-')
        }
        when (radix) {
            // @TODO: Generalize to power of two radix
            2 -> toUnsignedString2(sb)
            16 -> toUnsignedString16(sb)
            else -> toUnsignedStringGeneric(sb, radix)
        }
    }

	private fun toUnsignedString2(sb: StringBuilder) {
        val mb = maxBits
        var started = false
        for (n in 0 until mb) {
            val bit = getBit(mb - n - 1)
            if (!started && !bit) continue
            sb.append(if (bit) '1' else '0')
            started = true
        }
    }

    private fun toUnsignedString16(sb: StringBuilder) {
        var started = false
        for (n in 0 until data.size) {
            val i = data.size - 1 - n
            val value = data[i]
            for (m in 0 until 4) {
                val digit = (value ushr 12 - (4 * m)) and 0xF
                if (digit == 0 && !started) continue
                sb.append(digit(digit))
                started = true
            }
        }
    }

	private fun toUnsignedStringGeneric(sb: StringBuilder, radix: Int) {
		if (radix !in 2..26) throw BigIntInvalidFormatException("Invalid radix $radix!")

        // Divide and conquer
        //if (this.data.size > 20) return

        val out = StringBuilder()
		var num = this

		// Optimize with mutable data
		while (num != ZERO) {
			val result = UnsignedBigInt.divRemSmall(num.data, radix)
			out.append(digit(result.rem))
			num = CommonBigInt(result.div, 1)
		}
		sb.append(out.reversed().toString())
	}

    override fun toInt(): Int {
		if (significantBits > 31) throw BigIntOverflowException("Can't represent CommonBigInt($this) as integer: maxBits=$maxBits, significantBits=$significantBits, trailingZeros=${trailingZeros()}")
		val magnitude = (this.data[0].toLong() or (this.data[1].toLong() shl 16)) * signum
		return magnitude.toInt()
	}

	fun toBigNum(): BigNum = BigNum(this, 0)
}

class UInt16ArrayZeroPad internal constructor(val data: IntArray) {
    val isAllZero: Boolean get() = data.all { it == 0 }
    val size get() = data.size

	constructor(size: Int) : this(IntArray(max(1, size)))

	operator fun get(index: Int): Int {
        if (index !in data.indices) return 0
        return data[index]
    }
	operator fun set(index: Int, value: Int) {
		if (index !in data.indices) {
            if (value != 0) error("Trying to set a value different to 0 to index $index in UInt16ArrayZeroPad")
            return
        }
		data[index] = value and UINT16_MASK
	}

	fun contentEquals(other: UInt16ArrayZeroPad) = this.data.contentEquals(other.data)
	fun copyOf(size: Int = this.size): UInt16ArrayZeroPad = UInt16ArrayZeroPad(data.copyOf(size))

    override fun toString(): String = "${data.toList()}"
}

internal fun uint16ArrayZeroPadOf(vararg values: Int) =
	UInt16ArrayZeroPad(values.size).apply { for (n in 0 until values.size) this[n] = values[n] }

private fun digit(v: Int): Char {
	if (v in 0..9) return '0' + v
	if (v in 10..26) return 'a' + (v - 10)
    throw BigIntInvalidFormatException("Invalid digit $v")
}

internal fun digit(c: Char): Int {
	return when (c) {
		in '0'..'9' -> c - '0'
		in 'a'..'z' -> c - 'a' + 10
		in 'A'..'Z' -> c - 'A' + 10
		else -> throw BigIntInvalidFormatException("Invalid digit '$c'")
	}
}

internal fun digit(c: Char, radix: Int): Int {
    val d = digit(c)
    if (d >= radix) throw BigIntInvalidFormatException("Character '$c' interpreted as $d not in the radix=$radix")
    return d
}

internal object UnsignedBigInt {
    inline fun carriedOp(out: UInt16ArrayZeroPad, signedCarry: Boolean, op: (index: Int) -> Int) {
        var carry = 0
        for (n in 0 until out.data.size) {
            val product = op(n) + carry
            val res = product and UINT16_MASK
            out.data[n] = res
            carry = if (signedCarry) product shr UINT16_SHIFT else product ushr UINT16_SHIFT
        }
        if (carry != 0) error("Overflow in carriedOp")
    }

    fun inplaceSmallMulAdd(v: UInt16ArrayZeroPad, mul: Int, add: Int) {
        if (mul != 1) carriedOp(v, signedCarry = false) { v[it] * mul }
        if (add != 0) carriedOp(v, signedCarry = false) { if (it == 0) v[it] + add else v[it] + 0 }
    }

	fun add(l: UInt16ArrayZeroPad, r: UInt16ArrayZeroPad): UInt16ArrayZeroPad {
		val out = UInt16ArrayZeroPad(max(l.size, r.size) + 1)
        carriedOp(out, signedCarry = false) { l[it] + r[it] }
		return out
	}

	// l >= 0 && r >= 0 && l >= r
	fun sub(l: UInt16ArrayZeroPad, r: UInt16ArrayZeroPad): UInt16ArrayZeroPad {
        val out = UInt16ArrayZeroPad(max(l.size, r.size) + 1)
        carriedOp(out, signedCarry = true) { l[it] - r[it] }
        return out
	}

	// l >= 0 && r >= 0
	// TODO optimize using the Karatsuba algorithm:
	// TODO: - https://en.wikipedia.org/wiki/Multiplication_algorithm#Karatsuba_multiplication
	fun mul(l: UInt16ArrayZeroPad, r: UInt16ArrayZeroPad, stats: CommonBigInt.OpStats?): UInt16ArrayZeroPad {
        var its = 0
		val out = UInt16ArrayZeroPad(l.size + r.size + 1)
		for (rn in 0 until r.size) {
			var carry = 0
			for (ln in 0 until l.size + 1) {
				val n = ln + rn
				val res = out[n] + (l[ln] * r[rn]) + carry
				out[n] = res and UINT16_MASK
				carry = res ushr 16
                its++
			}
			if (carry != 0) throw BigIntOverflowException("carry expected to be zero at this point")
		}
        stats?.iterations = its
		return out
	}

	class DivRemSmall(val div: UInt16ArrayZeroPad, val rem: Int)

	fun divRemSmall(value: UInt16ArrayZeroPad, r: Int): DivRemSmall {
		val length = value.size
		var rem = 0
		val qq = UInt16ArrayZeroPad(value.size)
        for (n in 0 until length) {
            val i = length - 1 - n
			val dd = (rem shl 16) + value[i]
			val q = dd / r
			rem = dd - q * r
			qq[i] = q
		}
		return DivRemSmall(qq, rem)
	}

	fun compare(l: UInt16ArrayZeroPad, r: UInt16ArrayZeroPad): Int {
		for (n in max(l.size, r.size) - 1 downTo 0) {
			val vl = l[n]
			val vr = r[n]
			if (vl < vr) return -1
			if (vl > vr) return +1
		}
		return 0
	}
}

private const val UINT16_MASK = 0xFFFF
private const val UINT16_SHIFT = 16

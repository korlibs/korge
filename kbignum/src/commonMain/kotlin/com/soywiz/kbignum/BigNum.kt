package com.soywiz.kbignum

import kotlin.math.*

class BigNum(val int: BigInt, val scale: Int) {
	init {
		//println("BigNum($int, $scale) == $this")
	}

	companion object {
		val ZERO = BigNum(BigInt.ZERO, 0)
		val ONE = BigNum(BigInt.ONE, 0)
		val TWO = BigNum(BigInt.TWO, 0)

		operator fun invoke(str: String): BigNum {
			//val ss = if (str.contains('.')) str.trimEnd('0') else str
			val ss = str
			val point = ss.indexOf('.')
			val int = BigInt(ss.replace(".", ""))
			return if (point < 0) {
				BigNum(int, 0)
			} else {
				BigNum(int, ss.length - point - 1)
			}
		}
	}

	fun convertToScale(otherScale: Int): BigNum = when {
        this.scale == otherScale -> this
        otherScale > this.scale -> BigNum(int * (10.bi pow (otherScale - this.scale)), otherScale)
        else -> BigNum(int / (10.bi pow (this.scale - otherScale)), otherScale)
    }

	operator fun plus(other: BigNum): BigNum = binary(other, BigInt::plus)
	operator fun minus(other: BigNum): BigNum = binary(other, BigInt::minus)
	operator fun times(other: BigNum): BigNum =
		BigNum(this.int * other.int, this.scale + other.scale)
    //operator fun div(other: BigNum): BigNum = div(other, other.int.significantBits / 2)
    operator fun div(other: BigNum): BigNum = div(other, 0)

    fun div(other: BigNum, precision: Int): BigNum {
        val li = this.int * (10.bi pow (other.scale + precision))
        val ri = other.int
        val res = li / ri
        return BigNum(res, this.scale) * BigNum(1.bi, precision)
    }

	infix fun pow(other: Int) = pow(other, 32)

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

	operator fun compareTo(other: BigNum): Int {
		val commonScale = this.commonScale(other)
		return this.convertToScale(commonScale).int.compareTo(other.convertToScale(commonScale).int)
	}

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
        return (if (isNegative) "-" else "") + if (pos <= 0) {
            "0." + "0".repeat(-pos) + out
        } else {
            (out.substring(0, pos) + "." + out.substring(pos)).trimEnd('.')
        }
	}
}

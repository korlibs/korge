package com.soywiz.kbignum

// Big Integer
inline val Long.bi get() = BigInt(this)
inline val Int.bi get() = BigInt(this)
inline val String.bi get() = BigInt(this)
inline fun String.bi(radix: Int) = BigInt(this, radix)

// Big Number
inline val Double.bn get() = BigNum("$this")
inline val Long.bn get() = BigNum(this.bi, 0)
inline val Int.bn get() = BigNum(this.bi, 0)
inline val String.bn get() = BigNum(this)

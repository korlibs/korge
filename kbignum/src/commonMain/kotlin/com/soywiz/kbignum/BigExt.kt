package com.soywiz.kbignum

// Big Integer
val Long.bi get() = BigInt(this)
val Int.bi get() = BigInt(this)
val String.bi get() = BigInt(this)
fun String.bi(radix: Int) = BigInt(this, radix)

// Big Number
val Double.bn get() = BigNum("$this")
val Long.bn get() = BigNum(this.bi, 0)
val Int.bn get() = BigNum(this.bi, 0)
val String.bn get() = BigNum(this)

package com.soywiz.korma.numeric

/*
inline class FixedPoint16(val value: Int) {
    constructor(integral: Int, fraction: Int) : this((integral shl 16) or (fraction and 0xFFFF))

    val integral: Int get() = value shr 16
    val fraction: Int get() = value and 0xFFFF

    operator fun plus(that: FixedPoint16) = FixedPoint16(this.integral + that.integral, this.fraction + that.fraction)
    operator fun minus(that: FixedPoint16) = FixedPoint16(this.integral - that.integral, this.fraction - that.fraction)

    fun toDouble() = integral.toDouble() + (fraction.toDouble() / 0x10000)
}
*/

package com.soywiz.kproject.internal

internal object Hex {
    private const val DIGITS = "0123456789ABCDEF"
    val DIGITS_UPPER = DIGITS.toUpperCase()
    val DIGITS_LOWER = DIGITS.toLowerCase()
    fun encodeCharLower(v: Int): Char = DIGITS_LOWER[v]
}

package com.soywiz.kproject.internal

internal object Hex {
    private const val DIGITS = "0123456789ABCDEF"
    val DIGITS_UPPER = DIGITS.uppercase()
    val DIGITS_LOWER = DIGITS.lowercase()
    fun encodeCharLower(v: Int): Char = DIGITS_LOWER[v]
}

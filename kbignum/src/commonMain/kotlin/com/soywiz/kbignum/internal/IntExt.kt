package com.soywiz.kbignum.internal

// https://graphics.stanford.edu/~seander/bithacks.html#CountBitsSetParallel
@OptIn(ExperimentalStdlibApi::class)
internal fun Int.bitCount(): Int = countOneBits()

@OptIn(ExperimentalStdlibApi::class)
internal fun Int.trailingZeros(): Int = countTrailingZeroBits()

@OptIn(ExperimentalStdlibApi::class)
internal fun Int.leadingZeros(): Int = countLeadingZeroBits()

internal fun min2(a: Int, b: Int) = if (a < b) a else b
internal fun max2(a: Int, b: Int) = if (a > b) a else b

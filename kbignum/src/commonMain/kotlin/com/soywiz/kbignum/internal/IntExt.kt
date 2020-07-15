package com.soywiz.kbignum.internal

// https://graphics.stanford.edu/~seander/bithacks.html#CountBitsSetParallel
@OptIn(ExperimentalStdlibApi::class)
fun Int.bitCount(): Int = countOneBits()

@OptIn(ExperimentalStdlibApi::class)
fun Int.trailingZeros(): Int = countTrailingZeroBits()

@OptIn(ExperimentalStdlibApi::class)
fun Int.leadingZeros(): Int = countLeadingZeroBits()

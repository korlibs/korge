package com.soywiz.kbignum.internal

import kotlin.math.*

// https://graphics.stanford.edu/~seander/bithacks.html#CountBitsSetParallel
@OptIn(ExperimentalStdlibApi::class)
internal fun Int.bitCount(): Int = countOneBits()

@OptIn(ExperimentalStdlibApi::class)
internal fun Int.trailingZeros(): Int = countTrailingZeroBits()

@OptIn(ExperimentalStdlibApi::class)
internal fun Int.leadingZeros(): Int = countLeadingZeroBits()

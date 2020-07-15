package com.soywiz.korau.internal

// @TODO: This is not considering overlapping

internal fun arraycopyStep(step: Int, src: ShortArray, srcPos: Int, dst: ShortArray, dstPos: Int, length: Int) {
    for (n in 0 until length step step) dst[dstPos + n] = src[srcPos + n]
}

internal fun arraycopyStep(step: Int, src: FloatArray, srcPos: Int, dst: FloatArray, dstPos: Int, length: Int) {
    for (n in 0 until length step step) dst[dstPos + n] = src[srcPos + n]
}

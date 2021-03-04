package com.soywiz.kmem

fun arrayinterleave(
    out: ByteArray, outPos: Int,
    array1: ByteArray, array1Pos: Int,
    array2: ByteArray, array2Pos: Int,
    size: Int,
    temp: FastByteTransfer = FastByteTransfer()
) {
    temp.use(out) { outp ->
        var m = outPos
        for (n in 0 until size) {
            outp[m++] = array1[array1Pos + n]
            outp[m++] = array2[array2Pos + n]
        }
    }
}

fun arrayinterleave(
    out: ShortArray, outPos: Int,
    array1: ShortArray, array1Pos: Int,
    array2: ShortArray, array2Pos: Int,
    size: Int,
    temp: FastShortTransfer = FastShortTransfer()
) {
    temp.use(out) { outp ->
        var m = outPos
        for (n in 0 until size) {
            outp[m++] = array1[array1Pos + n]
            outp[m++] = array2[array2Pos + n]
        }
    }
}

fun arrayinterleave(
    out: IntArray, outPos: Int,
    array1: IntArray, array1Pos: Int,
    array2: IntArray, array2Pos: Int,
    size: Int,
    temp: FastIntTransfer = FastIntTransfer()
) {
    temp.use(out) { outp ->
        var m = outPos
        for (n in 0 until size) {
            outp[m++] = array1[array1Pos + n]
            outp[m++] = array2[array2Pos + n]
        }
    }
}


fun arrayinterleave(
    out: FloatArray, outPos: Int,
    array1: FloatArray, array1Pos: Int,
    array2: FloatArray, array2Pos: Int,
    size: Int,
    temp: FastFloatTransfer = FastFloatTransfer()
) {
    temp.use(out) { outp ->
        var m = outPos
        for (n in 0 until size) {
            outp[m++] = array1[array1Pos + n]
            outp[m++] = array2[array2Pos + n]
        }
    }
}

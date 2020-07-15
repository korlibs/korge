package com.soywiz.korma.math

fun DoubleArray.minOrElse(nil: Double): Double {
    if (isEmpty()) return nil
    var out = Double.POSITIVE_INFINITY
    for (i in 0..lastIndex) out = kotlin.math.min(out, this[i])
    return out
}

fun DoubleArray.maxOrElse(nil: Double): Double {
    if (isEmpty()) return nil
    var out = Double.NEGATIVE_INFINITY
    for (i in 0..lastIndex) out = kotlin.math.max(out, this[i])
    return out
}

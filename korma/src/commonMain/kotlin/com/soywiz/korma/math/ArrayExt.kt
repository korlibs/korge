package com.soywiz.korma.math

import com.soywiz.korma.internal.*
import com.soywiz.korma.internal.max2

fun DoubleArray.minOrElse(nil: Double): Double {
    if (isEmpty()) return nil
    var out = Double.POSITIVE_INFINITY
    for (i in 0..lastIndex) out = min2(out, this[i])
    return out
}

fun DoubleArray.maxOrElse(nil: Double): Double {
    if (isEmpty()) return nil
    var out = Double.NEGATIVE_INFINITY
    for (i in 0..lastIndex) out = max2(out, this[i])
    return out
}

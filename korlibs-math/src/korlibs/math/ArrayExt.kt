package korlibs.math

import kotlin.math.max
import kotlin.math.min

fun DoubleArray.minOrElse(nil: Double): Double {
    if (isEmpty()) return nil
    var out = Double.POSITIVE_INFINITY
    for (i in 0..lastIndex) out = min(out, this[i])
    return out
}

fun DoubleArray.maxOrElse(nil: Double): Double {
    if (isEmpty()) return nil
    var out = Double.NEGATIVE_INFINITY
    for (i in 0..lastIndex) out = max(out, this[i])
    return out
}

package korlibs.math.internal

import kotlin.math.*

@PublishedApi internal infix fun Int.umod(other: Int): Int {
    val remainder = this % other
    return when {
        remainder < 0 -> remainder + other
        else -> remainder
    }
}

@PublishedApi internal infix fun Double.umod(other: Double): Double {
    val remainder = this % other
    return when {
        remainder < 0 -> remainder + other
        else -> remainder
    }
}

@PublishedApi internal infix fun Float.umod(other: Float): Float {
    val remainder = this % other
    return when {
        remainder < 0 -> remainder + other
        else -> remainder
    }
}

@PublishedApi
internal fun floorCeil(v: Double): Double = if (v < 0.0) ceil(v) else floor(v)

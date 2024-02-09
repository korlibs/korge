package korlibs.math

private val MINUS_ZERO_F = -0.0f

////////////////////
////////////////////

/** Performs the unsigned modulo between [this] and [other] (negative values would wrap) */
public infix fun Int.umod(other: Int): Int {
    val rm = this % other
    val remainder = if (rm == -0) 0 else rm
    return when {
        remainder < 0 -> remainder + other
        else -> remainder
    }
}

/** Performs the unsigned modulo between [this] and [other] (negative values would wrap) */
public infix fun Double.umod(other: Double): Double {
    val rm = this % other
    val remainder = if (rm == -0.0) 0.0 else rm
    return when {
        remainder < 0.0 -> remainder + other
        else -> remainder
    }
}

public infix fun Float.umod(other: Float): Float {
    val rm = this % other
    val remainder = if (rm == MINUS_ZERO_F) 0f else rm
    return when {
        remainder < 0f -> remainder + other
        else -> remainder
    }
}

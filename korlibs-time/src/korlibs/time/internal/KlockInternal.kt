package korlibs.time.internal

import kotlin.math.*

internal infix fun Int.umod(other: Int): Int {
    val rm = this % other
    val remainder = if (rm == -0) 0 else rm
    return when {
        remainder < 0 -> remainder + other
        else -> remainder
    }
}
internal infix fun Double.umod(other: Double): Double {
    val rm = this % other
    val remainder = if (rm == -0.0) 0.0 else rm
    return when {
        remainder < 0.0 -> remainder + other
        else -> remainder
    }
}
internal infix fun Int.div2(other: Int): Int = when {
    this < 0 || this % other == 0 -> this / other
    else -> (this / other) - 1
}

internal fun Int.cycle(min: Int, max: Int): Int = ((this - min) umod (max - min + 1)) + min
internal fun Int.cycleSteps(min: Int, max: Int): Int = (this - min) / (max - min + 1)
internal fun Double.toInt2(): Int = if (this < 0.0) floor(this).toInt() else this.toInt()
internal fun Double.toIntMod(mod: Int): Int = (this umod mod.toDouble()).toInt2()

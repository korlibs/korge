package korlibs.math

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

////////////////////
////////////////////

/** Converts [this] into [Int] rounding to the ceiling */
fun Float.toIntCeil(): Int = ceil(this).toInt()
/** Converts [this] into [Int] rounding to the ceiling */
fun Double.toIntCeil(): Int = ceil(this).toInt()

/** Converts [this] into [Int] rounding to the nearest */
fun Float.toIntRound(): Int = round(this).toInt()
/** Converts [this] into [Int] rounding to the nearest */
fun Double.toIntRound(): Int = round(this).toInt()

/** Converts [this] into [Int] rounding to the nearest */
fun Float.toLongRound(): Long = round(this).toLong()
/** Converts [this] into [Int] rounding to the nearest */
fun Double.toLongRound(): Long = round(this).toLong()

/** Convert this [Long] into an [Int] but throws an [IllegalArgumentException] in the case that operation would produce an overflow */
fun Long.toIntSafe(): Int = if (this in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) this.toInt() else throw IllegalArgumentException("Long doesn't fit Integer")

/** Converts [this] into [Int] rounding to the floor */
fun Float.toIntFloor(): Int = floor(this).toInt()
/** Converts [this] into [Int] rounding to the floor */
fun Double.toIntFloor(): Int = floor(this).toInt()

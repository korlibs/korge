package korlibs.time.internal

import korlibs.math.*
import korlibs.time.internal.clamp
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sign

internal const val MILLIS_PER_SECOND = 1000
internal const val MILLIS_PER_MINUTE = MILLIS_PER_SECOND * 60 // 60_000
internal const val MILLIS_PER_HOUR = MILLIS_PER_MINUTE * 60 // 3600_000
internal const val MILLIS_PER_DAY = MILLIS_PER_HOUR * 24 // 86400_000
internal const val MILLIS_PER_WEEK = MILLIS_PER_DAY * 7 // 604800_000

internal fun Int.padded(count: Int): String {
    // @TODO: Handle edge case Int.MIN_VALUE that could not be represented as abs
    val res = this.absoluteValue.toString().padStart(count, '0')
    return if (this < 0) return "-$res" else res
}
internal fun Double.padded(intCount: Int, decCount: Int): String {
    val intPart = floor(this).toInt()
    val decPart = round((this - intPart) * 10.0.pow(decCount)).toInt()
    return "${intPart.padded(intCount).substr(-intCount, intCount)}.${decPart.toString().padStart(decCount, '0').substr(-decCount)}"
}

internal fun String.substr(start: Int, length: Int = this.length): String {
    val low = (if (start >= 0) start else this.length + start).clamp(0, this.length)
    val high = (if (length >= 0) low + length else this.length + length).clamp(0, this.length)
    return if (high < low) "" else this.substring(low, high)
}

internal fun Int.clamp(min: Int, max: Int): Int = if (this < min) min else if (this > max) max else this
internal fun Int.cycle(min: Int, max: Int): Int = ((this - min) umod (max - min + 1)) + min
internal fun Int.cycleSteps(min: Int, max: Int): Int = (this - min) / (max - min + 1)

internal class Moduler(val value: Double) {
    private var avalue = abs(value)
    private val sign = sign(value)

    fun double(count: Double): Double {
        val ret = (avalue / count)
        avalue %= count
        return floor(ret) * sign
    }
    fun double(count: Int): Double = double(count.toDouble())
    fun double(count: Float): Double = double(count.toDouble())

    fun int(count: Double): Int = double(count).toInt()
    fun int(count: Int): Int = int(count.toDouble())
    fun int(count: Float): Int = int(count.toDouble())
}

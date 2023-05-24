package korlibs.math.interpolation

import korlibs.memory.*
import korlibs.math.roundDecimalPlaces

//inline class Ratio(val valueD: Double) : Comparable<Ratio> {
//    constructor(ratio: Float) : this(ratio.toDouble())
//    val value: Double get() = valueD
//    val valueF: Float get() = value.toFloat()
inline class Ratio(val valueF: Float) : Comparable<Ratio> {
    constructor(ratio: Double) : this(ratio.toFloat())
    val value: Float get() = valueF
    //val value: Double get() = valueD
    val valueD: Double get() = valueF.toDouble()

    fun toFloat(): Float = valueF
    fun toDouble(): Double = valueD

    constructor(value: Int, maximum: Int) : this(value.toFloat() / maximum.toFloat())
    constructor(value: Float, maximum: Float) : this(value / maximum)
    constructor(value: Double, maximum: Double) : this(value / maximum)

    val clamped: Ratio get() = Ratio(value.clamp01())

    fun roundDecimalPlaces(places: Int): Ratio = Ratio(value.roundDecimalPlaces(places))

    fun convertToRange(min: Float, max: Float): Float = valueF.convertRange(0f, 1f, min, max)
    fun convertToRange(min: Double, max: Double): Double = valueD.convertRange(0.0, 1.0, min, max)
    fun convertToRange(min: Ratio, max: Ratio): Ratio = Ratio(valueD.convertRange(0.0, 1.0, min.valueD, max.valueD))

    override fun compareTo(other: Ratio): Int = value.compareTo(other.value)

    fun isNaN(): Boolean = value.isNaN()

    companion object {
        val ZERO = Ratio(0f)
        val QUARTER = Ratio(.25f)
        val HALF = Ratio(.5f)
        val ONE = Ratio(1f)
        val NaN = Ratio(Float.NaN)

        inline fun forEachRatio(steps: Int, include0: Boolean = true, include1: Boolean = true, block: (ratio: Ratio) -> Unit) {
            val NS = steps - 1
            val NSf = NS.toFloat()
            val start = if (include0) 0 else 1
            val end = if (include1) NS else NS - 1
            for (n in start..end) {
                val ratio = n.toFloat() / NSf
                block(ratio.toRatio())
            }
        }
    }
}

@Deprecated("", ReplaceWith("this")) fun Ratio.toRatio(): Ratio = this

fun Float.toRatio(): Ratio = Ratio(this)
fun Double.toRatio(): Ratio = Ratio(this)

fun Float.toRatio(max: Float): Ratio = Ratio(this, max)
fun Double.toRatio(max: Double): Ratio = Ratio(this, max)

fun min(a: Ratio, b: Ratio): Ratio = Ratio(kotlin.math.min(a.value, b.value))
fun max(a: Ratio, b: Ratio): Ratio = Ratio(kotlin.math.max(a.value, b.value))
fun Ratio.clamp(min: Ratio, max: Ratio): Ratio = when {
    this < min -> min
    this > max -> max
    else -> this
}

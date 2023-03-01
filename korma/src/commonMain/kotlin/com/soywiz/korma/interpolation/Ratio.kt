package com.soywiz.korma.interpolation

import com.soywiz.kmem.*
import com.soywiz.korma.math.*

inline class Ratio(val value: Float) : Comparable<Ratio> {
    constructor(ratio: Double) : this(ratio.toFloat())
    constructor(value: Float, maximum: Float) : this(value / maximum)
    constructor(value: Double, maximum: Double) : this(value / maximum)

    val valueF: Float get() = value
    val valueD: Double get() = value.toDouble()

    val clamped: Ratio get() = Ratio(value.clamp01())

    fun roundDecimalPlaces(places: Int): Ratio = Ratio(value.roundDecimalPlaces(places))

    fun convertToRange(min: Float, max: Float): Float = value.convertRange(0f, 1f, min, max)
    fun convertToRange(min: Double, max: Double): Double = valueD.convertRange(0.0, 1.0, min, max)
    fun convertToRange(min: Ratio, max: Ratio): Ratio = Ratio(value.convertRange(0f, 1f, min.value, max.value))

    override fun compareTo(other: Ratio): Int = value.compareTo(other.value)

    companion object {
        val ZERO = Ratio(0f)
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

fun Float.toRatio(): Ratio = Ratio(this)
fun Double.toRatio(): Ratio = Ratio(this.toFloat())

fun min(a: Ratio, b: Ratio): Ratio = Ratio(kotlin.math.min(a.value, b.value))
fun max(a: Ratio, b: Ratio): Ratio = Ratio(kotlin.math.max(a.value, b.value))
fun Ratio.clamp(min: Ratio, max: Ratio): Ratio = when {
    this < min -> min
    this > max -> max
    else -> this
}
